package ar.edu.uade.recordatorios.notification;

import ar.edu.uade.recordatorios.model.Turno;

import java.time.format.DateTimeFormatter;

/**
 * Arma el texto del recordatorio automático a partir de una plantilla con
 * placeholders, editable desde la GUI (pestaña "Mensaje"). Placeholders
 * disponibles: {paciente}, {profesional}, {especialidad}, {fecha}, {hora}.
 * {consultorio} también funciona pero queda vacío contra el sistema real de
 * FKT Lomas (una sola sede, sin ese concepto en el modelo de datos).
 */
public class PlantillaMensaje {

    public static final String PLANTILLA_POR_DEFECTO =
        "Hola {paciente}, te recordamos tu turno con {profesional} ({especialidad}) "
        + "el {fecha} a las {hora} hs en FKT Lomas, Larrea 496, Lomas de Zamora. "
        + "Ante cualquier inconveniente para asistir, por favor avisanos. ¡Te esperamos!";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private PlantillaMensaje() {
    }

    public static String construir(String plantilla, Turno turno) {
        String base = (plantilla == null || plantilla.isBlank()) ? PLANTILLA_POR_DEFECTO : plantilla;
        return base
            .replace("{paciente}", turno.getPaciente().getNombre())
            .replace("{profesional}", valorSeguro(turno.getProfesional()))
            .replace("{especialidad}", valorSeguro(turno.getEspecialidad()))
            .replace("{consultorio}", valorSeguro(turno.getConsultorio()))
            .replace("{fecha}", turno.getFechaHora().format(FORMATO_FECHA))
            .replace("{hora}", turno.getFechaHora().format(FORMATO_HORA));
    }

    private static String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
