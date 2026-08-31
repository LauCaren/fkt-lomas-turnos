package ar.edu.uade.recordatorios.repository;

import ar.edu.uade.recordatorios.model.Turno;

import java.time.LocalDateTime;
import java.util.List;

public interface TurnoRepository {

    /**
     * Devuelve los turnos cuya fecha/hora cae dentro de la ventana indicada
     * y que todavía no tienen un recordatorio con estado ENVIADO.
     */
    List<Turno> buscarTurnosProximos(LocalDateTime desde, LocalDateTime hasta);
}
