package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoArrepentimiento;
import com.fktlomas.turnos.domain.EstadoSolicitudBaja;
import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.repository.ConversacionWhatsAppRepository;
import com.fktlomas.turnos.repository.MensajeContactoRepository;
import com.fktlomas.turnos.repository.NotificacionRepository;
import com.fktlomas.turnos.repository.PostulacionRepository;
import com.fktlomas.turnos.repository.SolicitudArrepentimientoRepository;
import com.fktlomas.turnos.repository.SolicitudBajaCuentaRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Borrado automático de los datos personales que ya no hacen falta — la Ley 25.326 (art. 4,
 * inc. 7) pide destruir los datos cuando dejan de ser necesarios para la finalidad con la que se
 * juntaron, y la Política de Privacidad del sitio promete estos plazos (sección "Cuánto tiempo
 * conservamos tus datos"). Si se cambia un plazo acá, hay que cambiarlo también ahí.
 *
 * <p>Qué NO se borra nunca desde acá, a propósito:
 * <ul>
 *   <li>Turnos, evoluciones, planes de kinesiología e ingresos: son historia clínica y
 *       registros contables, con obligación de conservarlos (10 años, Ley 26.529 art. 18).</li>
 *   <li>Registros de auditoría: son la constancia de qué se hizo y cuándo.</li>
 *   <li>Pedidos de baja o de arrepentimiento todavía pendientes (los resueltos se guardan 5 años
 *       como constancia de que el centro los gestionó, y después se borran).</li>
 * </ul>
 *
 * <p>Corre todas las madrugadas (03:30, hora de Argentina). Los plazos se pueden ajustar por
 * variable de entorno sin tocar código (ver app.retencion.* en application.yml).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetencionDatosService {

    private final PostulacionRepository postulacionRepository;
    private final MensajeContactoRepository mensajeContactoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ConversacionWhatsAppRepository conversacionWhatsAppRepository;
    private final SolicitudArrepentimientoRepository solicitudArrepentimientoRepository;
    private final SolicitudBajaCuentaRepository solicitudBajaCuentaRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.retencion.postulaciones-dias}")
    private int diasPostulaciones;

    @Value("${app.retencion.mensajes-contacto-dias}")
    private int diasMensajesContacto;

    @Value("${app.retencion.notificaciones-dias}")
    private int diasNotificaciones;

    @Value("${app.retencion.conversaciones-whatsapp-dias}")
    private int diasConversacionesWhatsApp;

    @Value("${app.retencion.pedidos-resueltos-dias}")
    private int diasPedidosResueltos;

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Argentina/Buenos_Aires")
    @SchedulerLock(name = "retencionDatos")
    @Transactional
    public void aplicarPlazos() {
        Instant ahora = Instant.now();

        // Las postulaciones se borran una por una para poder borrar también el CV del storage.
        List<Postulacion> postulaciones = postulacionRepository.findByCreatedAtBefore(ahora.minus(Duration.ofDays(diasPostulaciones)));
        postulaciones.forEach(p -> fileStorageService.borrar(p.getCvPath()));
        postulacionRepository.deleteAll(postulaciones);

        long mensajes = mensajeContactoRepository.deleteByCreatedAtBefore(ahora.minus(Duration.ofDays(diasMensajesContacto)));
        long notificaciones = notificacionRepository.deleteByCreatedAtBefore(ahora.minus(Duration.ofDays(diasNotificaciones)));
        long conversaciones = conversacionWhatsAppRepository.deleteByUltimaActividadBefore(ahora.minus(Duration.ofDays(diasConversacionesWhatsApp)));
        Instant limitePedidos = ahora.minus(Duration.ofDays(diasPedidosResueltos));
        long pedidos = solicitudArrepentimientoRepository.deleteByEstadoAndResueltaAtBefore(EstadoArrepentimiento.RESUELTA, limitePedidos)
                + solicitudBajaCuentaRepository.deleteByEstadoNotAndResueltaAtBefore(EstadoSolicitudBaja.PENDIENTE, limitePedidos);

        if (!postulaciones.isEmpty() || mensajes + notificaciones + conversaciones + pedidos > 0) {
            log.info("Retención de datos: borradas {} postulaciones, {} mensajes de contacto, {} notificaciones, {} conversaciones de WhatsApp y {} pedidos resueltos vencidos",
                    postulaciones.size(), mensajes, notificaciones, conversaciones, pedidos);
        }
    }
}
