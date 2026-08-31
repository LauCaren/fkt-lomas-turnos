package ar.edu.uade.recordatorios.repository.impl;

import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.EstadoRecordatorio;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Recordatorio;
import ar.edu.uade.recordatorios.model.Turno;
import ar.edu.uade.recordatorios.repository.RecordatorioRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRecordatorioRepository implements RecordatorioRepository {

    private static final String SQL_BASE = """
        SELECT r.id AS recordatorio_id, r.canal, r.estado, r.intentos, r.fecha_envio, r.detalle_error,
               t.id AS turno_id, t.fecha_hora, t.profesional, t.especialidad, t.consultorio,
               p.id AS paciente_id, p.nombre, p.telefono, p.email, p.canal_preferido
        FROM recordatorios r
        JOIN turnos t ON t.id = r.turno_id
        JOIN pacientes p ON p.id = t.paciente_id
        """;

    private final DataSource dataSource;

    public JdbcRecordatorioRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Recordatorio guardar(Recordatorio recordatorio) {
        String sql = "INSERT INTO recordatorios (turno_id, canal, estado, intentos) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, recordatorio.getTurno().getId());
            stmt.setString(2, recordatorio.getCanal().name());
            stmt.setString(3, recordatorio.getEstado().name());
            stmt.setInt(4, recordatorio.getIntentos());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    recordatorio.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando recordatorio", e);
        }

        return recordatorio;
    }

    @Override
    public void actualizarEstado(Recordatorio recordatorio) {
        String sql = "UPDATE recordatorios SET estado = ?, fecha_envio = ?, intentos = ?, detalle_error = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, recordatorio.getEstado().name());
            stmt.setTimestamp(2, recordatorio.getFechaEnvio() != null
                ? Timestamp.valueOf(recordatorio.getFechaEnvio()) : null);
            stmt.setInt(3, recordatorio.getIntentos());
            stmt.setString(4, recordatorio.getDetalleError());
            stmt.setLong(5, recordatorio.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando recordatorio", e);
        }
    }

    @Override
    public boolean existeRecordatorioEnviado(Long turnoId) {
        String sql = "SELECT 1 FROM recordatorios WHERE turno_id = ? AND estado = 'ENVIADO'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, turnoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error verificando recordatorio", e);
        }
    }

    @Override
    public Optional<Recordatorio> buscarPorId(Long id) {
        String sql = SQL_BASE + " WHERE r.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearRecordatorio(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando recordatorio " + id, e);
        }
    }

    @Override
    public Optional<Recordatorio> buscarPorTurno(Long turnoId) {
        String sql = SQL_BASE + " WHERE r.turno_id = ? ORDER BY r.id DESC LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, turnoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapearRecordatorio(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando recordatorio del turno " + turnoId, e);
        }
    }

    @Override
    public List<Recordatorio> listarUltimos(int limite) {
        String sql = SQL_BASE + " ORDER BY r.id DESC LIMIT ?";
        List<Recordatorio> resultado = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearRecordatorio(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando recordatorios", e);
        }

        return resultado;
    }

    private Recordatorio mapearRecordatorio(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente(
            rs.getLong("paciente_id"),
            rs.getString("nombre"),
            rs.getString("telefono"),
            rs.getString("email"),
            CanalNotificacion.valueOf(rs.getString("canal_preferido"))
        );

        Turno turno = new Turno(
            rs.getLong("turno_id"),
            paciente,
            rs.getTimestamp("fecha_hora").toLocalDateTime(),
            rs.getString("profesional"),
            rs.getString("especialidad"),
            rs.getString("consultorio")
        );

        Recordatorio recordatorio = new Recordatorio(turno, CanalNotificacion.valueOf(rs.getString("canal")));
        recordatorio.setId(rs.getLong("recordatorio_id"));
        recordatorio.setEstado(EstadoRecordatorio.valueOf(rs.getString("estado")));
        recordatorio.setIntentos(rs.getInt("intentos"));
        Timestamp fechaEnvio = rs.getTimestamp("fecha_envio");
        if (fechaEnvio != null) {
            recordatorio.setFechaEnvio(fechaEnvio.toLocalDateTime());
        }
        recordatorio.setDetalleError(rs.getString("detalle_error"));
        return recordatorio;
    }
}
