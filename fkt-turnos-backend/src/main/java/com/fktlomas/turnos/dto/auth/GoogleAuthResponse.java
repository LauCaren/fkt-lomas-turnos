package com.fktlomas.turnos.dto.auth;

/**
 * Forma "aplanada" en vez de anidar un {@link AuthResponse}: cuando {@code requierePerfil} es
 * true, todos los campos de sesión (token, role, etc.) van en null — todavía no se creó ninguna
 * cuenta — y solo importan {@code nombreSugerido}/{@code email} para precargar el mini-formulario
 * del frontend. Cuando es false, es una sesión normal recién creada o ya existente.
 */
public record GoogleAuthResponse(
        boolean requierePerfil,
        String nombreSugerido,
        String email,
        String token,
        String nombreCompleto,
        String role,
        boolean mfaSetupRequerido
) {
    public static GoogleAuthResponse requierePerfil(String nombreSugerido, String email) {
        return new GoogleAuthResponse(true, nombreSugerido, email, null, null, null, false);
    }

    public static GoogleAuthResponse sesion(AuthResponse auth) {
        return new GoogleAuthResponse(false, null, null, auth.token(), auth.nombreCompleto(), auth.role(), auth.mfaSetupRequerido());
    }
}
