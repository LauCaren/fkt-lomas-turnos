package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;

import java.time.Instant;

/** Vista genérica de una cuenta de staff (MEDICO/CONTADOR/SECRETARIA/ADMIN) para las pantallas de gestión de cuentas del admin. */
public record CuentaStaffDto(
        Long id,
        String nombreCompleto,
        String email,
        Role role,
        boolean activo,
        boolean mfaHabilitado,
        Instant createdAt
) {
    public static CuentaStaffDto de(Usuario u) {
        return new CuentaStaffDto(u.getId(), u.getNombreCompleto(), u.getEmail(), u.getRole(),
                u.isActivo(), u.isMfaHabilitado(), u.getCreatedAt());
    }
}
