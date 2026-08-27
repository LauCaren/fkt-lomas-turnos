package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Postulacion;

import java.time.Instant;

public record PostulacionDto(
        Long id, String nombreCompleto, String email, String telefono, String mensaje,
        String cvNombreOriginal, long cvSize, boolean revisado, Instant createdAt
) {
    public static PostulacionDto de(Postulacion p) {
        return new PostulacionDto(p.getId(), p.getNombreCompleto(), p.getEmail(), p.getTelefono(), p.getMensaje(),
                p.getCvNombreOriginal(), p.getCvSize(), p.isRevisado(), p.getCreatedAt());
    }
}
