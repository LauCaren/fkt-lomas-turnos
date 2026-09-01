package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.admin.FichaPacienteDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Búsqueda global de pacientes para el panel de admin: encontrar a alguien y ver de una sus
 * turnos, su plan de kinesiología y los cobros asociados — sin tener que ir pestaña por
 * pestaña. Deliberadamente no incluye historia clínica (privada del médico).
 */
@Service
@RequiredArgsConstructor
public class AdminPacienteService {

    private final UsuarioRepository usuarioRepository;
    private final TurnoService turnoService;
    private final KinesiologiaService kinesiologiaService;
    private final CajaService cajaService;

    @Transactional(readOnly = true)
    public List<PacienteBusquedaDto> buscar(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return List.of();
        }
        return usuarioRepository.buscarPacientes(busqueda.trim()).stream().map(PacienteBusquedaDto::de).toList();
    }

    @Transactional(readOnly = true)
    public FichaPacienteDto ficha(Long pacienteId) {
        Usuario paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
        if (paciente.getRole() != Role.PACIENTE) {
            throw new RecursoNoEncontradoException("Paciente no encontrado");
        }
        return new FichaPacienteDto(
                PacienteBusquedaDto.de(paciente),
                turnoService.misTurnos(pacienteId),
                kinesiologiaService.misPlanes(pacienteId),
                cajaService.ingresosDePaciente(pacienteId)
        );
    }
}
