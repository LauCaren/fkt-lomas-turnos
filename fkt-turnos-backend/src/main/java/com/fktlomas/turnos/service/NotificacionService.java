package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Notificacion;
import com.fktlomas.turnos.domain.TipoNotificacion;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.NotificacionDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notificaciones dentro de la app (campanita del header) — se suman a, no reemplazan, el aviso
 * por email/WhatsApp que ya manda NotificacionTurnoService: ese puede fallar en silencio (canal
 * no configurado, número inválido, etc, ver CanalFallbackService) y de cualquier forma el
 * paciente puede no revisar el mail; esto queda visible la próxima vez que entra a la app.
 * Se crea una fila por cada evento relevante sobre un turno que el paciente no generó él mismo
 * (lo canceló o reprogramó secretaría/admin, o se le asignó un lugar de la lista de espera) —
 * ver los call sites en TurnoService y ListaEsperaService.
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Transactional
    public void crear(Usuario destinatario, TipoNotificacion tipo, String mensaje, Long turnoId) {
        notificacionRepository.save(new Notificacion(destinatario, tipo, mensaje, turnoId));
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDto> mias(Long usuarioId, Pageable pageable) {
        return notificacionRepository.findByUsuarioIdOrderByCreatedAtDescIdDesc(usuarioId, pageable).map(NotificacionDto::de);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Transactional
    public void marcarLeida(Long id, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"));
        // 404 en vez de 403 si es de otro usuario — mismo criterio que el resto del sistema
        // (ver TurnoService.cancelar), no hay necesidad de confirmarle a nadie que el id existe.
        if (!notificacion.getUsuario().getId().equals(usuarioId)) {
            throw new RecursoNoEncontradoException("Notificación no encontrada");
        }
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }
}
