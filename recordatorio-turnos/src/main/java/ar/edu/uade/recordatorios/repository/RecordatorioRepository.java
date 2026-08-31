package ar.edu.uade.recordatorios.repository;

import ar.edu.uade.recordatorios.model.Recordatorio;

import java.util.List;
import java.util.Optional;

public interface RecordatorioRepository {

    Recordatorio guardar(Recordatorio recordatorio);

    void actualizarEstado(Recordatorio recordatorio);

    boolean existeRecordatorioEnviado(Long turnoId);

    Optional<Recordatorio> buscarPorId(Long id);

    /** El recordatorio (de cualquier estado) que ya exista para ese turno, si hay alguno. */
    Optional<Recordatorio> buscarPorTurno(Long turnoId);

    /** Los más recientes primero, para mostrar en el panel de monitoreo. */
    List<Recordatorio> listarUltimos(int limite);
}
