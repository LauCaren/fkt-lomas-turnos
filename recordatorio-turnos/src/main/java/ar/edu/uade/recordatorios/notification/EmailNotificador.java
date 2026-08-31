package ar.edu.uade.recordatorios.notification;

import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Turno;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailNotificador implements Notificador {

    private final String host;
    private final String usuario;
    private final String password;
    private final String plantillaMensaje;

    public EmailNotificador(String host, String usuario, String password, String plantillaMensaje) {
        this.host = host;
        this.usuario = usuario;
        this.password = password;
        this.plantillaMensaje = plantillaMensaje;
    }

    @Override
    public void enviar(Turno turno) throws NotificacionException {
        enviarCorreo(turno.getPaciente().getEmail(), "Recordatorio de turno",
            PlantillaMensaje.construir(plantillaMensaje, turno));
    }

    @Override
    public void enviarMensaje(Paciente paciente, String mensaje) throws NotificacionException {
        enviarCorreo(paciente.getEmail(), "Mensaje de FKT Lomas", mensaje);
    }

    private void enviarCorreo(String destino, String asunto, String cuerpo) throws NotificacionException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, password);
            }
        });

        try {
            Message mensaje = new MimeMessage(session);
            mensaje.setFrom(new InternetAddress(usuario));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            Transport.send(mensaje);

        } catch (MessagingException e) {
            throw new NotificacionException("Error enviando email a " + destino, e);
        }
    }
}
