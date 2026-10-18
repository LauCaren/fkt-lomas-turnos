package com.fktlomas.turnos.domain;

/** Ver {@link SolicitudArrepentimiento}. */
public enum EstadoArrepentimiento {
    /** Recibida, todavía nadie del centro la gestionó (cancelar el turno, devolver el pago). */
    PENDIENTE,
    /** El centro ya la gestionó; la nota de resolución dice cómo (ej. "reintegro por MP el 12/10"). */
    RESUELTA
}
