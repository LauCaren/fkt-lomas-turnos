package com.fktlomas.turnos.dto.auth;

import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Un solo endpoint cubre las tres idas y vueltas de "Iniciar sesión con Google":
 * 1) primer llamado, solo {@code credential} — el backend verifica el ID token y responde
 *    directamente (cuenta existente) o pide perfil ({@link GoogleAuthResponse#requierePerfil()}).
 * 2) si pide perfil (cuenta nueva), el frontend reenvía el mismo {@code credential} más
 *    dni/obraSocial/telefono/genero/aceptaPoliticaPrivacidad.
 * 3) si la cuenta ya existente tiene MFA activado, el frontend reenvía el mismo
 *    {@code credential} más {@code codigoMfa} (igual que {@link LoginRequest}).
 * Todos los campos salvo {@code credential} son opcionales a nivel de Bean Validation a
 * propósito — cuáles son obligatorios depende de en qué paso del flujo está, y eso lo decide
 * AuthService.loginConGoogle, no las anotaciones acá.
 */
public record GoogleAuthRequest(
        @NotBlank(message = "Falta el token de Google")
        String credential,

        @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
        String dni,

        @Size(max = 100)
        String obraSocial,

        @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono no es válido")
        String telefono,

        Genero genero,

        Boolean aceptaPoliticaPrivacidad,

        String codigoMfa,

        /** Campo honeypot, igual que en {@link RegistroRequest}: invisible para personas. */
        String sitioWeb
) {
    public boolean pareceSpam() {
        return sitioWeb != null && !sitioWeb.isBlank();
    }
}
