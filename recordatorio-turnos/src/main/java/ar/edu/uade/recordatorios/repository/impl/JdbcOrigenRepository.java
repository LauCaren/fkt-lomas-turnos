package ar.edu.uade.recordatorios.repository.impl;

import ar.edu.uade.recordatorios.repository.OrigenRepository;
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
import java.util.List;

/**
 * Contra el esquema real de fkt-turnos-backend (PostgreSQL, ver
 * db/migration/V1__init.sql en ese repo). Ambos métodos son estrictamente
 * SELECT: esta clase nunca debe escribir en la base de origen.
 *
 * Notas del esquema real:
 * - Pacientes y personal comparten la tabla `usuarios`, distinguidos por
 *   `role` ('PACIENTE' / 'ADMIN') — solo se sincronizan los PACIENTE.
 * - `turnos` también contiene los horarios libres (`estado = 'DISPONIBLE'`);
 *   solo interesan los reservados (`estado = 'RESERVADO'`, con paciente_id).
 * - No existe el concepto de "consultorio" (FKT Lomas tiene una sola sede),
 *   así que TurnoOrigen.consultorio queda vacío.
 */
public class JdbcOrigenRepository implements OrigenRepository {

    private final DataSource dataSource;

    public JdbcOrigenRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<PacienteOrigen> listarPacientes() {
        String sql = "SELECT id, nombre_completo, telefono, email FROM usuarios WHERE role = 'PACIENTE'";
        List<PacienteOrigen> pacientes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                pacientes.add(new PacienteOrigen(
                    rs.getLong("id"),
                    rs.getString("nombre_completo"),
                    rs.getString("telefono"),
                    rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error leyendo pacientes del sistema de turnos", e);
        }

        return pacientes;
    }

    @Override
    public List<TurnoOrigen> listarTurnosDesde(LocalDateTime desde) {
        String sql = """
            SELECT t.id, (t.fecha + t.hora_inicio) AS fecha_hora,
                   m.nombre_completo AS profesional, e.nombre AS especialidad,
                   u.id AS paciente_id
            FROM turnos t
            JOIN medicos m ON m.id = t.medico_id
            JOIN especialidades e ON e.id = m.especialidad_id
            JOIN usuarios u ON u.id = t.paciente_id
            WHERE t.estado = 'RESERVADO'
              AND (t.fecha + t.hora_inicio) >= ?
            """;
        List<TurnoOrigen> turnos = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(desde));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    turnos.add(new TurnoOrigen(
                        rs.getLong("id"),
                        rs.getLong("paciente_id"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        rs.getString("profesional"),
                        rs.getString("especialidad"),
                        ""
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error leyendo turnos del sistema de turnos", e);
        }

        return turnos;
    }
}
