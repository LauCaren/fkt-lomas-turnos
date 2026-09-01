package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.ConfirmacionPrevia;
import com.fktlomas.turnos.domain.EstadoAsistencia;
import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.admin.ActualizarAsistenciaRequest;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.dto.auditoria.TurnoSnapshot;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
import com.fktlomas.turnos.repository.EvolucionClinicaRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final String DOMINIO_EMAIL_SINTETICO = "@sin-email.fktlomas.local";
    private static final String AUDITORIA_TIPO_TURNO = "TURNO";

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificacionTurnoService notificacionTurnoService;
    private final ListaEsperaService listaEsperaService;
    private final AuditoriaService auditoriaService;
    private final EvolucionClinicaRepository evolucionClinicaRepository;

    /**
     * Marca en cada turno si el médico ya cargó una evolución — nunca el contenido, que es
     * privado del profesional (ver EvolucionClinica) — para que el admin sepa que existe sin
     * poder leerla.
     */
    private List<TurnoAdminDto> conEvolucion(List<Turno> turnos) {
        Set<Long> turnoIds = turnos.stream().map(Turno::getId).collect(Collectors.toSet());
        Set<Long> conEvolucion = turnoIds.isEmpty() ? Set.of() : evolucionClinicaRepository.findByTurnoIdIn(turnoIds)
                .stream().map(e -> e.getTurno().getId()).collect(Collectors.toSet());
        return turnos.stream().map(t -> TurnoAdminDto.de(t, conEvolucion.contains(t.getId()))).toList();
    }

    private static TurnoSnapshot snapshotDe(Turno t) {
        return new TurnoSnapshot(
                t.getEstado().name(),
                t.getPaciente() != null ? t.getPaciente().getId() : null,
                t.getMotivoConsulta(),
                t.getEstadoAsistencia().name()
        );
    }

    @Transactional(readOnly = true)
    public List<TurnoDto> disponiblesDeMedico(Long medicoId, LocalDate fecha) {
        return turnoRepository.findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(medicoId, fecha, EstadoTurno.DISPONIBLE)
                .stream().map(TurnoDto::de).toList();
    }

    @Transactional
    public TurnoDto reservar(Long turnoId, Usuario paciente, ReservarTurnoRequest request) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        TurnoSnapshot antes = snapshotDe(turno);

        int filas = turnoRepository.reservarSiDisponible(turnoId, paciente, request.motivoConsulta());
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }

        Turno actualizado = turnoRepository.findById(turnoId).orElseThrow();
        auditoriaService.registrar(paciente, AUDITORIA_TIPO_TURNO, turnoId, "RESERVAR", antes, snapshotDe(actualizado));
        notificacionTurnoService.confirmarReserva(paciente, actualizado.getMedico().getNombreCompleto(),
                actualizado.getMedico().getEspecialidad().getNombre(), actualizado.getFecha(),
                actualizado.getHoraInicio(), actualizado.getHoraFin());
        return TurnoDto.de(actualizado);
    }

    @Transactional(readOnly = true)
    public List<TurnoDto> misTurnos(Long pacienteId) {
        return turnoRepository.findByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId)
                .stream().map(TurnoDto::de).toList();
    }

    @Transactional
    public void cancelar(Long turnoId, Usuario paciente) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        if (turno.getPaciente() == null || !turno.getPaciente().getId().equals(paciente.getId())) {
            throw new RecursoNoEncontradoException("Turno no encontrado");
        }
        if (turno.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede cancelar un turno que ya pasó");
        }
        cancelarInterno(turno, paciente, "CANCELAR");
    }

    /**
     * Confirmación de asistencia del día anterior (punto pedido explícitamente): el link del
     * recordatorio no requiere login, así que el token hace las veces de credencial — es un
     * UUID no adivinable, generado por RecordatorioService. Si el paciente avisa que no puede
     * venir, se libera el turno igual que una cancelación común.
     */
    @Transactional
    public void confirmarPorToken(String token, boolean confirma) {
        Turno turno = turnoRepository.findByTokenConfirmacion(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("El link no es válido o ya venció"));

        if (turno.getEstado() != EstadoTurno.RESERVADO) {
            // El turno ya se canceló o reprogramó por otra vía — no hay nada que confirmar.
            return;
        }

        turno.setConfirmacionPrevia(confirma ? ConfirmacionPrevia.CONFIRMADO : ConfirmacionPrevia.RECHAZADO);
        if (confirma) {
            turnoRepository.save(turno);
        } else {
            cancelarInterno(turno, turno.getPaciente(), "CANCELAR_POR_CONFIRMACION_PREVIA");
        }
    }

    private void cancelarInterno(Turno turno, Usuario paciente, String operacionAuditoria) {
        TurnoSnapshot antes = snapshotDe(turno);
        String medicoNombre = turno.getMedico().getNombreCompleto();
        String especialidadNombre = turno.getMedico().getEspecialidad().getNombre();
        Long medicoId = turno.getMedico().getId();
        Long turnoId = turno.getId();
        LocalDate fecha = turno.getFecha();
        LocalTime horaInicio = turno.getHoraInicio();
        LocalTime horaFin = turno.getHoraFin();

        turno.setEstado(EstadoTurno.DISPONIBLE);
        turno.setPaciente(null);
        turno.setMotivoConsulta(null);
        // La confirmación/rechazo previo queda visible en la agenda mientras el turno siga
        // libre (útil para la secretaria) — se limpia recién cuando alguien lo reserva de
        // nuevo, ver reservarSiDisponible.
        Turno guardado = turnoRepository.save(turno);
        auditoriaService.registrar(paciente, AUDITORIA_TIPO_TURNO, turnoId, operacionAuditoria, antes, snapshotDe(guardado));

        notificacionTurnoService.confirmarCancelacion(paciente, medicoNombre, especialidadNombre, fecha, horaInicio, horaFin);
        listaEsperaService.notificarLiberacion(medicoId, fecha);
    }

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> agenda(LocalDate desde, LocalDate hasta, Long medicoId, Long especialidadId) {
        return conEvolucion(turnoRepository.findAgenda(desde, hasta, medicoId, especialidadId));
    }

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> colaEspera(LocalDate fecha, Long medicoId) {
        return turnoRepository.colaEspera(fecha, medicoId).stream().map(TurnoAdminDto::de).toList();
    }

    /** Check-in de sala de espera — lo marca la secretaria/admin (llegó / ausente / lo vuelve a pendiente). */
    @Transactional
    public TurnoAdminDto actualizarAsistencia(Long turnoId, ActualizarAsistenciaRequest request) {
        turnoRepository.actualizarAsistencia(turnoId, request.estado());
        Turno actualizado = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        return TurnoAdminDto.de(actualizado);
    }

    private static final LocalDate DESDE_MINIMO = LocalDate.of(2000, 1, 1);
    private static final LocalDate HASTA_MAXIMO = LocalDate.of(2099, 12, 31);

    @Transactional(readOnly = true)
    public Page<TurnoAdminDto> buscarTurnos(LocalDate desde, LocalDate hasta, Long medicoId, Long especialidadId, EstadoTurno estado, Pageable pageable) {
        LocalDate desdeEfectivo = desde != null ? desde : DESDE_MINIMO;
        LocalDate hastaEfectivo = hasta != null ? hasta : HASTA_MAXIMO;
        Page<Turno> pagina = turnoRepository.buscarTurnos(desdeEfectivo, hastaEfectivo, medicoId, especialidadId, estado, pageable);
        return new org.springframework.data.domain.PageImpl<>(conEvolucion(pagina.getContent()), pageable, pagina.getTotalElements());
    }

    @Transactional
    public TurnoAdminDto reservarParaPaciente(Long turnoId, ReservarParaPacienteRequest request, Usuario actor) {
        Turno turnoPrevio = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        TurnoSnapshot antes = snapshotDe(turnoPrevio);

        Usuario paciente = usuarioRepository.findByDni(request.dni())
                .orElseGet(() -> crearPacienteTelefonico(request));

        int filas = turnoRepository.reservarSiDisponible(turnoId, paciente, request.motivoConsulta());
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }

        Turno actualizado = turnoRepository.findById(turnoId).orElseThrow();
        auditoriaService.registrar(actor, AUDITORIA_TIPO_TURNO, turnoId, "RESERVAR_PARA_PACIENTE", antes, snapshotDe(actualizado));
        if (paciente.getEmail() != null && !paciente.getEmail().endsWith(DOMINIO_EMAIL_SINTETICO)) {
            emailService.enviarConfirmacionTurno(paciente, actualizado.getMedico().getNombreCompleto(),
                    actualizado.getMedico().getEspecialidad().getNombre(), actualizado.getFecha(),
                    actualizado.getHoraInicio(), actualizado.getHoraFin());
        }
        return TurnoAdminDto.de(actualizado);
    }

    /**
     * Undo acotado a turnos/agenda (punto 11): deshace la última operación no deshecha
     * registrada en auditoría para este turno (reservar, cancelar o reservar-para-paciente),
     * volviendo el turno al estado que tenía justo antes de esa operación.
     */
    @Transactional
    public TurnoAdminDto deshacerUltimaOperacion(Long turnoId, Usuario actor) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        TurnoSnapshot snapshotActual = snapshotDe(turno);
        TurnoSnapshot snapshotAntes = auditoriaService.snapshotParaDeshacer(AUDITORIA_TIPO_TURNO, turnoId, TurnoSnapshot.class);

        turno.setEstado(EstadoTurno.valueOf(snapshotAntes.estado()));
        turno.setPaciente(snapshotAntes.pacienteId() != null ? usuarioRepository.findById(snapshotAntes.pacienteId()).orElse(null) : null);
        turno.setMotivoConsulta(snapshotAntes.motivoConsulta());
        turno.setEstadoAsistencia(EstadoAsistencia.valueOf(snapshotAntes.estadoAsistencia()));
        Turno restaurado = turnoRepository.save(turno);

        auditoriaService.registrar(actor, AUDITORIA_TIPO_TURNO, turnoId, "DESHACER", snapshotActual, snapshotAntes);
        return TurnoAdminDto.de(restaurado);
    }

    private Usuario crearPacienteTelefonico(ReservarParaPacienteRequest request) {
        String email = (request.email() != null && !request.email().isBlank())
                ? request.email()
                : request.dni() + DOMINIO_EMAIL_SINTETICO;

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoDeDatosException("Ya existe un paciente registrado con ese email");
        }

        Usuario paciente = new Usuario();
        paciente.setDni(request.dni());
        paciente.setNombreCompleto(request.nombreCompleto());
        paciente.setEmail(email);
        paciente.setTelefono(request.telefono());
        paciente.setObraSocial(request.obraSocial());
        paciente.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        paciente.setAceptaPoliticaPrivacidad(true);
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
