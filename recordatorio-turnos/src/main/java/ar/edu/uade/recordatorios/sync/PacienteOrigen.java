package ar.edu.uade.recordatorios.sync;

/**
 * Paciente tal como lo devuelve el sistema de turnos real. A propósito no
 * tiene canal de notificación: ese dato pasa a vivir solo en la base local
 * de esta app (ver PacienteRepository), y la sincronización no lo toca.
 */
public record PacienteOrigen(Long id, String nombre, String telefono, String email) {
}
