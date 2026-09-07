package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoAsistencia;
import com.fktlomas.turnos.domain.EstadoResena;
import com.fktlomas.turnos.domain.Resena;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.resena.CrearResenaRequest;
import com.fktlomas.turnos.dto.resena.ResenaDto;
import com.fktlomas.turnos.dto.resena.ResenaPublicaDto;
import com.fktlomas.turnos.dto.resena.ResumenResenasDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.ResenaRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Calificación de un turno real ya atendido — ver Resena. Reemplaza (a futuro) las reseñas
 * fijas de FKT Pagina/opiniones.html por prueba social genuina, moderada antes de publicarse
 * igual que un pedido de baja de cuenta (ver SolicitudBajaService, mismo patrón).
 */
@Service
@RequiredArgsConstructor
public class ResenaService {

    private static final String AUDITORIA_TIPO = "RESENA";

    private final ResenaRepository resenaRepository;
    private final TurnoRepository turnoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public ResenaDto crear(Long turnoId, Usuario paciente, CrearResenaRequest request) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (turno.getPaciente() == null || !turno.getPaciente().getId().equals(paciente.getId())) {
            throw new RecursoNoEncontradoException("Turno no encontrado");
        }
        if (turno.getEstadoAsistencia() != EstadoAsistencia.ATENDIDO) {
            throw new IllegalArgumentException("Solo se puede calificar un turno que ya fue atendido");
        }
        if (resenaRepository.existsByTurnoId(turnoId)) {
            throw new ConflictoDeDatosException("Ya calificaste este turno");
        }

        Resena resena = new Resena();
        resena.setTurno(turno);
        resena.setPaciente(paciente);
        resena.setMedico(turno.getMedico());
        resena.setPuntaje(request.puntaje());
        resena.setComentario(request.comentario());
        return ResenaDto.de(resenaRepository.save(resena));
    }

    @Transactional(readOnly = true)
    public List<ResenaDto> listarParaModeracion(EstadoResena estado) {
        return resenaRepository.buscar(estado).stream().map(ResenaDto::de).toList();
    }

    @Transactional
    public ResenaDto aprobar(Long id, Usuario actor) {
        Resena resena = obtenerPendiente(id);
        resena.setEstado(EstadoResena.APROBADA);
        resena.setResueltaPor(actor);
        resena.setResueltaAt(Instant.now());
        ResenaDto resuelta = ResenaDto.de(resenaRepository.save(resena));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "APROBAR", null, resuelta);
        return resuelta;
    }

    @Transactional
    public ResenaDto rechazar(Long id, Usuario actor, String motivo) {
        Resena resena = obtenerPendiente(id);
        resena.setEstado(EstadoResena.RECHAZADA);
        resena.setMotivoRechazo(motivo);
        resena.setResueltaPor(actor);
        resena.setResueltaAt(Instant.now());
        ResenaDto resuelta = ResenaDto.de(resenaRepository.save(resena));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "RECHAZAR", null, resuelta);
        return resuelta;
    }

    @Transactional(readOnly = true)
    public ResumenResenasDto publicasDeMedico(Long medicoId) {
        List<ResenaPublicaDto> resenas = resenaRepository.aprobadasDeMedico(medicoId)
                .stream().map(ResenaPublicaDto::de).toList();
        double promedio = BigDecimal.valueOf(resenaRepository.promedioDeMedico(medicoId))
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        return new ResumenResenasDto(promedio, resenas.size(), resenas);
    }

    /** Para el sitio de marketing (FKT Pagina/opiniones.html) — últimas reseñas del centro entero, no de un médico puntual. */
    @Transactional(readOnly = true)
    public List<ResenaPublicaDto> ultimasAprobadas(int limite) {
        return resenaRepository.ultimasAprobadas(PageRequest.of(0, limite))
                .stream().map(ResenaPublicaDto::de).toList();
    }

    private Resena obtenerPendiente(Long id) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada"));
        if (resena.getEstado() != EstadoResena.PENDIENTE) {
            throw new ConflictoDeDatosException("Esta reseña ya fue resuelta");
        }
        return resena;
    }
}
