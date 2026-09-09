package com.fktlomas.turnos.security;

import com.twilio.security.RequestValidator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Valida que un POST al webhook de WhatsApp venga realmente de Twilio (header
 * X-Twilio-Signature, HMAC-SHA1 sobre la URL + los parámetros del POST, firmado con el auth
 * token de la cuenta). Mismo criterio "fail-closed" que MercadoPagoService.validarFirma: sin
 * el auth token configurado, se rechaza toda notificación en vez de degradar con gracia como
 * hace WhatsAppService para el envío saliente — acá una firma no verificada significa que
 * cualquiera podría simular ser un paciente identificándose con el DNI de otro.
 *
 * La URL contra la que se valida se arma desde app.twilio.webhook-base-url (config fija), NO
 * desde HttpServletRequest.getRequestURL()/X-Forwarded-*: esos headers los puede mandar
 * cualquiera, y aceptarlos abriría una forma de calcular una firma "válida" contra una URL
 * distinta a la real.
 */
@Slf4j
@Component
public class TwilioSignatureVerifier {

    private static final String PATH_WEBHOOK = "/api/whatsapp/webhook";

    @Value("${app.twilio.auth-token}")
    private String authToken;

    @Value("${app.twilio.webhook-base-url}")
    private String webhookBaseUrl;

    private RequestValidator requestValidator;

    @PostConstruct
    void inicializar() {
        if (!authToken.isBlank()) {
            requestValidator = new RequestValidator(authToken);
        }
    }

    public void verificar(Map<String, String> parametrosPost, String xTwilioSignature) {
        if (requestValidator == null) {
            throw new AccessDeniedException(
                    "Falta configurar TWILIO_AUTH_TOKEN — se rechaza el mensaje entrante por seguridad");
        }
        if (webhookBaseUrl.isBlank()) {
            throw new AccessDeniedException(
                    "Falta configurar TWILIO_WEBHOOK_BASE_URL — se rechaza el mensaje entrante por seguridad");
        }
        if (xTwilioSignature == null || xTwilioSignature.isBlank()) {
            throw new AccessDeniedException("Falta el header X-Twilio-Signature");
        }
        String url = webhookBaseUrl + PATH_WEBHOOK;
        if (!requestValidator.validate(url, parametrosPost, xTwilioSignature)) {
            throw new AccessDeniedException("Firma de Twilio inválida");
        }
    }
}
