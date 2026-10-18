package com.fktlomas.turnos.service;

import com.fktlomas.turnos.util.DatosPersonales;
import com.fktlomas.turnos.domain.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Confirmaciones de turno (reserva y cancelación) al paciente por su canal
 * preferido, y aviso a la clínica cuando entra una reserva por la web.
 * Todo best-effort y asincrónico: si el envío falla, se loguea pero no
 * afecta la reserva/cancelación en sí, que ya se hizo antes de llamar acá.
 *
 * El email usa el diseño de marca de FKT Lomas (ver EmailService); WhatsApp
 * sigue en texto plano, Twilio no soporta HTML. El fallback de canal (si
 * falla el preferido, reintentar por el otro) vive en CanalFallbackService,
 * compartido con RecordatorioService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionTurnoService {

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final CanalFallbackService canalFallbackService;

    @Async
    public void confirmarReserva(Usuario paciente, String medicoNombre, String especialidadNombre,
                                  LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        enviarConFallback(paciente, canal -> {
            switch (canal) {
                case EMAIL -> emailService.enviarConfirmacionTurnoHtmlSync(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), textoConfirmacion(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin));
            }
        });

        emailService.notificarNuevaReservaOnline(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
    }

    @Async
    public void confirmarCancelacion(Usuario paciente, String medicoNombre, String especialidadNombre,
                                      LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        enviarConFallback(paciente, canal -> {
            switch (canal) {
                case EMAIL -> emailService.enviarCancelacionTurnoHtmlSync(paciente, medicoNombre, especialidadNombre, fecha, horaInicio);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), textoCancelacion(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin));
            }
        });

        emailService.notificarCancelacionOnline(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
    }

    @Async
    public void avisarReprogramacion(Usuario paciente, String medicoNombre, String especialidadNombre,
                                      LocalDate fechaNueva, LocalTime horaInicioNueva, LocalTime horaFinNueva) {
        enviarConFallback(paciente, canal -> {
            switch (canal) {
                case EMAIL -> emailService.enviarReprogramacionTurnoHtmlSync(paciente, medicoNombre, especialidadNombre, fechaNueva, horaInicioNueva, horaFinNueva);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), textoReprogramacion(paciente, medicoNombre, especialidadNombre, fechaNueva, horaInicioNueva, horaFinNueva));
            }
        });
    }

    @Async
    public void avisarPlanKinesiologiaValidado(Usuario paciente, int sesionesGeneradas, int cantidadSesiones) {
        enviarConFallback(paciente, canal -> {
            switch (canal) {
                case EMAIL -> emailService.enviarPlanValidadoHtmlSync(paciente, sesionesGeneradas, cantidadSesiones);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), textoPlanValidado(paciente, sesionesGeneradas, cantidadSesiones));
            }
        });
    }

    @Async
    public void avisarPlanKinesiologiaRechazado(Usuario paciente, String motivo) {
        enviarConFallback(paciente, canal -> {
            switch (canal) {
                case EMAIL -> emailService.enviarPlanRechazadoHtmlSync(paciente, motivo);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), textoPlanRechazado(paciente, motivo));
            }
        });
    }

    @Async
    public void avisarTurnoLiberado(Usuario paciente, String medicoNombre, String especialidadNombre, LocalDate fecha) {
        enviarConFallback(paciente, canal -> {
            switch (canal) {
                case EMAIL -> emailService.enviarTurnoLiberadoHtmlSync(paciente, medicoNombre, especialidadNombre, fecha);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), textoTurnoLiberado(paciente, medicoNombre, especialidadNombre, fecha));
            }
        });
    }

    /** Best-effort: si fallan tanto el canal preferido como el alternativo, se loguea y no se propaga — la operación real ya se hizo antes de notificar. */
    private void enviarConFallback(Usuario paciente, CanalFallbackService.EnvioPorCanal envio) {
        try {
            canalFallbackService.enviarConFallback(paciente, envio);
        } catch (Exception e) {
            log.warn("No se pudo notificar a {} por ningún canal: {}", DatosPersonales.enmascararEmail(paciente.getEmail()), e.getMessage());
        }
    }

    private String textoConfirmacion(Usuario paciente, String medicoNombre, String especialidadNombre,
                                      LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return """
                Hola %s,

                Tu turno fue reservado con éxito:
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s

                Si necesitás cancelarlo, podés hacerlo desde tu cuenta.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
    }

    private String textoCancelacion(Usuario paciente, String medicoNombre, String especialidadNombre,
                                     LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return """
                Hola %s,

                Tu turno fue cancelado:
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s

                Si querés, podés sacar un turno nuevo desde tu cuenta.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
    }

    private String textoReprogramacion(Usuario paciente, String medicoNombre, String especialidadNombre,
                                        LocalDate fechaNueva, LocalTime horaInicioNueva, LocalTime horaFinNueva) {
        return """
                Hola %s,

                Tu turno fue reprogramado:
                Médico: %s
                Especialidad: %s
                Nueva fecha: %s
                Nuevo horario: %s a %s

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fechaNueva, horaInicioNueva, horaFinNueva);
    }

    private String textoPlanValidado(Usuario paciente, int sesionesGeneradas, int cantidadSesiones) {
        return """
                Hola %s,

                Tu plan de kinesiología fue aprobado y ya se generaron %d de %d sesiones.

                Podés ver los días y horarios desde tu cuenta, en "Kinesiología".

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), sesionesGeneradas, cantidadSesiones);
    }

    private String textoPlanRechazado(Usuario paciente, String motivo) {
        return """
                Hola %s,

                Tu plan de kinesiología no pudo aprobarse. Motivo: %s

                Podés volver a cargarlo desde tu cuenta, en "Kinesiología", o contactarnos si tenés dudas.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), motivo);
    }

    private String textoTurnoLiberado(Usuario paciente, String medicoNombre, String especialidadNombre, LocalDate fecha) {
        return """
                Hola %s,

                Se liberó un turno de %s con %s para el %s.

                Entrá a tu cuenta para reservarlo antes de que lo tome otro paciente — es por orden de llegada.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), especialidadNombre, medicoNombre, fecha);
    }
}
