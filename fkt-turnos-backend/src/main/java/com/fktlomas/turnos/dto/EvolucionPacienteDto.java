package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.EvolucionClinica;

import java.time.Instant;
import java.time.LocalDate;

/** Vista de una evolución clínica propia para el paciente — ver EvolucionClinicaRepository.findByPacienteIdOrderByCreatedAtDesc. */
public record EvolucionPacienteDto(
        Long id,
        String medicoNombre,
        String especialidadNombre,
        LocalDate fechaTurno,
        String texto,
        Instant createdAt
) {
    public static EvolucionPacienteDto de(EvolucionClinica e) {
        return new EvolucionPacienteDto(
                e.getId(),
                e.getMedico().getNombreCompleto(),
                e.getMedico().getEspecialidad().getNombre(),
                e.getTurno().getFecha(),
                e.getTexto(),
                e.getCreatedAt()
        );
    }
}
