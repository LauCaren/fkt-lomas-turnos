package com.fktlomas.turnos.dto.auth;

public record AuthResponse(
        String token,
        String nombreCompleto,
        String email,
        String role,
        boolean mfaSetupRequerido
) {
}
