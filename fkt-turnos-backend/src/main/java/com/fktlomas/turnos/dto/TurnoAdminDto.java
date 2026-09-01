package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoAdminDto(
        Long id,
        Long medicoId,
        String medicoNombre,
        String especialidadNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado,
        String motivoConsulta,
        Long pacienteId,
        String pacienteNombre,
        String pacienteDni,
        String pacienteTelefono,
        String pacienteObraSocial,
        String estadoAsistencia
) {
    public static TurnoAdminDto de(Turno t) {
        Usuario p = t.getPaciente();
        return new TurnoAdminDto(
                t.getId(),
                t.getMedico().getId(),
                t.getMedico().getNombreCompleto(),
                t.getMedico().getEspecialidad().getNombre(),
                t.getFecha(),
                t.getHoraInicio(),
                t.getHoraFin(),
                t.getEstado().name(),
                t.getMotivoConsulta(),
                p != null ? p.getId() : null,
                p != null ? p.getNombreCompleto() : null,
                p != null ? p.getDni() : null,
                p != null ? p.getTelefono() : null,
                p != null ? p.getObraSocial() : null,
                t.getEstadoAsistencia().name()
        );
    }
}
