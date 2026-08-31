package ar.edu.uade.recordatorios.gui;

import java.util.Locale;

/**
 * Traduce los mensajes de error más comunes (JDBC, SMTP, Twilio) a algo que
 * alguien sin conocimientos técnicos pueda entender y accionar. No es
 * exhaustivo a propósito: cubre los casos que más probablemente se dan al
 * configurar la app por primera vez; el resto de los errores se muestran
 * tal cual vienen.
 */
final class MensajesAmigables {

    private MensajesAmigables() {
    }

    static String traducir(String mensajeOriginal) {
        if (mensajeOriginal == null || mensajeOriginal.isBlank()) {
            return "Ocurrió un error inesperado.";
        }
        String m = mensajeOriginal.toLowerCase(Locale.ROOT);

        if (m.contains("communications link failure") || m.contains("connection refused")) {
            return "No se pudo conectar a la base de datos. Verificá la URL, que el servidor esté encendido, y tu conexión de red.";
        }
        if (m.contains("access denied for user")) {
            return "Usuario o contraseña incorrectos para la base de datos.";
        }
        if (m.contains("unknown database")) {
            return "La base de datos indicada no existe. Revisá el nombre en la URL de conexión.";
        }
        if (m.contains("no suitable driver")) {
            return "No se encontró el driver de la base de datos. Revisá que la URL de conexión sea correcta.";
        }
        if (m.contains("535") || m.contains("username and password not accepted")) {
            return "El servidor de email rechazó el usuario o la contraseña. Si usás Gmail, acordate que hace falta una "
                + "\"contraseña de aplicación\" (no la contraseña normal de la cuenta).";
        }
        if (m.contains("could not connect to smtp host") || m.contains("unknown smtp host")) {
            return "No se pudo conectar al servidor de email. Revisá el servidor SMTP y tu conexión a internet.";
        }
        if (m.contains("authenticate") && (m.contains("twilio") || m.contains("20003"))) {
            return "No se pudo autenticar con Twilio. Verificá el Account SID y el Auth Token.";
        }
        if (m.contains("not a valid phone number") || m.contains("not a valid number")) {
            return "El número de teléfono no tiene un formato válido. Probá con el formato internacional (ej: +5491122334455).";
        }
        if (m.contains("read timed out") || m.contains("connect timed out") || m.contains("timeout")) {
            return "Se agotó el tiempo de espera. Revisá la conexión a internet o que los datos del servidor sean correctos.";
        }

        return mensajeOriginal;
    }
}
