package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.SolicitudBajaCuenta;

import java.time.Instant;

public record SolicitudBajaDto(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String usuarioEmail,
        String motivo,
        String estado,
        String motivoRechazo,
        Instant createdAt,
        Instant resueltaAt
) {
    public static SolicitudBajaDto de(SolicitudBajaCuenta s) {
        return new SolicitudBajaDto(
                s.getId(),
                s.getUsuario().getId(),
                s.getUsuario().getNombreCompleto(),
                s.getUsuario().getEmail(),
                s.getMotivo(),
                s.getEstado().name(),
                s.getMotivoRechazo(),
                s.getCreatedAt(),
                s.getResueltaAt()
        );
    }
}
