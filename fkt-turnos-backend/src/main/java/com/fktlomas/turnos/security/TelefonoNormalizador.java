package com.fktlomas.turnos.security;

/**
 * Normaliza el "From" que manda Twilio (ej "whatsapp:+54 9 11 4970-3857") a una clave estable
 * para conversaciones_whatsapp.telefono. Deliberadamente NO se usa para buscar un Usuario por
 * teléfono — esa columna no tiene normalización ni unicidad, así que un match por teléfono no
 * prueba identidad (ver ConversacionWhatsApp). Solo sirve para no tratar como conversaciones
 * distintas al mismo número escrito con o sin espacios/guiones.
 */
public final class TelefonoNormalizador {

    private TelefonoNormalizador() {
    }

    public static String normalizar(String telefono) {
        if (telefono == null) {
            return null;
        }
        String sinPrefijo = telefono.startsWith("whatsapp:") ? telefono.substring("whatsapp:".length()) : telefono;
        String soloDigitosYSigno = sinPrefijo.replaceAll("[^+\\d]", "");
        if (soloDigitosYSigno.isEmpty()) {
            return soloDigitosYSigno;
        }
        return soloDigitosYSigno.startsWith("+") ? soloDigitosYSigno : "+" + soloDigitosYSigno;
    }
}
