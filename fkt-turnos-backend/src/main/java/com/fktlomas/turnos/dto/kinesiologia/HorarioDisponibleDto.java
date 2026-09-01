package com.fktlomas.turnos.dto.kinesiologia;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Un horario semanal real que un kinesiólogo puede ofrecer para un plan — calculado a partir
 * de su ReglaDisponibilidad y su duración de turno, para que el paciente elija de una lista
 * de opciones válidas en vez de escribir un día/hora a ciegas que el médico capaz no atiende.
 */
public record HorarioDisponibleDto(Long medicoId, String medicoNombre, DayOfWeek diaSemana, LocalTime horaInicio) {
}
