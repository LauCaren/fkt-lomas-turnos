package com.fktlomas.turnos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fktlomas.turnos.domain.RegistroAuditoria;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.auditoria.RegistroAuditoriaDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.util.List;

/**
 * Auditoría genérica: quién hizo qué, cuándo, con un snapshot antes/después en JSON. Hoy solo
 * la usa TurnoService (reservar/cancelar/reservar-para-paciente) — es la base del undo acotado
 * a turnos del punto 11; extenderla a otras entidades (médicos, caja, historia clínica) es un
 * paso natural después, no algo que haya que rediseñar.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int LIMITE_LISTADO = 200;

    private final RegistroAuditoriaRepository registroRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void registrar(Usuario actor, String tipoEntidad, Long entidadId, String operacion, Object antes, Object despues) {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setUsuario(actor);
        registro.setTipoEntidad(tipoEntidad);
        registro.setEntidadId(entidadId);
        registro.setOperacion(operacion);
        registro.setSnapshotAntes(serializar(antes));
        registro.setSnapshotDespues(serializar(despues));
        registroRepository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoriaDto> buscar(String tipoEntidad, Long entidadId) {
        return registroRepository.buscar(tipoEntidad, entidadId, PageRequest.of(0, LIMITE_LISTADO))
                .stream().map(RegistroAuditoriaDto::de).toList();
    }

    /**
     * Trae el snapshot "antes" de la última operación no deshecha sobre esta entidad, y la
     * marca como deshecha. El caller es responsable de aplicar ese snapshot al estado actual.
     */
    @Transactional
    public <T> T snapshotParaDeshacer(String tipoEntidad, Long entidadId, Class<T> tipo) {
        RegistroAuditoria ultimo = registroRepository
                .findTopByTipoEntidadAndEntidadIdAndDeshechoFalseOrderByCreatedAtDesc(tipoEntidad, entidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay operaciones para deshacer"));
        if (ultimo.getSnapshotAntes() == null) {
            throw new ConflictoDeDatosException("La última operación sobre este registro no se puede deshacer");
        }
        ultimo.setDeshecho(true);
        registroRepository.save(ultimo);
        return deserializar(ultimo.getSnapshotAntes(), tipo);
    }

    private String serializar(Object o) {
        if (o == null) return null;
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T deserializar(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
