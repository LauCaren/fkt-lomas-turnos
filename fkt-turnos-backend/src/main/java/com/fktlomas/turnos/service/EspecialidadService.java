package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Usuario;
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

    private static final String AUDITORIA_TIPO = "ESPECIALIDAD";

    private final EspecialidadRepository especialidadRepository;
    private final AuditoriaService auditoriaService;

    public List<EspecialidadDto> listar() {
        return especialidadRepository.findAll().stream().map(EspecialidadDto::de).toList();
    }

    public EspecialidadDto crear(CrearEspecialidadRequest request, Usuario actor) {
        boolean existe = especialidadRepository.findAll().stream()
                .anyMatch(e -> e.getNombre().equalsIgnoreCase(request.nombre()));
        if (existe) {
            throw new ConflictoDeDatosException("Ya existe una especialidad con ese nombre");
        }
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(request.nombre());
        EspecialidadDto creada = EspecialidadDto.de(especialidadRepository.save(especialidad));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, creada.id(), "CREAR", null, creada);
        return creada;
    }

    public void eliminar(Long id, Usuario actor) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
        EspecialidadDto antes = EspecialidadDto.de(especialidad);
        especialidadRepository.deleteById(id);
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "ELIMINAR", antes, null);
    }

    public Especialidad obtenerEntidad(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
    }
}
