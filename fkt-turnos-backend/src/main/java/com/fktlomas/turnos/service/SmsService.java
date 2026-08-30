package com.fktlomas.turnos.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A diferencia de EmailService, estos métodos son sincrónicos (no @Async) y
 * tiran excepción en vez de tragarla: RecordatorioService necesita saber en
 * el momento si el envío funcionó para marcar el Recordatorio como ENVIADO o
 * FALLIDO en la misma corrida.
 */
@Slf4j
@Service
public class SmsService {

    @Value("${app.twilio.account-sid}")
    private String accountSid;

    @Value("${app.twilio.auth-token}")
    private String authToken;

    @Value("${app.twilio.numero-origen}")
    private String numeroOrigen;

    private boolean configurado;

    @PostConstruct
    void inicializar() {
        configurado = !accountSid.isBlank() && !authToken.isBlank() && !numeroOrigen.isBlank();
        if (configurado) {
            Twilio.init(accountSid, authToken);
        } else {
            log.info("SMS deshabilitado: faltan credenciales de Twilio (TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/TWILIO_NUMERO_ORIGEN)");
        }
    }

    public boolean estaConfigurado() {
        return configurado;
    }

    public void enviar(String telefonoDestino, String texto) {
        if (!configurado) {
            throw new IllegalStateException("El canal SMS no está configurado (faltan credenciales de Twilio)");
        }
        try {
            Message.creator(new PhoneNumber(telefonoDestino), new PhoneNumber(numeroOrigen), texto).create();
        } catch (Exception e) {
            throw new IllegalStateException("Error enviando SMS a " + telefonoDestino + ": " + e.getMessage(), e);
        }
    }
}
