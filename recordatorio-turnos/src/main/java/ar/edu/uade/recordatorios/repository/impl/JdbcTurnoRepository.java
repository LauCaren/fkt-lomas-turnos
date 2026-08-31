package ar.edu.uade.recordatorios.repository.impl;

import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Turno;
import ar.edu.uade.recordatorios.repository.TurnoRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee de la base LOCAL (el espejo, no el sistema de turnos real — para eso
 * está OrigenRepository). Coincide con sql/schema-local.sql, que esta app
 * gestiona por su cuenta, así que en principio no hace falta tocar este SQL.
 */
public class JdbcTurnoRepository implements TurnoRepository {

    private final DataSource dataSource;

    public JdbcTurnoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
     * El LEFT JOIN excluye turnos cuyo recordatorio ya llegó a un estado
     * definitivo (ENVIADO o FALLIDO tras agotar los reintentos). Uno con
     * estado PENDIENTE sigue apareciendo a propósito: así el scheduler lo
     * puede reintentar. Es responsabilidad de RecordatorioService reusar
     * esa fila existente en vez de crear una nueva en cada chequeo — si
     * solo se filtrara por ENVIADO, un turno con envíos fallidos generaría
     * un recordatorio duplicado (y un posible mensaje repetido al paciente)
     * en cada corrida del scheduler.
     */
    private static final String SQL_TURNOS_PROXIMOS = """
        SELECT t.id, t.fecha_hora, t.profesional, t.especialidad, t.consultorio,
               p.id AS paciente_id, p.nombre, p.telefono, p.email, p.canal_preferido
        FROM turnos t
        JOIN pacientes p ON p.id = t.paciente_id
        LEFT JOIN recordatorios r ON r.turno_id = t.id AND r.estado IN ('ENVIADO', 'FALLIDO')
        WHERE t.fecha_hora BETWEEN ? AND ?
          AND t.activo = TRUE AND p.activo = TRUE
          AND r.id IS NULL
        """;

    @Override
    public List<Turno> buscarTurnosProximos(LocalDateTime desde, LocalDateTime hasta) {
        List<Turno> turnos = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_TURNOS_PROXIMOS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(desde));
            stmt.setTimestamp(2, Timestamp.valueOf(hasta));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    turnos.add(mapearTurno(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando turnos próximos", e);
        }

        return turnos;
    }

    private Turno mapearTurno(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente(
            rs.getLong("paciente_id"),
            rs.getString("nombre"),
            rs.getString("telefono"),
            rs.getString("email"),
            CanalNotificacion.valueOf(rs.getString("canal_preferido"))
        );

        return new Turno(
            rs.getLong("id"),
            paciente,
            rs.getTimestamp("fecha_hora").toLocalDateTime(),
            rs.getString("profesional"),
            rs.getString("especialidad"),
            rs.getString("consultorio")
        );
    }
}
