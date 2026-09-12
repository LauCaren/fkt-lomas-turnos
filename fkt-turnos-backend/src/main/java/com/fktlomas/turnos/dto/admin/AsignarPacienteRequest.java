package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotNull;

/** Asignar un turno DISPONIBLE directo a un paciente ya registrado (ej. desde la lista de espera), sin pasar por el alta/lookup por DNI de ReservarParaPacienteRequest. */
public record AsignarPacienteRequest(
        @NotNull(message = "Elegí un paciente")
        Long pacienteId
) {
}
