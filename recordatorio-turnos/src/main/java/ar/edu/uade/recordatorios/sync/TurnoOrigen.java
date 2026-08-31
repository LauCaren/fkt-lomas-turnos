package ar.edu.uade.recordatorios.sync;

import java.time.LocalDateTime;

/** Turno tal como lo devuelve el sistema de turnos real. */
public record TurnoOrigen(
    Long id,
    Long pacienteId,
    LocalDateTime fechaHora,
    String profesional,
    String especialidad,
    String consultorio
) {
}
