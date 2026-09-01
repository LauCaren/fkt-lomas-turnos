package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.MensajeContacto;
import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.domain.Usuario;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    public void enviarRecuperacionPassword(Usuario usuario, String linkRecuperacion) {
        String asunto = "Recuperar contraseña — FKT Lomas";
        String cuerpo = """
                Hola %s,

                Pediste restablecer tu contraseña. Entrá a este link para elegir una nueva
                (válido por 1 hora):

                %s

                Si no fuiste vos, ignorá este email — tu contraseña actual sigue funcionando.

                FKT Lomas
                """.formatted(usuario.getNombreCompleto(), linkRecuperacion);
        enviar(usuario.getEmail(), asunto, cuerpo);
    }

    @Async
    public void notificarNuevaReservaOnline(Usuario paciente, String medicoNombre, String especialidadNombre,
                                             LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String asunto = "Nueva reserva online — " + paciente.getNombreCompleto();
        String cuerpo = """
                Se reservó un turno desde la web:

                Paciente: %s
                DNI: %s
                Teléfono: %s
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s
                """.formatted(paciente.getNombreCompleto(), paciente.getDni(), paciente.getTelefono(),
                medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
        enviar(adminNotifyEmail, asunto, cuerpo);
    }

    public void notificarCancelacionOnline(Usuario paciente, String medicoNombre, String especialidadNombre,
                                            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String asunto = "Cancelación online — " + paciente.getNombreCompleto();
        String cuerpo = """
                Un paciente canceló su turno desde la web:

                Paciente: %s
                DNI: %s
                Teléfono: %s
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s

                Ese horario quedó disponible de nuevo.
                """.formatted(paciente.getNombreCompleto(), paciente.getDni(), paciente.getTelefono(),
                medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
        enviar(adminNotifyEmail, asunto, cuerpo);
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

    /**
     * Si el envío mensual automático de caja falla (ver ReporteCajaProgramadoService), esto
     * es lo único que se entera alguien — sin esto quedaba solo en un log que nadie revisa.
     * Va al mail interno de la clínica, no al destinatario configurado del reporte (que puede
     * ser justamente la dirección que está fallando).
     */
    public void notificarFalloReporteMensualCaja(String destinatarioConfigurado, String motivo) {
        String asunto = "No se pudo enviar el reporte mensual de caja — FKT Lomas";
        String cuerpo = """
                El envío automático del reporte mensual de caja falló.

                Destinatario configurado: %s
                Motivo: %s

                Podés generar el reporte a mano desde el panel de administración,
                pestaña "Reportes", y revisar la dirección configurada en "Caja".
                """.formatted(destinatarioConfigurado, motivo);
        enviar(adminNotifyEmail, asunto, cuerpo);
    }

    /**
     * A diferencia de los métodos de arriba (@Async, no le importa a nadie el
     * resultado), este es sincrónico y tira excepción si falla: lo usa
     * RecordatorioService, que necesita saber en el momento si el envío
     * funcionó para marcar el Recordatorio como ENVIADO o FALLIDO.
     */
    public void enviarRecordatorioSync(String destinatario, String asunto, String cuerpo) {
        if (!mailEnabled) {
            throw new IllegalStateException("El canal Email está deshabilitado (app.mail.enabled=false)");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(destinatario);
            message.setSubject(asunto);
            message.setText(cuerpo);
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Error enviando email a " + destinatario + ": " + e.getMessage(), e);
        }
    }

    /** Sincrónico y tira excepción si falla — lo usa ReporteCajaProgramadoService, que necesita
     * saber si el envío mensual funcionó para poder loguearlo como tal. */
    public void enviarConAdjuntoSync(String to, String asunto, String cuerpo, String nombreArchivo, String contentType, byte[] adjunto) {
        if (!mailEnabled) {
            throw new IllegalStateException("El canal Email está deshabilitado (app.mail.enabled=false)");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(asunto);
            helper.setText(cuerpo);
            helper.addAttachment(nombreArchivo, new ByteArrayResource(adjunto), contentType);
            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException("Error enviando email con adjunto a " + to + ": " + e.getMessage(), e);
        }
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
