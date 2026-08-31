package ar.edu.uade.recordatorios.repository;

import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.Paciente;

import java.util.List;

public interface PacienteRepository {

    List<Paciente> listarTodos();

    void actualizarCanalPreferido(Long pacienteId, CanalNotificacion canal);
}
