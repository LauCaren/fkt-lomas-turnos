package com.fktlomas.turnos.dto;

import java.time.LocalTime;

/**
 * Resultado de escanear el QR de la sala de espera. `resultado` le dice al frontend qué
 * pantalla mostrar; `mensaje` ya viene redactado para mostrarse tal cual al paciente.
 */
public record CheckInDto(
        Resultado resultado,
        String mensaje,
        String medicoNombre,
        LocalTime horaTurno
) {
    public enum Resultado {
        /** Se marcó la asistencia automáticamente, ya puede esperar a que lo llamen. */
        LLEGO,
        /** Tiene copago de IOMA — no se autoregistra, tiene que pasar por el mostrador a abonar. */
        REQUIERE_MOSTRADOR,
        /** No tiene ningún turno reservado hoy. */
        SIN_TURNO_HOY,
        /** Ya se había registrado (llegó/ausente/atendido) antes de este escaneo. */
        YA_REGISTRADO
    }
}
