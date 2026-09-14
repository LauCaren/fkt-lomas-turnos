package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Notificacion;

import java.time.Instant;

public record NotificacionDto(
        Long id,
        String tipo,
        String mensaje,
        Long turnoId,
        boolean leida,
        Instant createdAt
) {
    public static NotificacionDto de(Notificacion n) {
        return new NotificacionDto(n.getId(), n.getTipo().name(), n.getMensaje(), n.getTurnoId(), n.isLeida(), n.getCreatedAt());
    }
}
