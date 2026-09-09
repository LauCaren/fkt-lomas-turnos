package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.security.TwilioSignatureVerifier;
import com.fktlomas.turnos.service.WhatsAppBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A este endpoint le pega Twilio, no un usuario con sesión — la seguridad es la verificación
 * de firma X-Twilio-Signature (ver TwilioSignatureVerifier), no el JWT (ver SecurityConfig).
 * Si la firma no valida, TwilioSignatureVerifier tira AccessDeniedException y GlobalExceptionHandler
 * responde 403 — a diferencia del webhook de Mercado Pago, acá una firma inválida SÍ es un
 * error real (no una notificación irrelevante), no corresponde devolver 200 igual.
 */
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final TwilioSignatureVerifier twilioSignatureVerifier;
    private final WhatsAppBotService whatsAppBotService;

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> webhook(@RequestParam Map<String, String> parametros,
                                           @RequestHeader(name = "X-Twilio-Signature", required = false) String firma) {
        twilioSignatureVerifier.verificar(parametros, firma);
        whatsAppBotService.procesarMensajeEntrante(parametros);
        // Twilio espera TwiML de vuelta, no JSON — las respuestas al paciente ya se mandaron
        // por WhatsAppService dentro de procesarMensajeEntrante, así que acá alcanza con un
        // TwiML vacío en vez de generar una respuesta también por este camino.
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                .body("<Response></Response>");
    }
}
