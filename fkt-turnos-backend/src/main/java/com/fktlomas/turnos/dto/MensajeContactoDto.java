package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.MensajeContacto;

import java.time.Instant;

public record MensajeContactoDto(
        Long id, String nombre, String email, String telefono, String mensaje, boolean leido, Instant createdAt
) {
    public static MensajeContactoDto de(MensajeContacto m) {
        return new MensajeContactoDto(m.getId(), m.getNombre(), m.getEmail(), m.getTelefono(), m.getMensaje(), m.isLeido(), m.getCreatedAt());
    }
}
