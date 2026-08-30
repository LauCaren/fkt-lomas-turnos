package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Recordatorio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record RecordatorioDto(
        Long id,
        Long turnoId,
        String pacienteNombre,
        String medicoNombre,
        String especialidadNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        String canal,
        String estado,
        int intentos,
        String errorMensaje,
        Instant fechaEnvio
) {
    public static RecordatorioDto de(Recordatorio r) {
        var turno = r.getTurno();
        return new RecordatorioDto(
                r.getId(),
                turno.getId(),
                turno.getPaciente().getNombreCompleto(),
                turno.getMedico().getNombreCompleto(),
                turno.getMedico().getEspecialidad().getNombre(),
                turno.getFecha(),
                turno.getHoraInicio(),
                r.getCanal().name(),
                r.getEstado().name(),
                r.getIntentos(),
                r.getErrorMensaje(),
                r.getFechaEnvio()
        );
    }
}
