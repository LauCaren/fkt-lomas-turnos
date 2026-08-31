package ar.edu.uade.recordatorios.notification;

import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Turno;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class WhatsAppNotificador implements Notificador {

    private final String numeroOrigenWhatsApp; // formato requerido por Twilio: "whatsapp:+549..."
    private final String plantillaMensaje;

    public WhatsAppNotificador(String accountSid, String authToken, String numeroOrigenWhatsApp, String plantillaMensaje) {
        Twilio.init(accountSid, authToken);
        this.numeroOrigenWhatsApp = numeroOrigenWhatsApp;
        this.plantillaMensaje = plantillaMensaje;
    }

    @Override
    public void enviar(Turno turno) throws NotificacionException {
        enviarTexto(turno.getPaciente().getTelefono(), PlantillaMensaje.construir(plantillaMensaje, turno));
    }

    @Override
    public void enviarMensaje(Paciente paciente, String mensaje) throws NotificacionException {
        enviarTexto(paciente.getTelefono(), mensaje);
    }

    private void enviarTexto(String telefono, String texto) throws NotificacionException {
        try {
            String destino = "whatsapp:" + telefono;
            Message.creator(new PhoneNumber(destino), new PhoneNumber(numeroOrigenWhatsApp), texto).create();
        } catch (Exception e) {
            throw new NotificacionException("Error enviando WhatsApp a " + telefono, e);
        }
    }
}
