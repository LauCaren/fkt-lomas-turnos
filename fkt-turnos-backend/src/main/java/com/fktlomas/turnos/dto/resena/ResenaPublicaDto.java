package com.fktlomas.turnos.dto.resena;

import com.fktlomas.turnos.domain.Resena;

import java.time.LocalDate;
import java.time.ZoneId;

/** Solo lo necesario para mostrar en el sitio de marketing o la ficha del médico — nunca el nombre completo del paciente ni a qué turno corresponde. */
public record ResenaPublicaDto(
        Long id,
        int puntaje,
        String comentario,
        String pacienteNombreCorto,
        LocalDate fecha,
        Long medicoId,
        String medicoNombre,
        String especialidadNombre
) {
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    public static ResenaPublicaDto de(Resena r) {
        return new ResenaPublicaDto(
                r.getId(),
                r.getPuntaje(),
                r.getComentario(),
                nombreCorto(r.getPaciente().getNombreCompleto()),
                r.getCreatedAt().atZone(ZONA).toLocalDate(),
                r.getMedico().getId(),
                r.getMedico().getNombreCompleto(),
                r.getMedico().getEspecialidad().getNombre()
        );
    }

    /** "Martina Fernández" -> "Martina F." — nunca se expone el apellido completo de un paciente en una reseña pública. */
    private static String nombreCorto(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length < 2) {
            return partes[0];
        }
        return partes[0] + " " + partes[1].charAt(0) + ".";
    }
}
