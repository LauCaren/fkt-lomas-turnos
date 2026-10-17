package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.RegistroAuditoria;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.auditoria.RegistroAuditoriaDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    // Mismo criterio que CajaService: un rango concreto en vez de nulls "abiertos" (ver
    // RegistroAuditoriaRepository.buscarGeneral).
    private static final LocalDate DESDE_MINIMO = LocalDate.of(2000, 1, 1);
    private static final LocalDate HASTA_MAXIMO = LocalDate.of(2099, 12, 31);

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

    /** Panel de auditoría general del admin — "hasta" es inclusive del día completo. */
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDto> buscarGeneral(String tipoEntidad, Long usuarioId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        LocalDate desdeEfectivo = desde != null ? desde : DESDE_MINIMO;
        LocalDate hastaEfectivo = hasta != null ? hasta : HASTA_MAXIMO;
        Instant desdeInstant = desdeEfectivo.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant hastaInstant = hastaEfectivo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return registroRepository.buscarGeneral(tipoEntidad, usuarioId, desdeInstant, hastaInstant, pageable)
                .map(RegistroAuditoriaDto::de);
    }

    /**
     * Trae el snapshot "antes" de la última operación no deshecha sobre esta entidad, y la
     * marca como deshecha. El caller es responsable de aplicar ese snapshot al estado actual.
     */
    @Transactional
    public <T> T snapshotParaDeshacer(String tipoEntidad, Long entidadId, Class<T> tipo) {
        RegistroAuditoria ultimo = registroRepository
                .findTopByTipoEntidadAndEntidadIdAndDeshechoFalseOrderByCreatedAtDescIdDesc(tipoEntidad, entidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay operaciones para deshacer"));
        if (ultimo.getSnapshotAntes() == null) {
            throw new ConflictoDeDatosException("La última operación sobre este registro no se puede deshacer");
        }
        ultimo.setDeshecho(true);
        registroRepository.save(ultimo);
        return deserializar(ultimo.getSnapshotAntes(), tipo);
    }

    // Jackson 3: JacksonException ya es unchecked, no hace falta envolverla.
    private String serializar(Object o) {
        if (o == null) return null;
        return objectMapper.writeValueAsString(o);
    }

    private <T> T deserializar(String json, Class<T> tipo) {
        return objectMapper.readValue(json, tipo);
    }
}
