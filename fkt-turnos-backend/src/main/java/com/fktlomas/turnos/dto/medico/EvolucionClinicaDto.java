package com.fktlomas.turnos.dto.medico;

import com.fktlomas.turnos.domain.EvolucionClinica;

import java.time.Instant;

public record EvolucionClinicaDto(
        Long id,
        Long turnoId,
        Long pacienteId,
        String pacienteNombre,
        String texto,
        Instant createdAt
) {
    public static EvolucionClinicaDto de(EvolucionClinica e) {
        return new EvolucionClinicaDto(
                e.getId(),
                e.getTurno().getId(),
                e.getPaciente().getId(),
                e.getPaciente().getNombreCompleto(),
                e.getTexto(),
                e.getCreatedAt()
        );
    }
}
