package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.ConfirmacionPrevia;
import com.fktlomas.turnos.domain.EstadoAsistencia;
import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.OrigenConsentimiento;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.TipoNotificacion;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.CheckInDto;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.admin.ActualizarAsistenciaRequest;
import com.fktlomas.turnos.dto.admin.ReprogramarTurnoResponse;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.dto.auditoria.TurnoSnapshot;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
import com.fktlomas.turnos.repository.EvolucionClinicaRepository;
import com.fktlomas.turnos.repository.RecordatorioRepository;
import com.fktlomas.turnos.repository.ResenaRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final String AUDITORIA_TIPO_TURNO = "TURNO";

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificacionTurnoService notificacionTurnoService;
    private final NotificacionService notificacionService;
    private final ListaEsperaService listaEsperaService;
    private final AuditoriaService auditoriaService;
    private final EvolucionClinicaRepository evolucionClinicaRepository;
    private final ResenaRepository resenaRepository;
    private final RecordatorioRepository recordatorioRepository;

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

    /** Mismo patrón que conEvolucion, más el estado del recordatorio para pintarlo inline en la agenda operativa. */
    private List<TurnoAdminDto> conEvolucionYRecordatorio(List<Turno> turnos) {
        Set<Long> turnoIds = turnos.stream().map(Turno::getId).collect(Collectors.toSet());
        Set<Long> conEvolucion = turnoIds.isEmpty() ? Set.of() : evolucionClinicaRepository.findByTurnoIdIn(turnoIds)
                .stream().map(e -> e.getTurno().getId()).collect(Collectors.toSet());
        Map<Long, String> estadoRecordatorioPorTurno = turnoIds.isEmpty() ? Map.of() : recordatorioRepository.findByTurnoIdIn(turnoIds)
                .stream().collect(Collectors.toMap(r -> r.getTurno().getId(), r -> r.getEstado().name()));
        return turnos.stream()
                .map(t -> TurnoAdminDto.de(t, conEvolucion.contains(t.getId()), estadoRecordatorioPorTurno.get(t.getId())))
                .toList();
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
                .stream()
                .filter(t -> !yaPaso(t.getFecha(), t.getHoraInicio()))
                .map(TurnoDto::de).toList();
    }

    /**
     * Un turno de hoy generado a una hora que ya pasó (DisponibilidadService genera los del día
     * entero sin importar qué hora es "ahora") sigue quedando DISPONIBLE en la base — hay que
     * filtrarlo acá para no ofrecerlo ni dejar reservarlo, ver disponiblesDeMedico/reservar.
     */
    private static boolean yaPaso(LocalDate fecha, LocalTime horaInicio) {
        LocalDate hoy = LocalDate.now();
        return fecha.isBefore(hoy) || (fecha.isEqual(hoy) && horaInicio.isBefore(LocalTime.now()));
    }

    private static final int SUGERENCIAS_PROXIMA_DISPONIBILIDAD = 5;

    /** Próximas fechas con lugar para ese médico, a partir de (sin incluir) la fecha dada — para sugerirle otro día al paciente cuando la elegida no tiene horarios. */
    @Transactional(readOnly = true)
    public List<LocalDate> proximaDisponibilidad(Long medicoId, LocalDate desde) {
        return turnoRepository.proximasFechasConDisponibilidad(medicoId, desde, PageRequest.of(0, SUGERENCIAS_PROXIMA_DISPONIBILIDAD));
    }

    @Transactional
    public TurnoDto reservar(Long turnoId, Usuario paciente, ReservarTurnoRequest request) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (yaPaso(turno.getFecha(), turno.getHoraInicio())) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }
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
        List<Turno> turnos = turnoRepository.findByPacienteIdOrderByFechaDescHoraInicioDesc(pacienteId);
        Set<Long> turnoIds = turnos.stream().map(Turno::getId).collect(Collectors.toSet());
        Set<Long> conResena = turnoIds.isEmpty() ? Set.of() : Set.copyOf(resenaRepository.turnoIdsConResena(turnoIds));
        return turnos.stream().map(t -> TurnoDto.de(t, conResena.contains(t.getId()))).toList();
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
        // Solo cuando lo cancela secretaría/admin — si lo canceló el propio paciente (o
        // contestó que no venía al link de confirmación previa) no hace falta "avisarle" de
        // su propia acción en la campanita.
        if ("CANCELAR_STAFF".equals(operacionAuditoria)) {
            notificacionService.crear(paciente, TipoNotificacion.TURNO_CANCELADO_STAFF,
                    "Tu turno de " + especialidadNombre + " con " + medicoNombre + " el " + fecha + " a las "
                            + horaInicio + " fue cancelado.", null);
        }
        listaEsperaService.notificarLiberacion(medicoId, fecha);
    }

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> agenda(LocalDate desde, LocalDate hasta, Long medicoId, Long especialidadId) {
        return conEvolucionYRecordatorio(turnoRepository.findAgenda(desde, hasta, medicoId, especialidadId));
    }

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> colaEspera(LocalDate fecha, Long medicoId) {
        return turnoRepository.colaEspera(fecha, medicoId).stream().map(TurnoAdminDto::de).toList();
    }

    /** Para el panel "requiere atención" del resumen admin (ver AdminResumenService). */
    @Transactional(readOnly = true)
    public int turnosSinConfirmarHoy(LocalDate fecha) {
        return (int) turnoRepository.countByFechaAndEstadoAndConfirmacionPrevia(
                fecha, EstadoTurno.RESERVADO, ConfirmacionPrevia.SIN_RESPUESTA);
    }

    /** Check-in de sala de espera — lo marca la secretaria/admin (llegó / ausente / lo vuelve a pendiente). */
    @Transactional
    public TurnoAdminDto actualizarAsistencia(Long turnoId, ActualizarAsistenciaRequest request) {
        turnoRepository.actualizarAsistencia(turnoId, request.estado());
        Turno actualizado = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        return TurnoAdminDto.de(actualizado);
    }

    /**
     * Check-in que el propio paciente dispara al escanear el QR pegado en la sala de espera.
     * Los pacientes con copago de IOMA (mismo criterio que el aviso de WhatsApp post-reserva,
     * ver BookingWizard.jsx) no se autoregistran: tienen que pasar por el mostrador a abonar
     * antes de que la secretaria los sume a la cola de espera.
     */
    @Transactional
    public CheckInDto checkInPorQr(Usuario paciente) {
        List<Turno> turnosHoy = turnoRepository.turnosDeHoyDePaciente(paciente.getId(), LocalDate.now());
        if (turnosHoy.isEmpty()) {
            return new CheckInDto(CheckInDto.Resultado.SIN_TURNO_HOY,
                    "No encontramos ningún turno tuyo para hoy. Si venís por un turno, avisale a la secretaria en el mostrador.",
                    null, null);
        }

        Turno turno = turnosHoy.stream().filter(t -> t.getEstadoAsistencia() == EstadoAsistencia.PENDIENTE).findFirst().orElse(null);
        if (turno == null) {
            Turno primero = turnosHoy.get(0);
            return new CheckInDto(CheckInDto.Resultado.YA_REGISTRADO,
                    "Ya habíamos registrado tu turno de hoy. Esperá que te llamen.",
                    primero.getMedico().getNombreCompleto(), primero.getHoraInicio());
        }

        boolean requiereMostrador = "IOMA".equals(paciente.getObraSocial()) && turno.getMedico().isCobraCopagoIoma();
        if (requiereMostrador) {
            return new CheckInDto(CheckInDto.Resultado.REQUIERE_MOSTRADOR,
                    "Tenés copago de IOMA para esta consulta — acercate al mostrador para registrarte y abonar antes de pasar.",
                    turno.getMedico().getNombreCompleto(), turno.getHoraInicio());
        }

        turno.setEstadoAsistencia(EstadoAsistencia.LLEGO);
        turnoRepository.save(turno);
        return new CheckInDto(CheckInDto.Resultado.LLEGO,
                "¡Listo! Ya avisamos que llegaste. Esperá que te llamen.",
                turno.getMedico().getNombreCompleto(), turno.getHoraInicio());
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

    /**
     * Asignar un turno DISPONIBLE directo a un paciente ya registrado — pensado para la lista
     * de espera (punto 11 del pedido de UX): la secretaria ve quién está anotado para ese
     * médico/día y lo asigna con un clic, sin repetir el alta/lookup por DNI que sí necesita
     * un walk-in nuevo (ver reservarParaPaciente).
     */
    @Transactional
    public TurnoAdminDto asignarTurnoAPaciente(Long turnoId, Long pacienteId, Usuario actor) {
        Turno turnoPrevio = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (yaPaso(turnoPrevio.getFecha(), turnoPrevio.getHoraInicio())) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }
        TurnoSnapshot antes = snapshotDe(turnoPrevio);
        Usuario paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));

        int filas = turnoRepository.reservarSiDisponible(turnoId, paciente, null);
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }

        Turno actualizado = turnoRepository.findById(turnoId).orElseThrow();
        auditoriaService.registrar(actor, AUDITORIA_TIPO_TURNO, turnoId, "ASIGNAR_DESDE_LISTA_ESPERA", antes, snapshotDe(actualizado));
        if (paciente.tieneEmailReal()) {
            emailService.enviarConfirmacionTurno(paciente, actualizado.getMedico().getNombreCompleto(),
                    actualizado.getMedico().getEspecialidad().getNombre(), actualizado.getFecha(),
                    actualizado.getHoraInicio(), actualizado.getHoraFin());
        }
        notificacionService.crear(paciente, TipoNotificacion.TURNO_ASIGNADO_LISTA_ESPERA,
                "Se te asignó un turno de " + actualizado.getMedico().getEspecialidad().getNombre() + " con "
                        + actualizado.getMedico().getNombreCompleto() + " el " + actualizado.getFecha() + " a las "
                        + actualizado.getHoraInicio() + " desde la lista de espera.", actualizado.getId());
        return TurnoAdminDto.de(actualizado);
    }

    @Transactional
    public TurnoAdminDto reservarParaPaciente(Long turnoId, ReservarParaPacienteRequest request, Usuario actor) {
        Turno turnoPrevio = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (yaPaso(turnoPrevio.getFecha(), turnoPrevio.getHoraInicio())) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }
        TurnoSnapshot antes = snapshotDe(turnoPrevio);

        Usuario paciente = usuarioRepository.findByDni(request.dni())
                .orElseGet(() -> crearPacienteTelefonico(request, actor));

        int filas = turnoRepository.reservarSiDisponible(turnoId, paciente, request.motivoConsulta());
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }

        Turno actualizado = turnoRepository.findById(turnoId).orElseThrow();
        auditoriaService.registrar(actor, AUDITORIA_TIPO_TURNO, turnoId, "RESERVAR_PARA_PACIENTE", antes, snapshotDe(actualizado));
        if (paciente.tieneEmailReal()) {
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

    /**
     * Cancelar un turno reservado desde secretaría/admin (ej. el paciente avisó por teléfono) —
     * hoy solo existía la cancelación que hace el propio paciente logueado (cancelar()) y el
     * "deshacer" acotado a la última operación; esto cubre el caso de "cancelar cualquier turno
     * reservado" que pide la agenda operativa. Reusa cancelarInterno tal cual (mismo aviso al
     * paciente, misma auditoría, mismo aviso a la lista de espera).
     */
    @Transactional
    public TurnoAdminDto cancelarComoStaff(Long turnoId, Usuario actor) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (turno.getEstado() != EstadoTurno.RESERVADO) {
            throw new TurnoNoDisponibleException("Este turno no está reservado");
        }
        // Cargado de nuevo por la misma razón que en reprogramar: turno.getPaciente() es un
        // proxy lazy y cancelarInterno se lo pasa tal cual al aviso @Async.
        Usuario paciente = usuarioRepository.findById(turno.getPaciente().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
        cancelarInterno(turno, paciente, "CANCELAR_STAFF");
        return TurnoAdminDto.de(turnoRepository.findById(turnoId).orElseThrow());
    }

    /**
     * Reprograma un turno reservado a otro horario en un solo paso (en vez de cancelar y volver
     * a reservar a mano): toma el destino de forma atómica con el mismo reservarSiDisponible que
     * usan reservar/reservarParaPaciente, libera el origen sin avisarle al paciente que se
     * "canceló" (no es una cancelación real), y borra el recordatorio del origen si existía —
     * ahora apunta a un horario vacío. Queda auditado como dos operaciones separadas
     * (REPROGRAMAR_ORIGEN/REPROGRAMAR_DESTINO) para que "deshacer" sobre cualquiera de los dos
     * turnos revierta esa mitad con el mecanismo genérico que ya existe, sin código nuevo.
     */
    @Transactional
    public ReprogramarTurnoResponse reprogramar(Long origenId, Long destinoId, Usuario actor) {
        Turno origen = turnoRepository.findById(origenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (origen.getEstado() != EstadoTurno.RESERVADO) {
            throw new TurnoNoDisponibleException("Este turno no está reservado, no se puede reprogramar");
        }
        Turno destino = turnoRepository.findById(destinoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (yaPaso(destino.getFecha(), destino.getHoraInicio())) {
            throw new TurnoNoDisponibleException("Ese horario ya no está disponible, elegí otro");
        }

        TurnoSnapshot antesOrigen = snapshotDe(origen);
        TurnoSnapshot antesDestino = snapshotDe(destino);
        // Cargado de nuevo (no origen.getPaciente() directo): esa asociación es lazy, y el
        // proxy sin inicializar revienta con LazyInitializationException al llegar al aviso
        // @Async, que corre después de que esta transacción ya cerró la sesión.
        Usuario paciente = usuarioRepository.findById(origen.getPaciente().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
        String medicoNombre = origen.getMedico().getNombreCompleto();
        String especialidadNombre = origen.getMedico().getEspecialidad().getNombre();
        Long medicoIdOrigen = origen.getMedico().getId();
        LocalDate fechaOrigen = origen.getFecha();

        int filas = turnoRepository.reservarSiDisponible(destinoId, paciente, origen.getMotivoConsulta());
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Ese horario ya no está disponible, elegí otro");
        }

        origen.setEstado(EstadoTurno.DISPONIBLE);
        origen.setPaciente(null);
        origen.setMotivoConsulta(null);
        Turno origenLibre = turnoRepository.save(origen);

        recordatorioRepository.findByTurnoId(origenId).ifPresent(recordatorioRepository::delete);

        Turno destinoActualizado = turnoRepository.findById(destinoId).orElseThrow();

        auditoriaService.registrar(actor, AUDITORIA_TIPO_TURNO, origenId, "REPROGRAMAR_ORIGEN", antesOrigen, snapshotDe(origenLibre));
        auditoriaService.registrar(actor, AUDITORIA_TIPO_TURNO, destinoId, "REPROGRAMAR_DESTINO", antesDestino, snapshotDe(destinoActualizado));

        listaEsperaService.notificarLiberacion(medicoIdOrigen, fechaOrigen);
        notificacionTurnoService.avisarReprogramacion(paciente, medicoNombre, especialidadNombre,
                destinoActualizado.getFecha(), destinoActualizado.getHoraInicio(), destinoActualizado.getHoraFin());
        // reprogramar() es exclusivo de secretaría/admin (ver AdminTurnoController) — no hay
        // vía para que un paciente reprograme su propio turno, así que siempre vale la pena
        // avisarle acá, a diferencia de cancelarInterno más arriba.
        notificacionService.crear(paciente, TipoNotificacion.TURNO_REPROGRAMADO,
                "Tu turno de " + especialidadNombre + " con " + medicoNombre + " se reprogramó para el "
                        + destinoActualizado.getFecha() + " a las " + destinoActualizado.getHoraInicio() + ".",
                destinoActualizado.getId());

        return new ReprogramarTurnoResponse(TurnoAdminDto.de(origenLibre), TurnoAdminDto.de(destinoActualizado));
    }

    /**
     * A diferencia del registro web (checkbox obligatorio + validado en el backend, ver
     * AuthService.registrarPaciente), acá nadie tipeó "acepto" en ningún lado — por eso
     * acepta_politica_privacidad queda en false en vez del `true` ciego que había antes.
     * origen_consentimiento sí distingue los dos casos reales que existen: el bot de WhatsApp
     * manda un aviso pasivo al arrancar la conversación (no es un opt-in, pero es más que nada),
     * y una carga telefónica por secretaria/admin no tiene ningún aviso del sistema en absoluto.
     */
    private Usuario crearPacienteTelefonico(ReservarParaPacienteRequest request, Usuario actor) {
        String email = (request.email() != null && !request.email().isBlank())
                ? request.email()
                : request.dni() + Usuario.DOMINIO_EMAIL_SINTETICO;

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoDeDatosException("Ya existe un paciente registrado con ese email");
        }

        boolean esBot = WhatsAppBotAccountInitializer.EMAIL_BOT.equals(actor.getEmail());

        Usuario paciente = new Usuario();
        paciente.setDni(request.dni());
        paciente.setNombreCompleto(request.nombreCompleto());
        paciente.setEmail(email);
        paciente.setTelefono(request.telefono());
        paciente.setObraSocial(request.obraSocial());
        paciente.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        paciente.setAceptaPoliticaPrivacidad(false);
        paciente.setOrigenConsentimiento(esBot ? OrigenConsentimiento.WHATSAPP : OrigenConsentimiento.TELEFONICA);
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
