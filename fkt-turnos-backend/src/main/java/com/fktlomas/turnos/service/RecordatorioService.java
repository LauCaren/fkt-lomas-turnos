package com.fktlomas.turnos.service;

import com.fktlomas.turnos.util.DatosPersonales;
import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.ConfiguracionRecordatoriosDto;
import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.RecordatorioDto;
import com.fktlomas.turnos.dto.admin.ConfiguracionRecordatoriosRequest;
import com.fktlomas.turnos.dto.admin.MensajeMasivoResponse;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.ConfiguracionRecordatoriosRepository;
import com.fktlomas.turnos.repository.RecordatorioRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Job de recordatorios de turno. A propósito NO envuelve todo procesarPendientes()
 * en un único @Transactional: los envíos pegan a servicios externos (Twilio, SMTP)
 * uno por turno, y mantener una transacción/conexión abierta durante ese tiempo
 * (potencialmente varios turnos, cada uno con su propio round-trip de red) no vale
 * la pena a esta escala. Cada lectura/escritura puntual (findReservadosEntreFechas,
 * findByTurnoId, save) ya es transaccional por sí misma (Spring Data JPA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private static final int MAX_INTENTOS = 3;
    private static final int MAX_LARGO_ERROR = 500;
    private static final int MAX_DIAS_MENSAJE_MASIVO = 60;
    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    // Este job corre cada pocos minutos (app.recordatorios.intervalo-minutos); sin este
    // enfriamiento, una falla persistente (ej. la fila de configuración borrada, la base
    // caída) le mandaría un mail nuevo al admin en cada corrida en vez de uno solo por hora.
    private static final Duration ENFRIAMIENTO_ALERTA_FALLO = Duration.ofHours(1);
    private volatile Instant ultimaAlertaFallo;

    private final TurnoRepository turnoRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionRecordatoriosRepository configuracionRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final CanalFallbackService canalFallbackService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Scheduled(fixedDelayString = "#{${app.recordatorios.intervalo-minutos} * 60000}")
    public void procesarPendientes() {
        // Los fallos de un recordatorio puntual (Twilio/SMTP caído, etc.) ya los maneja
        // intentarEnvio() por turno, marcando el Recordatorio como FALLIDO con reintento
        // manual desde el panel — este try/catch es para lo que puede tirar el método entero
        // antes de llegar ahí (ej. falta la fila de configuración, la base no responde), que
        // si no se avisa, deja de correr en silencio hasta que un paciente falta a un turno.
        try {
            ConfiguracionRecordatorios config = obtenerConfiguracionEntidad();
            ZonedDateTime ahora = ZonedDateTime.now(ZONA_CLINICA);
            ZonedDateTime limite = ahora.plusHours(config.getHorasAnticipacion());

            List<Turno> candidatos = turnoRepository.findReservadosEntreFechas(ahora.toLocalDate(), limite.toLocalDate());

            int procesados = 0;
            for (Turno turno : candidatos) {
                ZonedDateTime fechaHoraTurno = ZonedDateTime.of(turno.getFecha(), turno.getHoraInicio(), ZONA_CLINICA);
                if (fechaHoraTurno.isBefore(ahora) || fechaHoraTurno.isAfter(limite)) {
                    continue;
                }

                Recordatorio recordatorio = recordatorioRepository.findByTurnoId(turno.getId())
                        .orElseGet(() -> recordatorioRepository.save(
                                new Recordatorio(turno, turno.getPaciente().getCanalNotificacionPreferido())));

                if (recordatorio.getEstado() != EstadoRecordatorio.PENDIENTE) {
                    continue;
                }
                intentarEnvio(recordatorio, config.getPlantillaMensaje());
                procesados++;
            }
            log.info("Recordatorios procesados en esta corrida: {}", procesados);
        } catch (Exception e) {
            log.error("El job de recordatorios falló y no procesó nada en esta corrida", e);
            alertarFalloSiCorresponde(e);
        }
    }

    private void alertarFalloSiCorresponde(Exception causa) {
        Instant ahora = Instant.now();
        Instant ultima = ultimaAlertaFallo;
        if (ultima != null && Duration.between(ultima, ahora).compareTo(ENFRIAMIENTO_ALERTA_FALLO) < 0) {
            return;
        }
        ultimaAlertaFallo = ahora;
        emailService.notificarFalloJobRecordatorios(causa.getMessage());
    }

    /** Reintento manual desde el panel — corre aunque el recordatorio ya haya llegado a FALLIDO (el tope de intentos solo frena los reintentos automáticos). */
    @Transactional
    public RecordatorioDto reenviar(Long recordatorioId) {
        Recordatorio recordatorio = recordatorioRepository.findById(recordatorioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recordatorio no encontrado"));
        intentarEnvio(recordatorio, obtenerConfiguracionEntidad().getPlantillaMensaje());
        return RecordatorioDto.de(recordatorio);
    }

    /**
     * "Enviar recordatorio" manual desde la agenda (punto 12 del pedido de UX): salta la
     * ventana de anticipación del job automático — el turno puede estar todavía lejos en el
     * tiempo, no importa. Get-or-create el Recordatorio del turno (mismo patrón que usa
     * procesarPendientes) y lo manda ya.
     */
    @Transactional
    public RecordatorioDto enviarAhora(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (turno.getEstado() != EstadoTurno.RESERVADO) {
            throw new IllegalArgumentException("Este turno no está reservado");
        }
        Recordatorio recordatorio = recordatorioRepository.findByTurnoId(turnoId)
                .orElseGet(() -> recordatorioRepository.save(
                        new Recordatorio(turno, turno.getPaciente().getCanalNotificacionPreferido())));
        intentarEnvio(recordatorio, obtenerConfiguracionEntidad().getPlantillaMensaje());
        return RecordatorioDto.de(recordatorio);
    }

    @Transactional(readOnly = true)
    public List<RecordatorioDto> listar() {
        return recordatorioRepository.listarConDetalle().stream().map(RecordatorioDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracionRecordatoriosDto obtenerConfiguracion() {
        return ConfiguracionRecordatoriosDto.de(obtenerConfiguracionEntidad());
    }

    @Transactional
    public ConfiguracionRecordatoriosDto actualizarConfiguracion(ConfiguracionRecordatoriosRequest request) {
        ConfiguracionRecordatorios config = obtenerConfiguracionEntidad();
        config.setPlantillaMensaje(request.plantillaMensaje());
        config.setHorasAnticipacion(request.horasAnticipacion());
        return ConfiguracionRecordatoriosDto.de(config);
    }

    @Transactional(readOnly = true)
    public List<PacienteBusquedaDto> buscarPacientes(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return List.of();
        }
        return usuarioRepository.buscarPacientes(busqueda.trim()).stream().map(PacienteBusquedaDto::de).toList();
    }

    @Transactional
    public PacienteBusquedaDto actualizarCanalPreferido(Long pacienteId, CanalNotificacion canal) {
        Usuario paciente = obtenerPaciente(pacienteId);
        paciente.setCanalNotificacionPreferido(canal);
        return PacienteBusquedaDto.de(paciente);
    }

    /** Mensaje libre a un paciente puntual, fuera del recordatorio automático. No queda registrado en `recordatorios`: esa tabla está atada a un turno, y esto no lo está. */
    @Transactional(readOnly = true)
    public void enviarMensajeManual(Long pacienteId, CanalNotificacion canal, String mensaje) {
        Usuario paciente = obtenerPaciente(pacienteId);
        enviarPorCanal(canal, paciente, "Mensaje de FKT Lomas", mensaje);
    }

    /** Vista previa de a quién le llegaría un aviso masivo — mismo criterio que {@link #enviarMensajeMasivo}, sin enviar nada. */
    @Transactional(readOnly = true)
    public List<PacienteBusquedaDto> pacientesEnRango(LocalDate desde, LocalDate hasta, Long medicoId) {
        return pacientesConTurnoEnRango(desde, hasta, medicoId).stream().map(PacienteBusquedaDto::de).toList();
    }

    /**
     * Aviso libre a todos los pacientes con un turno reservado en un rango de fechas (ej. "cerramos
     * el feriado tal") — a diferencia de {@link #enviarMensajeManual}, no es a un paciente puntual.
     * Cada paciente recibe el mensaje por su canal preferido; un paciente con varios turnos en el
     * rango lo recibe una sola vez. Best-effort: si falla el envío a un paciente puntual, se loguea
     * y se sigue con el resto — no tiene sentido que uno solo tire abajo todo el envío.
     */
    @Transactional(readOnly = true)
    public MensajeMasivoResponse enviarMensajeMasivo(LocalDate desde, LocalDate hasta, Long medicoId, String mensaje) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha 'hasta' debe ser posterior a 'desde'");
        }
        if (desde.plusDays(MAX_DIAS_MENSAJE_MASIVO).isBefore(hasta)) {
            throw new IllegalArgumentException("El rango no puede superar los " + MAX_DIAS_MENSAJE_MASIVO + " días");
        }
        int enviados = 0;
        for (Usuario paciente : pacientesConTurnoEnRango(desde, hasta, medicoId)) {
            try {
                enviarPorCanal(paciente.getCanalNotificacionPreferido(), paciente, "Aviso de FKT Lomas", mensaje);
                enviados++;
            } catch (Exception e) {
                log.warn("No se pudo enviar el aviso masivo al paciente {} ({}): {}", paciente.getId(), DatosPersonales.enmascararEmail(paciente.getEmail()), e.getMessage());
            }
        }
        return new MensajeMasivoResponse(enviados);
    }

    private List<Usuario> pacientesConTurnoEnRango(LocalDate desde, LocalDate hasta, Long medicoId) {
        return turnoRepository.findReservadosEntreFechas(desde, hasta).stream()
                .filter(t -> medicoId == null || t.getMedico().getId().equals(medicoId))
                .map(Turno::getPaciente)
                .collect(Collectors.toMap(Usuario::getId, p -> p, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Usuario::getNombreCompleto))
                .toList();
    }

    private Usuario obtenerPaciente(Long pacienteId) {
        Usuario paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
        if (paciente.getRole() != Role.PACIENTE) {
            throw new RecursoNoEncontradoException("Paciente no encontrado");
        }
        return paciente;
    }

    private void intentarEnvio(Recordatorio recordatorio, String plantilla) {
        Turno turno = recordatorio.getTurno();
        try {
            if (turno.getTokenConfirmacion() == null) {
                turno.setTokenConfirmacion(UUID.randomUUID().toString());
                turno = turnoRepository.save(turno);
            }
            Turno turnoFinal = turno;
            // El fallback de canal (reintentar por el otro si falla el preferido) vive en
            // CanalFallbackService, compartido con NotificacionTurnoService — acá solo se decide
            // CÓMO se manda por cada canal.
            CanalNotificacion canalUsado = canalFallbackService.enviarConFallback(turno.getPaciente(),
                    canal -> enviarRecordatorioPorCanal(canal, turnoFinal, plantilla));
            recordatorio.setCanal(canalUsado);
            recordatorio.setEstado(EstadoRecordatorio.ENVIADO);
            recordatorio.setFechaEnvio(Instant.now());
            recordatorio.setErrorMensaje(null);
        } catch (Exception e) {
            recordatorio.setIntentos(recordatorio.getIntentos() + 1);
            recordatorio.setErrorMensaje(acortar(e.getMessage()));
            recordatorio.setEstado(recordatorio.getIntentos() >= MAX_INTENTOS
                    ? EstadoRecordatorio.FALLIDO
                    : EstadoRecordatorio.PENDIENTE);
            log.warn("Fallo al enviar recordatorio del turno {}: {}", turno.getId(), e.getMessage());
        }
        recordatorioRepository.save(recordatorio);
    }

    private void enviarRecordatorioPorCanal(CanalNotificacion canal, Turno turno, String plantilla) {
        Usuario paciente = turno.getPaciente();
        switch (canal) {
            // El email usa el diseño HTML de la clínica con copy fijo — la plantilla editable
            // por el admin (`plantilla`) no tiene dónde insertarse en ese layout, así que solo
            // se usa para WhatsApp, que sigue siendo texto plano.
            case EMAIL -> emailService.enviarRecordatorioTurnoHtmlSync(
                    paciente.getEmail(), paciente.getNombreCompleto(),
                    turno.getMedico().getNombreCompleto(), turno.getMedico().getEspecialidad().getNombre(),
                    turno.getFecha(), turno.getHoraInicio(), turno.getTokenConfirmacion());
            case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), renderizarPlantilla(plantilla, turno));
        }
    }

    private void enviarPorCanal(CanalNotificacion canal, Usuario paciente, String asunto, String texto) {
        switch (canal) {
            case EMAIL -> emailService.enviarRecordatorioSync(paciente.getEmail(), asunto, texto);
            case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), texto);
        }
    }

    private String renderizarPlantilla(String plantilla, Turno turno) {
        String mensaje = plantilla
                .replace("{paciente}", turno.getPaciente().getNombreCompleto())
                .replace("{profesional}", turno.getMedico().getNombreCompleto())
                .replace("{especialidad}", turno.getMedico().getEspecialidad().getNombre())
                .replace("{fecha}", turno.getFecha().format(FORMATO_FECHA))
                .replace("{hora}", turno.getHoraInicio().format(FORMATO_HORA));

        // Se agrega siempre, no depende de que la plantilla (editable por el admin) tenga un
        // placeholder para esto — así el link de confirmación nunca queda afuera por error.
        String linkConfirmar = frontendUrl + "/confirmar-turno?token=" + turno.getTokenConfirmacion() + "&respuesta=si";
        String linkRechazar = frontendUrl + "/confirmar-turno?token=" + turno.getTokenConfirmacion() + "&respuesta=no";
        return mensaje + "\n\n¿Vas a poder venir?\nSí, confirmo: " + linkConfirmar + "\nNo voy a poder ir: " + linkRechazar;
    }

    private ConfiguracionRecordatorios obtenerConfiguracionEntidad() {
        return configuracionRepository.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de configuración de recordatorios (id=1)"));
    }

    private static String acortar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() > MAX_LARGO_ERROR ? mensaje.substring(0, MAX_LARGO_ERROR) : mensaje;
    }
}
