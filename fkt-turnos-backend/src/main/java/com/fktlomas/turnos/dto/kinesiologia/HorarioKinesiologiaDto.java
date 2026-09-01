package com.fktlomas.turnos.dto.kinesiologia;

import com.fktlomas.turnos.domain.PlanKinesiologiaHorario;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioKinesiologiaDto(DayOfWeek diaSemana, LocalTime horaInicio) {
    public static HorarioKinesiologiaDto de(PlanKinesiologiaHorario h) {
        return new HorarioKinesiologiaDto(h.getDiaSemana(), h.getHoraInicio());
    }
}
