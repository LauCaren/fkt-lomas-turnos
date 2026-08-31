package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoDto(
        Long id,
        Long medicoId,
        String medicoNombre,
        Long especialidadId,
        String especialidadNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado,
        String motivoConsulta
) {
    public static TurnoDto de(Turno t) {
        return new TurnoDto(
                t.getId(),
                t.getMedico().getId(),
                t.getMedico().getNombreCompleto(),
                t.getMedico().getEspecialidad().getId(),
                t.getMedico().getEspecialidad().getNombre(),
                t.getFecha(),
                t.getHoraInicio(),
                t.getHoraFin(),
                t.getEstado().name(),
                t.getMotivoConsulta()
        );
    }
}
