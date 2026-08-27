package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.ReglaDisponibilidad;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ReglaDisponibilidadDto(Long id, Long medicoId, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {
    public static ReglaDisponibilidadDto de(ReglaDisponibilidad r) {
        return new ReglaDisponibilidadDto(r.getId(), r.getMedico().getId(), r.getDiaSemana(), r.getHoraInicio(), r.getHoraFin());
    }
}
