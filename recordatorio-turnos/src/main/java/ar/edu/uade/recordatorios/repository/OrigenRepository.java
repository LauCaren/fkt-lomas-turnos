package ar.edu.uade.recordatorios.repository;

import ar.edu.uade.recordatorios.sync.PacienteOrigen;
import ar.edu.uade.recordatorios.sync.TurnoOrigen;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso de SOLO LECTURA a la base del sistema de turnos real. Ninguna
 * implementación de esta interfaz debe ejecutar INSERT/UPDATE/DELETE — el
 * único lugar que escribe algo relacionado a pacientes/turnos es la base
 * propia de la app (ver PacienteRepository/TurnoRepository, que apuntan a
 * la base local, no a esta).
 */
public interface OrigenRepository {

    List<PacienteOrigen> listarPacientes();

    /** Turnos desde la fecha indicada en adelante, para no traer todo el historial en cada sincronización. */
    List<TurnoOrigen> listarTurnosDesde(LocalDateTime desde);
}
