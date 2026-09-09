package com.fktlomas.turnos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unidad pura con Mockito (no @SpringBootTest) — igual criterio que MercadoPagoServiceTest para
 * validarFirma: no hay nada de contexto de Spring que ganar acá, solo el algoritmo de firma.
 */
class TwilioSignatureVerifierTest {

    private static final String AUTH_TOKEN = "un-auth-token-de-prueba";
    private static final String BASE_URL = "https://api.fktlomas.com";

    private TwilioSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new TwilioSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "authToken", AUTH_TOKEN);
        ReflectionTestUtils.setField(verifier, "webhookBaseUrl", BASE_URL);
        ReflectionTestUtils.invokeMethod(verifier, "inicializar");
    }

    @Test
    void unaFirmaCalculadaCorrectamenteEsAceptada() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("From", "whatsapp:+5491123456789");
        params.put("Body", "1234");

        assertThatNoException().isThrownBy(() -> verifier.verificar(params, firmaValida(params)));
    }

    @Test
    void unParametroAlteradoDespuesDeFirmarSeRechaza() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("From", "whatsapp:+5491123456789");
        params.put("Body", "1234");
        String firma = firmaValida(params);

        params.put("Body", "9999"); // alterado después de calcular la firma

        assertThatThrownBy(() -> verifier.verificar(params, firma)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void seRechazaSinAuthTokenConfigurado() {
        TwilioSignatureVerifier sinToken = new TwilioSignatureVerifier();
        ReflectionTestUtils.setField(sinToken, "authToken", "");
        ReflectionTestUtils.setField(sinToken, "webhookBaseUrl", BASE_URL);
        ReflectionTestUtils.invokeMethod(sinToken, "inicializar");

        assertThatThrownBy(() -> sinToken.verificar(Map.of(), "cualquier-firma"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void seRechazaSinWebhookBaseUrlConfigurada() {
        TwilioSignatureVerifier sinUrl = new TwilioSignatureVerifier();
        ReflectionTestUtils.setField(sinUrl, "authToken", AUTH_TOKEN);
        ReflectionTestUtils.setField(sinUrl, "webhookBaseUrl", "");
        ReflectionTestUtils.invokeMethod(sinUrl, "inicializar");

        assertThatThrownBy(() -> sinUrl.verificar(Map.of(), "cualquier-firma"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void seRechazaSinHeaderDeFirma() {
        assertThatThrownBy(() -> verifier.verificar(Map.of(), null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> verifier.verificar(Map.of(), "")).isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Mismo algoritmo que usa com.twilio.security.RequestValidator (y que Twilio documenta):
     * URL completa + parámetros ordenados alfabéticamente por clave, cada uno pegado como
     * clave+valor sin separadores, firmado HMAC-SHA1 con el auth token y codificado en Base64.
     */
    private static String firmaValida(Map<String, String> params) {
        StringBuilder datos = new StringBuilder(BASE_URL + "/api/whatsapp/webhook");
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            datos.append(e.getKey()).append(e.getValue());
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(AUTH_TOKEN.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(datos.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
