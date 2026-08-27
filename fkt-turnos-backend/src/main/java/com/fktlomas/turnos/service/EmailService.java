package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.MensajeContacto;
import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.domain.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.admin.notify-email}")
    private String adminNotifyEmail;

    @Async
    public void enviarConfirmacionTurno(Usuario paciente, String medicoNombre, String especialidadNombre,
                                         LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String asunto = "Confirmación de turno — FKT Lomas";
        String cuerpo = """
                Hola %s,

                Tu turno fue reservado con éxito:
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s

                Si necesitás cancelarlo, podés hacerlo desde tu cuenta.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
        enviar(paciente.getEmail(), asunto, cuerpo);
    }

    @Async
    public void notificarNuevoMensajeContacto(MensajeContacto mensaje) {
        String asunto = "Nueva consulta por alquiler de consultorio";
        String cuerpo = """
                Nombre: %s
                Email: %s
                Teléfono: %s

                Mensaje:
                %s
                """.formatted(mensaje.getNombre(), mensaje.getEmail(), mensaje.getTelefono(), mensaje.getMensaje());
        enviar(adminNotifyEmail, asunto, cuerpo);
    }

    @Async
    public void notificarNuevaPostulacion(Postulacion postulacion) {
        String asunto = "Nueva postulación recibida — " + postulacion.getNombreCompleto();
        String cuerpo = """
                Nombre: %s
                Email: %s
                Teléfono: %s

                Mensaje:
                %s

                El CV (%s) está disponible para descargar desde el panel de administración.
                """.formatted(postulacion.getNombreCompleto(), postulacion.getEmail(), postulacion.getTelefono(),
                postulacion.getMensaje(), postulacion.getCvNombreOriginal());
        enviar(adminNotifyEmail, asunto, cuerpo);
    }

    private void enviar(String to, String asunto, String cuerpo) {
        if (!mailEnabled) {
            log.info("Envío de email deshabilitado (app.mail.enabled=false). Destinatario: {}, asunto: {}", to, asunto);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(asunto);
            message.setText(cuerpo);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("No se pudo enviar el email a {}: {}", to, e.getMessage());
        }
    }
}
