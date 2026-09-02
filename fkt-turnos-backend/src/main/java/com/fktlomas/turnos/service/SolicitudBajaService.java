package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoSolicitudBaja;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.SolicitudBajaCuenta;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.SolicitarBajaRequest;
import com.fktlomas.turnos.dto.admin.SolicitudBajaDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.SolicitudBajaCuentaRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Derecho de supresión (Ley 25.326) implementado como un pedido que un admin/secretaria
 * procesa, no un borrado directo de autoservicio — turnos, evoluciones e ingresos ya generados
 * tienen que conservarse (historia clínica: Ley 26.529; registros contables), así que aprobar
 * un pedido anonimiza el PII de la cuenta en vez de eliminarla. Solo para pacientes: una cuenta
 * de staff se da de baja por otra vía (AdminUsuarioService.desactivar), porque ahí el nombre
 * real tiene que seguir apareciendo en turnos/caja ya generados a nombre de esa persona.
 */
@Service
@RequiredArgsConstructor
public class SolicitudBajaService {

    private static final String AUDITORIA_TIPO = "SOLICITUD_BAJA";
    private static final String DOMINIO_ANONIMIZADO = "@baja.fktlomas.local";

    private final SolicitudBajaCuentaRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public SolicitudBajaDto solicitar(Usuario paciente, SolicitarBajaRequest request) {
        if (paciente.getRole() != Role.PACIENTE) {
            throw new IllegalArgumentException("Las cuentas de staff se dan de baja desde el panel de administración, no por acá");
        }
        if (solicitudRepository.existsByUsuarioIdAndEstado(paciente.getId(), EstadoSolicitudBaja.PENDIENTE)) {
            throw new ConflictoDeDatosException("Ya tenés un pedido de baja pendiente de revisión");
        }
        SolicitudBajaCuenta solicitud = new SolicitudBajaCuenta();
        solicitud.setUsuario(paciente);
        solicitud.setMotivo(request.motivo());
        return SolicitudBajaDto.de(solicitudRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<SolicitudBajaDto> listar(EstadoSolicitudBaja estado) {
        return solicitudRepository.buscar(estado).stream().map(SolicitudBajaDto::de).toList();
    }

    @Transactional
    public SolicitudBajaDto aprobar(Long id, Usuario actor) {
        SolicitudBajaCuenta solicitud = obtenerPendiente(id);
        Usuario usuario = solicitud.getUsuario();

        usuario.setNombreCompleto("Cuenta eliminada");
        usuario.setEmail("baja-" + usuario.getId() + DOMINIO_ANONIMIZADO);
        usuario.setDni(null);
        usuario.setTelefono(null);
        usuario.setGenero(null);
        usuario.setObraSocial(null);
        usuario.setActivo(false);
        usuario.setSessionsRevokedAt(Instant.now());
        usuarioRepository.save(usuario);

        solicitud.setEstado(EstadoSolicitudBaja.APROBADA);
        solicitud.setResueltaPor(actor);
        solicitud.setResueltaAt(Instant.now());
        SolicitudBajaDto resuelta = SolicitudBajaDto.de(solicitudRepository.save(solicitud));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "APROBAR", null, resuelta);
        return resuelta;
    }

    @Transactional
    public SolicitudBajaDto rechazar(Long id, Usuario actor, String motivo) {
        SolicitudBajaCuenta solicitud = obtenerPendiente(id);
        solicitud.setEstado(EstadoSolicitudBaja.RECHAZADA);
        solicitud.setMotivoRechazo(motivo);
        solicitud.setResueltaPor(actor);
        solicitud.setResueltaAt(Instant.now());
        SolicitudBajaDto resuelta = SolicitudBajaDto.de(solicitudRepository.save(solicitud));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "RECHAZAR", null, resuelta);
        return resuelta;
    }

    private SolicitudBajaCuenta obtenerPendiente(Long id) {
        SolicitudBajaCuenta solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido de baja no encontrado"));
        if (solicitud.getEstado() != EstadoSolicitudBaja.PENDIENTE) {
            throw new ConflictoDeDatosException("Este pedido ya fue resuelto");
        }
        return solicitud;
    }
}
