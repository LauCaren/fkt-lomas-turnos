package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.dto.MedicoDto;
import com.fktlomas.turnos.dto.admin.ActualizarMedicoRequest;
import com.fktlomas.turnos.dto.admin.CrearMedicoRequest;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final EspecialidadService especialidadService;

    @Transactional(readOnly = true)
    public List<MedicoDto> listar(Long especialidadId) {
        List<Medico> medicos = especialidadId != null
                ? medicoRepository.findByEspecialidadIdAndActivoTrue(especialidadId)
                : medicoRepository.findByActivoTrue();
        return medicos.stream().map(MedicoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicoDto> listarTodosAdmin() {
        return medicoRepository.findAll().stream().map(MedicoDto::de).toList();
    }

    public Medico obtenerEntidad(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
    }

    public MedicoDto crear(CrearMedicoRequest request) {
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        Medico medico = new Medico();
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        return MedicoDto.de(medicoRepository.save(medico));
    }

    public MedicoDto actualizar(Long id, ActualizarMedicoRequest request) {
        Medico medico = obtenerEntidad(id);
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setActivo(request.activo());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        return MedicoDto.de(medicoRepository.save(medico));
    }
}
