package com.fktlomas.turnos.dto.resena;

import com.fktlomas.turnos.domain.Resena;

import java.time.Instant;

/** Vista completa — moderación (admin/secretaria) y el propio paciente sobre su reseña. Para lo público, ver ResenaPublicaDto. */
public record ResenaDto(
        Long id,
        Long turnoId,
        Long pacienteId,
        String pacienteNombre,
        Long medicoId,
        String medicoNombre,
        int puntaje,
        String comentario,
        String estado,
        String motivoRechazo,
        Instant createdAt,
        Instant resueltaAt
) {
    public static ResenaDto de(Resena r) {
        return new ResenaDto(
                r.getId(),
                r.getTurno().getId(),
                r.getPaciente().getId(),
                r.getPaciente().getNombreCompleto(),
                r.getMedico().getId(),
                r.getMedico().getNombreCompleto(),
                r.getPuntaje(),
                r.getComentario(),
                r.getEstado().name(),
                r.getMotivoRechazo(),
                r.getCreatedAt(),
                r.getResueltaAt()
        );
    }
}
