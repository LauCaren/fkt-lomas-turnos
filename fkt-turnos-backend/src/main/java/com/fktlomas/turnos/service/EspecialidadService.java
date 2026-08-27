package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.dto.EspecialidadDto;
import com.fktlomas.turnos.dto.admin.CrearEspecialidadRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public List<EspecialidadDto> listar() {
        return especialidadRepository.findAll().stream().map(EspecialidadDto::de).toList();
    }

    public EspecialidadDto crear(CrearEspecialidadRequest request) {
        boolean existe = especialidadRepository.findAll().stream()
                .anyMatch(e -> e.getNombre().equalsIgnoreCase(request.nombre()));
        if (existe) {
            throw new ConflictoDeDatosException("Ya existe una especialidad con ese nombre");
        }
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(request.nombre());
        return EspecialidadDto.de(especialidadRepository.save(especialidad));
    }

    public void eliminar(Long id) {
        if (!especialidadRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Especialidad no encontrada");
        }
        especialidadRepository.deleteById(id);
    }

    public Especialidad obtenerEntidad(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
    }
}
