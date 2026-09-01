package com.fktlomas.turnos.dto.kinesiologia;

import com.fktlomas.turnos.domain.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record SesionKinesiologiaDto(Long turnoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String estado) {
    public static SesionKinesiologiaDto de(Turno t) {
        return new SesionKinesiologiaDto(t.getId(), t.getFecha(), t.getHoraInicio(), t.getHoraFin(), t.getEstado().name());
    }
}
