package com.fktlomas.turnos.dto.kinesiologia;

import com.fktlomas.turnos.domain.PlanKinesiologia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PlanKinesiologiaDto(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        String pacienteDni,
        Long medicoId,
        String medicoNombre,
        int cantidadSesiones,
        int sesionesGeneradas,
        LocalDate fechaInicio,
        String estado,
        String motivoRechazo,
        Instant createdAt,
        List<HorarioKinesiologiaDto> horarios,
        List<SesionKinesiologiaDto> sesiones
) {
    public static PlanKinesiologiaDto de(PlanKinesiologia p, List<SesionKinesiologiaDto> sesiones) {
        return new PlanKinesiologiaDto(
                p.getId(),
                p.getPaciente().getId(),
                p.getPaciente().getNombreCompleto(),
                p.getPaciente().getDni(),
                p.getMedico().getId(),
                p.getMedico().getNombreCompleto(),
                p.getCantidadSesiones(),
                p.getSesionesGeneradas(),
                p.getFechaInicio(),
                p.getEstado().name(),
                p.getMotivoRechazo(),
                p.getCreatedAt(),
                p.getHorarios().stream().map(HorarioKinesiologiaDto::de).toList(),
                sesiones
        );
    }
}
