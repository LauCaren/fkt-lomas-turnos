package com.fktlomas.turnos.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encripta el secreto TOTP (mfa_secret) antes de guardarlo — transparente para el resto del
 * código, Usuario.getMfaSecret()/setMfaSecret() siguen trabajando con el valor en texto plano.
 * Si la base se llegara a filtrar (backup, acceso indebido), el secreto MFA no queda expuesto
 * en texto plano junto con todo lo demás — a diferencia de la contraseña (bcrypt), TOTP no es
 * un hash de un solo sentido, así que sin esto cualquiera con lectura a la tabla podría generar
 * códigos válidos y saltarse el segundo factor por completo.
 */
@Slf4j
@Component
@Converter
public class MfaSecretConverter implements AttributeConverter<String, String> {

    /** Tiene que coincidir con el default de app.mfa.encryption-key en application.yml. */
    private static final String CLAVE_POR_DEFECTO = "changeme-dev-mfa-key-change-in-prod-please";
    private static final int TAMANO_IV_BYTES = 12;
    private static final int TAMANO_TAG_BITS = 128;

    private final SecretKey clave;

    public MfaSecretConverter(@Value("${app.mfa.encryption-key:}") String claveConfigurada) {
        if (claveConfigurada == null || claveConfigurada.isBlank() || CLAVE_POR_DEFECTO.equals(claveConfigurada)) {
            log.warn("MFA_ENCRYPTION_KEY no está configurado — usando una clave aleatoria generada para esta "
                    + "ejecución. Los secretos MFA guardados con esta clave no se van a poder leer después de un "
                    + "reinicio del backend. Configurá la variable de entorno MFA_ENCRYPTION_KEY (256 bits en "
                    + "Base64) con un valor propio antes de desplegar a producción.");
            this.clave = generarClaveAleatoria();
        } else {
            this.clave = new SecretKeySpec(Base64.getDecoder().decode(claveConfigurada), "AES");
        }
    }

    @Override
    public String convertToDatabaseColumn(String secretoPlano) {
        if (secretoPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(secretoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo encriptar el secreto MFA", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String valorGuardado) {
        if (valorGuardado == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(valorGuardado);
            byte[] iv = new byte[TAMANO_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, TAMANO_IV_BYTES);
            byte[] cifrado = new byte[datos.length - TAMANO_IV_BYTES];
            System.arraycopy(datos, TAMANO_IV_BYTES, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo desencriptar el secreto MFA", e);
        }
    }

    private SecretKey generarClaveAleatoria() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            return kg.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
