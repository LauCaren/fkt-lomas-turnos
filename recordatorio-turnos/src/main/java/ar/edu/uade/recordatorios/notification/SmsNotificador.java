package ar.edu.uade.recordatorios.notification;

import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Turno;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsNotificador implements Notificador {

    private final String numeroOrigen;
    private final String plantillaMensaje;

    public SmsNotificador(String accountSid, String authToken, String numeroOrigen, String plantillaMensaje) {
        Twilio.init(accountSid, authToken);
        this.numeroOrigen = numeroOrigen;
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

    private void enviarTexto(String destino, String texto) throws NotificacionException {
        try {
            Message.creator(new PhoneNumber(destino), new PhoneNumber(numeroOrigen), texto).create();
        } catch (Exception e) {
            throw new NotificacionException("Error enviando SMS a " + destino, e);
        }
    }
}
