package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Usuario;

public record PacienteBusquedaDto(Long id, String nombreCompleto, String dni, String telefono, String canalNotificacionPreferido) {
    public static PacienteBusquedaDto de(Usuario u) {
        return new PacienteBusquedaDto(u.getId(), u.getNombreCompleto(), u.getDni(), u.getTelefono(),
                u.getCanalNotificacionPreferido().name());
    }
}
