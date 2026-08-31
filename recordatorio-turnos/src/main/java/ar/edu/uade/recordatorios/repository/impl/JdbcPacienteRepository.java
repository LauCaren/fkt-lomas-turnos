package ar.edu.uade.recordatorios.repository.impl;

import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.repository.PacienteRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Lee y escribe en la base LOCAL (el espejo) — nunca en el sistema de turnos real. */
public class JdbcPacienteRepository implements PacienteRepository {

    private final DataSource dataSource;

    public JdbcPacienteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Paciente> listarTodos() {
        String sql = "SELECT id, nombre, telefono, email, canal_preferido FROM pacientes WHERE activo = TRUE ORDER BY nombre";
        List<Paciente> pacientes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                pacientes.add(new Paciente(
                    rs.getLong("id"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("email"),
                    CanalNotificacion.valueOf(rs.getString("canal_preferido"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando pacientes", e);
        }

        return pacientes;
    }

    @Override
    public void actualizarCanalPreferido(Long pacienteId, CanalNotificacion canal) {
        String sql = "UPDATE pacientes SET canal_preferido = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, canal.name());
            stmt.setLong(2, pacienteId);
            int filas = stmt.executeUpdate();
            if (filas == 0) {
                throw new IllegalArgumentException("No existe el paciente " + pacienteId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando el canal preferido del paciente " + pacienteId, e);
        }
    }
}
