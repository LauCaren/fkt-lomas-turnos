package ar.edu.uade.recordatorios.service;

import ar.edu.uade.recordatorios.repository.OrigenRepository;
import ar.edu.uade.recordatorios.sync.EstadoSincronizacion;
import ar.edu.uade.recordatorios.sync.PacienteOrigen;
import ar.edu.uade.recordatorios.sync.TurnoOrigen;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copia pacientes y turnos desde el sistema de turnos real (solo lectura,
 * vía OrigenRepository) hacia la base propia de la app. Nunca escribe en el
 * origen. El canal_preferido de cada paciente vive solo en la base local y
 * esta clase jamás lo toca al sincronizar — solo agrega pacientes/turnos
 * nuevos, actualiza los datos que vienen del origen, y da de baja lógica
 * (activo = FALSE) lo que dejó de aparecer ahí.
 */
public class SincronizadorService {

    private static final Logger log = Logger.getLogger(SincronizadorService.class.getName());
    private static final int DIAS_HACIA_ATRAS = 1;

    private final OrigenRepository origenRepository;
    private final DataSource localDataSource;

    public SincronizadorService(OrigenRepository origenRepository, DataSource localDataSource) {
        this.origenRepository = origenRepository;
        this.localDataSource = localDataSource;
    }

    public EstadoSincronizacion sincronizar() {
        LocalDateTime desde = LocalDateTime.now().minusDays(DIAS_HACIA_ATRAS);
        EstadoSincronizacion resultado;

        try {
            List<PacienteOrigen> pacientes = origenRepository.listarPacientes();
            List<TurnoOrigen> turnos = origenRepository.listarTurnosDesde(desde);

            try (Connection conn = localDataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    upsertPacientes(conn, pacientes);
                    upsertTurnos(conn, turnos);
                    desactivarAusentes(conn, "pacientes", idsDe(pacientes, PacienteOrigen::id));
                    desactivarTurnosAusentes(conn, idsDe(turnos, TurnoOrigen::id), desde);
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }

            resultado = new EstadoSincronizacion(LocalDateTime.now(), pacientes.size(), turnos.size(), null);
            log.info(() -> "Sincronización OK: " + pacientes.size() + " pacientes, " + turnos.size() + " turnos");

            RespaldoService.respaldarSiCorresponde(localDataSource);

        } catch (Exception e) {
            log.log(Level.WARNING, "Falló la sincronización con el sistema de turnos", e);
            resultado = new EstadoSincronizacion(LocalDateTime.now(), 0, 0, causaRaiz(e));
        }

        guardarEstado(resultado);
        return resultado;
    }

    public EstadoSincronizacion obtenerUltimoEstado() {
        String sql = "SELECT ultima_ejecucion, pacientes_sincronizados, turnos_sincronizados, detalle_error "
            + "FROM sincronizacion WHERE id = 1";

        try (Connection conn = localDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                return EstadoSincronizacion.nuncaEjecutada();
            }
            Timestamp ultima = rs.getTimestamp("ultima_ejecucion");
            return new EstadoSincronizacion(
                ultima != null ? ultima.toLocalDateTime() : null,
                rs.getInt("pacientes_sincronizados"),
                rs.getInt("turnos_sincronizados"),
                rs.getString("detalle_error")
            );
        } catch (SQLException e) {
            throw new RuntimeException("Error leyendo el estado de sincronización", e);
        }
    }

    private void upsertPacientes(Connection conn, List<PacienteOrigen> pacientes) throws SQLException {
        String sql = "MERGE INTO pacientes (id, nombre, telefono, email, activo) KEY(id) VALUES (?, ?, ?, ?, TRUE)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PacienteOrigen p : pacientes) {
                stmt.setLong(1, p.id());
                stmt.setString(2, p.nombre());
                stmt.setString(3, p.telefono());
                stmt.setString(4, p.email());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void upsertTurnos(Connection conn, List<TurnoOrigen> turnos) throws SQLException {
        String sql = "MERGE INTO turnos (id, paciente_id, fecha_hora, profesional, especialidad, consultorio, activo) "
            + "KEY(id) VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (TurnoOrigen t : turnos) {
                stmt.setLong(1, t.id());
                stmt.setLong(2, t.pacienteId());
                stmt.setTimestamp(3, Timestamp.valueOf(t.fechaHora()));
                stmt.setString(4, t.profesional());
                stmt.setString(5, t.especialidad());
                stmt.setString(6, t.consultorio());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /** Fila activa en la base local que ya no vino en esta sincronización: baja lógica, no se borra. */
    private void desactivarAusentes(Connection conn, String tabla, Set<Long> idsVigentes) throws SQLException {
        List<Long> aDesactivar = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM " + tabla + " WHERE activo = TRUE")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!idsVigentes.contains(id)) {
                    aDesactivar.add(id);
                }
            }
        }
        ejecutarBaja(conn, tabla, aDesactivar);
    }

    /** Igual que desactivarAusentes, pero solo mira turnos dentro de la ventana que se sincronizó (no todo el historial). */
    private void desactivarTurnosAusentes(Connection conn, Set<Long> idsVigentes, LocalDateTime desde) throws SQLException {
        List<Long> aDesactivar = new ArrayList<>();
        String sql = "SELECT id FROM turnos WHERE activo = TRUE AND fecha_hora >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(desde));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    if (!idsVigentes.contains(id)) {
                        aDesactivar.add(id);
                    }
                }
            }
        }
        ejecutarBaja(conn, "turnos", aDesactivar);
    }

    private void ejecutarBaja(Connection conn, String tabla, List<Long> ids) throws SQLException {
        if (ids.isEmpty()) {
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE " + tabla + " SET activo = FALSE WHERE id = ?")) {
            for (Long id : ids) {
                stmt.setLong(1, id);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private <T> Set<Long> idsDe(List<T> lista, Function<T, Long> extractor) {
        Set<Long> ids = new HashSet<>();
        for (T item : lista) {
            ids.add(extractor.apply(item));
        }
        return ids;
    }

    private void guardarEstado(EstadoSincronizacion estado) {
        String sql = "MERGE INTO sincronizacion (id, ultima_ejecucion, pacientes_sincronizados, turnos_sincronizados, detalle_error) "
            + "KEY(id) VALUES (1, ?, ?, ?, ?)";
        try (Connection conn = localDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, estado.ultimaEjecucion() != null ? Timestamp.valueOf(estado.ultimaEjecucion()) : null);
            stmt.setInt(2, estado.pacientesSincronizados());
            stmt.setInt(3, estado.turnosSincronizados());
            stmt.setString(4, estado.detalleError());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.log(Level.WARNING, "No se pudo guardar el estado de sincronización", e);
        }
    }

    private String causaRaiz(Throwable t) {
        Throwable actual = t;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual.getMessage() != null ? actual.getMessage() : actual.toString();
    }
}
