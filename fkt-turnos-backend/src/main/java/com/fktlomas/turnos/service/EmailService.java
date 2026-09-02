package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.MensajeContacto;
import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.domain.Usuario;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    // Plantillas HTML (diseño de mail de FKT Lomas) leídas una sola vez del classpath —
    // ver src/main/resources/email-templates/. Placeholders: {paciente} {profesional}
    // {especialidad} {fecha} {hora} {token} {frontendUrl}.
    private static final String PLANTILLA_CONFIRMACION_HTML = leerPlantilla("confirmacion-turno.html");
    private static final String PLANTILLA_RECORDATORIO_HTML = leerPlantilla("recordatorio-turno.html");

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.admin.notify-email}")
    private String adminNotifyEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void enviarConfirmacionTurno(Usuario paciente, String medicoNombre, String especialidadNombre,
                                         LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String html = renderizarConfirmacionHtml(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio);
        enviarHtml(paciente.getEmail(), "Turno confirmado — FKT Lomas", html);
    }

    /**
     * Igual que {@link #enviarConfirmacionTurno}, pero sincrónico y tira excepción si falla —
     * lo usa NotificacionTurnoService cuando el paciente eligió email como canal preferido,
     * para poder loguear el fallo puntual sin afectar la reserva en sí.
     */
    public void enviarConfirmacionTurnoHtmlSync(Usuario paciente, String medicoNombre, String especialidadNombre,
                                                 LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String html = renderizarConfirmacionHtml(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio);
        enviarHtmlSync(paciente.getEmail(), "Turno confirmado — FKT Lomas", html);
    }

    /** Sincrónico y tira excepción si falla — lo usa RecordatorioService, que necesita saber si el envío funcionó para marcar el Recordatorio como ENVIADO o FALLIDO. */
    public void enviarRecordatorioTurnoHtmlSync(String destinatario, String paciente, String profesional, String especialidad,
                                                 LocalDate fecha, LocalTime horaInicio, String token) {
        String html = PLANTILLA_RECORDATORIO_HTML
                .replace("{paciente}", escapeHtml(paciente))
                .replace("{profesional}", escapeHtml(profesional))
                .replace("{especialidad}", escapeHtml(especialidad))
                .replace("{fecha}", fecha.format(FORMATO_FECHA))
                .replace("{hora}", horaInicio.format(FORMATO_HORA))
                .replace("{token}", token)
                .replace("{frontendUrl}", frontendUrl);
        enviarHtmlSync(destinatario, "Recordatorio de turno — FKT Lomas", html);
    }

    private String renderizarConfirmacionHtml(String paciente, String profesional, String especialidad, LocalDate fecha, LocalTime horaInicio) {
        return PLANTILLA_CONFIRMACION_HTML
                .replace("{paciente}", escapeHtml(paciente))
                .replace("{profesional}", escapeHtml(profesional))
                .replace("{especialidad}", escapeHtml(especialidad))
                .replace("{fecha}", fecha.format(FORMATO_FECHA))
                .replace("{hora}", horaInicio.format(FORMATO_HORA))
                .replace("{frontendUrl}", frontendUrl);
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String leerPlantilla(String nombre) {
        try (InputStream in = EmailService.class.getResourceAsStream("/email-templates/" + nombre)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró la plantilla de email: " + nombre);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo la plantilla de email: " + nombre, e);
        }
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
     * Si procesarPendientes() (RecordatorioService) falla antes de llegar a mandar un solo
     * recordatorio — falta la fila de configuración, la base no responde, etc. — el job
     * simplemente deja de correr en silencio hasta que un paciente falta a un turno porque
     * nunca le llegó el aviso. RecordatorioService enfría este envío a como mucho uno por
     * hora, para no inundar al admin si la falla persiste corrida tras corrida.
     */
    public void notificarFalloJobRecordatorios(String motivo) {
        String asunto = "El job de recordatorios de turno falló — FKT Lomas";
        String cuerpo = """
                El job automático que envía los recordatorios de turno (email/WhatsApp) falló
                y no procesó nada en esta corrida.

                Motivo: %s

                Mientras no se resuelva, los pacientes con turno próximo no van a recibir
                recordatorio. Revisá los logs del backend para más detalle.
                """.formatted(motivo);
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

    private void enviarHtml(String to, String asunto, String html) {
        if (!mailEnabled) {
            log.info("Envío de email deshabilitado (app.mail.enabled=false). Destinatario: {}, asunto: {}", to, asunto);
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el email HTML a {}: {}", to, e.getMessage());
        }
    }

    private void enviarHtmlSync(String to, String asunto, String html) {
        if (!mailEnabled) {
            throw new IllegalStateException("El canal Email está deshabilitado (app.mail.enabled=false)");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException("Error enviando email a " + to + ": " + e.getMessage(), e);
        }
    }
}
