package com.fktlomas.turnos.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Ver SmsService: mismo enfoque sincrónico, canal separado porque Twilio usa un número de origen distinto (prefijo "whatsapp:"). */
@Slf4j
@Service
public class WhatsAppService {

    @Value("${app.twilio.account-sid}")
    private String accountSid;

    @Value("${app.twilio.auth-token}")
    private String authToken;

    @Value("${app.twilio.numero-origen-whatsapp}")
    private String numeroOrigenWhatsApp;

    private boolean configurado;

    @PostConstruct
    void inicializar() {
        configurado = !accountSid.isBlank() && !authToken.isBlank() && !numeroOrigenWhatsApp.isBlank();
        if (configurado) {
            Twilio.init(accountSid, authToken);
        } else {
            log.info("WhatsApp deshabilitado: faltan credenciales de Twilio (TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/TWILIO_NUMERO_ORIGEN_WHATSAPP)");
        }
    }

    public boolean estaConfigurado() {
        return configurado;
    }

    public void enviar(String telefonoDestino, String texto) {
        if (!configurado) {
            throw new IllegalStateException("El canal WhatsApp no está configurado (faltan credenciales de Twilio)");
        }
        try {
            Message.creator(new PhoneNumber("whatsapp:" + telefonoDestino), new PhoneNumber(numeroOrigenWhatsApp), texto).create();
        } catch (Exception e) {
            throw new IllegalStateException("Error enviando WhatsApp a " + telefonoDestino + ": " + e.getMessage(), e);
        }
    }
}
