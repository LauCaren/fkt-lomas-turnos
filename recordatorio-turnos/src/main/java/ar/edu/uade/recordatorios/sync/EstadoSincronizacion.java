package ar.edu.uade.recordatorios.sync;

import java.time.LocalDateTime;

/** Para mostrar en la GUI cuándo sincronizó por última vez y con qué resultado. */
public record EstadoSincronizacion(
    LocalDateTime ultimaEjecucion,
    int pacientesSincronizados,
    int turnosSincronizados,
    String detalleError
) {
    public static EstadoSincronizacion nuncaEjecutada() {
        return new EstadoSincronizacion(null, 0, 0, null);
    }

    public boolean fueExitosa() {
        return detalleError == null;
    }
}
