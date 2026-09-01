package com.fktlomas.turnos.service;

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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionTurnoService {

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    @Async
    public void confirmarReserva(Usuario paciente, String medicoNombre, String especialidadNombre,
                                  LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String texto = """
                Hola %s,

                Tu turno fue reservado con éxito:
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s

                Si necesitás cancelarlo, podés hacerlo desde tu cuenta.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
        enviarSegunPreferencia(paciente, "Confirmación de turno — FKT Lomas", texto);

        emailService.notificarNuevaReservaOnline(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
    }

    @Async
    public void confirmarCancelacion(Usuario paciente, String medicoNombre, String especialidadNombre,
                                      LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String texto = """
                Hola %s,

                Tu turno fue cancelado:
                Médico: %s
                Especialidad: %s
                Fecha: %s
                Horario: %s a %s

                Si querés, podés sacar un turno nuevo desde tu cuenta.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
        enviarSegunPreferencia(paciente, "Turno cancelado — FKT Lomas", texto);

        emailService.notificarCancelacionOnline(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
    }

    @Async
    public void avisarPlanKinesiologiaValidado(Usuario paciente, int sesionesGeneradas, int cantidadSesiones) {
        String texto = """
                Hola %s,

                Tu plan de kinesiología fue aprobado y ya se generaron %d de %d sesiones.

                Podés ver los días y horarios desde tu cuenta, en "Kinesiología".

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), sesionesGeneradas, cantidadSesiones);
        enviarSegunPreferencia(paciente, "Tu plan de kinesiología fue aprobado — FKT Lomas", texto);
    }

    @Async
    public void avisarPlanKinesiologiaRechazado(Usuario paciente, String motivo) {
        String texto = """
                Hola %s,

                Tu plan de kinesiología no pudo aprobarse. Motivo: %s

                Podés volver a cargarlo desde tu cuenta, en "Kinesiología", o contactarnos si tenés dudas.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), motivo);
        enviarSegunPreferencia(paciente, "Tu plan de kinesiología necesita otra revisión — FKT Lomas", texto);
    }

    @Async
    public void avisarTurnoLiberado(Usuario paciente, String medicoNombre, String especialidadNombre, LocalDate fecha) {
        String texto = """
                Hola %s,

                Se liberó un turno de %s con %s para el %s.

                Entrá a tu cuenta para reservarlo antes de que lo tome otro paciente — es por orden de llegada.

                FKT Lomas
                """.formatted(paciente.getNombreCompleto(), especialidadNombre, medicoNombre, fecha);
        enviarSegunPreferencia(paciente, "Se liberó un turno — FKT Lomas", texto);
    }

    private void enviarSegunPreferencia(Usuario paciente, String asunto, String texto) {
        try {
            switch (paciente.getCanalNotificacionPreferido()) {
                case EMAIL -> emailService.enviarRecordatorioSync(paciente.getEmail(), asunto, texto);
                case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), texto);
            }
        } catch (Exception e) {
            log.warn("No se pudo notificar a {} por {}: {}", paciente.getEmail(), paciente.getCanalNotificacionPreferido(), e.getMessage());
        }
    }
}
