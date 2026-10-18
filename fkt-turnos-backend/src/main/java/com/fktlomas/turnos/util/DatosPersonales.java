package com.fktlomas.turnos.util;

import java.util.Locale;

/**
 * Helpers chicos para tratar datos personales de forma consistente en todo el backend.
 *
 * <p>Dos usos:
 * <ul>
 *   <li><b>Normalizar el email de entrada</b> antes de buscarlo en la base. La columna se guarda
 *       siempre en minúsculas (ver {@code Usuario.normalizarEmail()}), pero las búsquedas
 *       ({@code findByEmail}, {@code existsByEmail}) son exactas: sin normalizar la entrada, un
 *       paciente que escribe "Juan@Gmail.com" (el teclado del celular pone la mayúscula solo) no
 *       podía entrar aunque su cuenta existiera.</li>
 *   <li><b>Enmascarar emails y teléfonos en los logs.</b> Los logs suelen terminar en servicios de
 *       terceros (el host, un agregador) y se guardan más tiempo que los datos en sí: por el
 *       principio de minimización de la Ley 25.326 (art. 4) no deberían llevar datos de contacto
 *       completos de pacientes. Con la parte enmascarada alcanza para diagnosticar un problema
 *       sin exponer a la persona.</li>
 * </ul>
 */
public final class DatosPersonales {

    private DatosPersonales() {
    }

    /** {@code null} queda {@code null}; cualquier otro valor se recorta y pasa a minúsculas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** "juan.perez@gmail.com" → "ju***@gmail.com". */
    public static String enmascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(sin email)";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        String usuario = email.substring(0, arroba);
        String visible = usuario.length() <= 2 ? usuario.substring(0, 1) : usuario.substring(0, 2);
        return visible + "***" + email.substring(arroba);
    }

    /** "+5491155554444" → "***4444" (solo los últimos 4 dígitos). */
    public static String enmascararTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return "(sin teléfono)";
        }
        String digitos = telefono.replaceAll("\\D", "");
        return digitos.length() <= 4 ? "***" : "***" + digitos.substring(digitos.length() - 4);
    }
}
