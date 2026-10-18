package com.fktlomas.turnos.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * TOTP (RFC 6238) mínimo, sin dependencias externas: genera secretos en Base32
 * y verifica códigos de 6 dígitos compatibles con Google Authenticator / Authy / etc.
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final int PERIODO_SEGUNDOS = 30;
    private static final int DIGITOS = 6;
    private static final int VENTANA_TOLERANCIA = 1;
    // Una sola instancia reusada: SecureRandom es thread-safe y crear una nueva por llamada
    // tiene un costo de inicialización/sembrado innecesario.
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generarSecreto() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String otpAuthUri(String secretoBase32, String cuentaEmail) {
        String etiqueta = "FKT%20Lomas:" + cuentaEmail.replace(" ", "%20");
        return "otpauth://totp/" + etiqueta + "?secret=" + secretoBase32
                + "&issuer=FKT%20Lomas&digits=" + DIGITOS + "&period=" + PERIODO_SEGUNDOS;
    }

    public boolean verificar(String secretoBase32, String codigo) {
        return pasoCoincidente(secretoBase32, codigo) >= 0;
    }

    /**
     * Igual que {@link #verificar}, pero devuelve el "paso" de 30 segundos con el que coincidió
     * el código (o -1 si no coincide con ninguno). El login lo usa para rechazar un código ya
     * usado (RFC 6238 §5.2): sin eso, alguien que mira por encima del hombro o intercepta un
     * código podría reusarlo durante los ~90 segundos que sigue siendo válido. Ver
     * AuthService.verificarSegundoFactor y la columna usuarios.mfa_ultimo_paso (V30).
     */
    public long pasoCoincidente(String secretoBase32, String codigo) {
        if (secretoBase32 == null || codigo == null || !codigo.matches("\\d{6}")) {
            return -1;
        }
        byte[] clave = base32Decode(secretoBase32);
        long pasoActual = Instant.now().getEpochSecond() / PERIODO_SEGUNDOS;
        long coincide = -1;
        // Comparación de tiempo constante (MessageDigest.isEqual, mismo criterio que
        // MercadoPagoService.validarFirma) — con String.equals(), que corta apenas encuentra
        // el primer dígito distinto, alguien midiendo el tiempo de respuesta con precisión
        // podría en teoría ir deduciendo el código dígito a dígito. Se recorren las tres
        // ventanas siempre completas (sin "return" apenas matchea una) por la misma razón:
        // cortar antes también filtraría por tiempo cuál ventana fue la que coincidió.
        for (int i = -VENTANA_TOLERANCIA; i <= VENTANA_TOLERANCIA; i++) {
            String esperado = generarCodigo(clave, pasoActual + i);
            if (MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), codigo.getBytes(StandardCharsets.UTF_8))) {
                coincide = pasoActual + i;
            }
        }
        return coincide;
    }

    private String generarCodigo(byte[] clave, long paso) {
        try {
            byte[] data = new byte[8];
            long p = paso;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (p & 0xFF);
                p >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(clave, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int codigo = binario % (int) Math.pow(10, DIGITOS);
            return String.format("%0" + DIGITOS + "d", codigo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el código TOTP", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String encoded) {
        String limpio = encoded.trim().toUpperCase(java.util.Locale.ROOT).replace("=", "");
        byte[] result = new byte[limpio.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : limpio.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
