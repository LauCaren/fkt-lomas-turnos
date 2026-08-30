package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.ConfiguracionRecordatoriosDto;
import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.RecordatorioDto;
import com.fktlomas.turnos.dto.admin.ConfiguracionRecordatoriosRequest;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.ConfiguracionRecordatoriosRepository;
import com.fktlomas.turnos.repository.RecordatorioRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurnoRepository turnoRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionRecordatoriosRepository configuracionRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    @Scheduled(fixedDelayString = "#{${app.recordatorios.intervalo-minutos} * 60000}")
    public void procesarPendientes() {
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
    }

    /** Reintento manual desde el panel — corre aunque el recordatorio ya haya llegado a FALLIDO (el tope de intentos solo frena los reintentos automáticos). */
    @Transactional
    public RecordatorioDto reenviar(Long recordatorioId) {
        Recordatorio recordatorio = recordatorioRepository.findById(recordatorioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recordatorio no encontrado"));
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
            String texto = renderizarPlantilla(plantilla, turno);
            enviarPorCanal(recordatorio.getCanal(), turno.getPaciente(), "Recordatorio de turno — FKT Lomas", texto);

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

    private void enviarPorCanal(CanalNotificacion canal, Usuario paciente, String asunto, String texto) {
        switch (canal) {
            case EMAIL -> emailService.enviarRecordatorioSync(paciente.getEmail(), asunto, texto);
            case WHATSAPP -> whatsAppService.enviar(paciente.getTelefono(), texto);
        }
    }

    private String renderizarPlantilla(String plantilla, Turno turno) {
        return plantilla
                .replace("{paciente}", turno.getPaciente().getNombreCompleto())
                .replace("{profesional}", turno.getMedico().getNombreCompleto())
                .replace("{especialidad}", turno.getMedico().getEspecialidad().getNombre())
                .replace("{fecha}", turno.getFecha().format(FORMATO_FECHA))
                .replace("{hora}", turno.getHoraInicio().format(FORMATO_HORA));
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
