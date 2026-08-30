package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Usuario;

public record PerfilDto(
        Long id,
        String dni,
        String nombreCompleto,
        String email,
        String telefono,
        String genero,
        String obraSocial,
        String role,
        boolean mfaHabilitado
) {
    public static PerfilDto de(Usuario u) {
        return new PerfilDto(
                u.getId(),
                u.getDni(),
                u.getNombreCompleto(),
                u.getEmail(),
                u.getTelefono(),
                u.getGenero() != null ? u.getGenero().name() : null,
                u.getObraSocial(),
                u.getRole().name(),
                u.isMfaHabilitado()
        );
    }
}
