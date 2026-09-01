package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.kinesiologia.HorarioDisponibleDto;
import com.fktlomas.turnos.dto.kinesiologia.PlanKinesiologiaDto;
import com.fktlomas.turnos.dto.kinesiologia.SesionKinesiologiaDto;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.*;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KinesiologiaService {

    private static final String DOMINIO_EMAIL_SINTETICO = "@sin-email.fktlomas.local";
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final int MAX_SESIONES = 60;
    /** Tope de semanas a explorar buscando huecos libres antes de darse por vencido con lo que se pudo generar. */
    private static final int LIMITE_SEMANAS_BUSQUEDA = 78;

    private static final Map<DayOfWeek, String> DIA_EN_ESPANOL = Map.of(
            DayOfWeek.MONDAY, "lunes",
            DayOfWeek.TUESDAY, "martes",
            DayOfWeek.WEDNESDAY, "miércoles",
            DayOfWeek.THURSDAY, "jueves",
            DayOfWeek.FRIDAY, "viernes",
            DayOfWeek.SATURDAY, "sábado",
            DayOfWeek.SUNDAY, "domingo"
    );

    private final PlanKinesiologiaRepository planRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final ReglaDisponibilidadRepository reglaRepository;
    private final FeriadoService feriadoService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionTurnoService notificacionTurnoService;

    public static List<LocalTime> parsearHoras(List<String> horas) {
        try {
            return horas.stream().map(LocalTime::parse).toList();
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Los horarios deben tener el formato HH:mm");
        }
    }

    @Transactional(readOnly = true)
    public List<PlanKinesiologiaDto> misPlanes(Long pacienteId) {
        return planRepository.findByPacienteIdOrderByCreatedAtDesc(pacienteId).stream()
                .map(this::conSesiones).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanKinesiologiaDto> buscarAdmin(EstadoPlanKinesiologia estado) {
        return planRepository.buscar(estado).stream().map(this::conSesiones).toList();
    }

    /**
     * Horarios semanales reales que se pueden elegir para un plan: si se pasa medicoId, solo
     * los de ese kinesiólogo; si se omite (el paciente eligió "me da igual quién"), los de
     * todos los kinesiólogos activos juntos, para que arme el plan a partir de una lista real
     * en vez de un día/hora que el médico capaz no atiende.
     */
    @Transactional(readOnly = true)
    public List<HorarioDisponibleDto> horariosDisponibles(Long medicoId) {
        List<Medico> kinesiologos;
        if (medicoId != null) {
            Medico medico = medicoRepository.findById(medicoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
            if (!medico.getEspecialidad().isEsKinesiologia()) {
                throw new IllegalArgumentException("El médico elegido no es de kinesiología");
            }
            kinesiologos = List.of(medico);
        } else {
            kinesiologos = medicoRepository.findByActivoTrue().stream()
                    .filter(m -> m.getEspecialidad().isEsKinesiologia())
                    .toList();
        }

        List<HorarioDisponibleDto> resultado = new ArrayList<>();
        for (Medico medico : kinesiologos) {
            int duracion = medico.getDuracionTurnoMinutos();
            for (ReglaDisponibilidad regla : reglaRepository.findByMedicoId(medico.getId())) {
                LocalTime inicio = regla.getHoraInicio();
                while (inicio.plusMinutes(duracion).compareTo(regla.getHoraFin()) <= 0) {
                    resultado.add(new HorarioDisponibleDto(medico.getId(), medico.getNombreCompleto(), regla.getDiaSemana(), inicio));
                    inicio = inicio.plusMinutes(duracion);
                }
            }
        }
        resultado.sort(Comparator
                .comparingInt((HorarioDisponibleDto h) -> h.diaSemana().getValue())
                .thenComparing(HorarioDisponibleDto::horaInicio)
                .thenComparing(HorarioDisponibleDto::medicoNombre));
        return resultado;
    }

    private PlanKinesiologiaDto conSesiones(PlanKinesiologia plan) {
        List<SesionKinesiologiaDto> sesiones = turnoRepository.findByPlanKinesiologiaId(plan.getId())
                .stream().map(SesionKinesiologiaDto::de).toList();
        return PlanKinesiologiaDto.de(plan, sesiones);
    }

    @Transactional
    public PlanKinesiologiaDto crearParaPaciente(Usuario paciente, Long medicoId, int cantidadSesiones,
                                                  LocalDate fechaInicio, List<DayOfWeek> diasSemana, List<LocalTime> horasInicio,
                                                  MultipartFile ordenMedica) {
        return crear(paciente, medicoId, cantidadSesiones, fechaInicio, diasSemana, horasInicio, ordenMedica);
    }

    @Transactional
    public PlanKinesiologiaDto crearPorTelefono(String dni, String nombreCompleto, String telefono, String obraSocial, String email,
                                                 Long medicoId, int cantidadSesiones, LocalDate fechaInicio,
                                                 List<DayOfWeek> diasSemana, List<LocalTime> horasInicio, MultipartFile ordenMedica) {
        Usuario paciente = usuarioRepository.findByDni(dni).orElseGet(() -> crearPacienteTelefonico(dni, nombreCompleto, telefono, obraSocial, email));
        return crear(paciente, medicoId, cantidadSesiones, fechaInicio, diasSemana, horasInicio, ordenMedica);
    }

    private Usuario crearPacienteTelefonico(String dni, String nombreCompleto, String telefono, String obraSocial, String email) {
        String emailEfectivo = (email != null && !email.isBlank()) ? email : dni + DOMINIO_EMAIL_SINTETICO;
        if (usuarioRepository.existsByEmail(emailEfectivo)) {
            throw new ConflictoDeDatosException("Ya existe un paciente registrado con ese email");
        }
        Usuario paciente = new Usuario();
        paciente.setDni(dni);
        paciente.setNombreCompleto(nombreCompleto);
        paciente.setEmail(emailEfectivo);
        paciente.setTelefono(telefono);
        paciente.setObraSocial(obraSocial);
        paciente.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        paciente.setAceptaPoliticaPrivacidad(true);
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }

    private PlanKinesiologiaDto crear(Usuario paciente, Long medicoId, int cantidadSesiones, LocalDate fechaInicio,
                                       List<DayOfWeek> diasSemana, List<LocalTime> horasInicio, MultipartFile ordenMedica) {
        if (cantidadSesiones < 1 || cantidadSesiones > MAX_SESIONES) {
            throw new IllegalArgumentException("La cantidad de sesiones debe ser entre 1 y " + MAX_SESIONES);
        }
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a hoy");
        }
        if (diasSemana == null || diasSemana.isEmpty() || diasSemana.size() != horasInicio.size()) {
            throw new IllegalArgumentException("Cargá al menos un horario semanal fijo");
        }

        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        if (!medico.getEspecialidad().isEsKinesiologia()) {
            throw new IllegalArgumentException("El médico elegido no es de kinesiología");
        }

        List<ReglaDisponibilidad> reglas = reglaRepository.findByMedicoId(medicoId);
        for (int i = 0; i < diasSemana.size(); i++) {
            validarHorarioDentroDeDisponibilidad(reglas, diasSemana.get(i), horasInicio.get(i), medico.getDuracionTurnoMinutos());
        }

        ArchivoGuardado archivoGuardado = guardarOrdenMedica(ordenMedica);

        PlanKinesiologia plan = new PlanKinesiologia();
        plan.setPaciente(paciente);
        plan.setMedico(medico);
        plan.setCantidadSesiones(cantidadSesiones);
        plan.setFechaInicio(fechaInicio);
        plan.setOrdenMedicaPath(archivoGuardado.clave());
        plan.setOrdenMedicaContentType(archivoGuardado.contentType());

        for (int i = 0; i < diasSemana.size(); i++) {
            PlanKinesiologiaHorario horario = new PlanKinesiologiaHorario();
            horario.setPlan(plan);
            horario.setDiaSemana(diasSemana.get(i));
            horario.setHoraInicio(horasInicio.get(i));
            plan.getHorarios().add(horario);
        }

        return conSesiones(planRepository.save(plan));
    }

    private void validarHorarioDentroDeDisponibilidad(List<ReglaDisponibilidad> reglas, DayOfWeek dia, LocalTime horaInicio, int duracionMinutos) {
        LocalTime horaFin = horaInicio.plusMinutes(duracionMinutos);
        boolean encaja = reglas.stream().anyMatch(r ->
                r.getDiaSemana() == dia && !horaInicio.isBefore(r.getHoraInicio()) && !horaFin.isAfter(r.getHoraFin()));
        if (!encaja) {
            throw new IllegalArgumentException("El médico no atiende los " + DIA_EN_ESPANOL.get(dia) + " a las " + horaInicio + " según su horario cargado");
        }
    }

    private record ArchivoGuardado(String clave, String contentType) {
    }

    private ArchivoGuardado guardarOrdenMedica(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Adjuntá la orden médica (foto o PDF)");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoInvalidoException("El archivo no puede superar los 5MB");
        }
        TipoArchivo tipo = validarYExtension(archivo);
        try {
            String clave = fileStorageService.guardar("kinesiologia/ordenes-medicas", UUID.randomUUID() + tipo.extension(), archivo);
            return new ArchivoGuardado(clave, tipo.contentType());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la orden médica", e);
        }
    }

    private record TipoArchivo(String extension, String contentType) {
    }

    /**
     * El Content-Type del multipart lo declara el cliente y se puede falsificar — nunca se
     * persiste ni se sirve de vuelta. Acá se deriva la extensión Y el content-type real a
     * partir de la firma binaria del archivo, la única fuente confiable.
     */
    private TipoArchivo validarYExtension(MultipartFile archivo) {
        byte[] cabecera;
        try (var in = archivo.getInputStream()) {
            cabecera = in.readNBytes(8);
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo");
        }
        if (esPdf(cabecera)) return new TipoArchivo(".pdf", "application/pdf");
        if (esJpeg(cabecera)) return new TipoArchivo(".jpg", "image/jpeg");
        if (esPng(cabecera)) return new TipoArchivo(".png", "image/png");
        throw new ArchivoInvalidoException("La orden médica debe ser una imagen (JPG/PNG) o un PDF");
    }

    private boolean esPdf(byte[] c) {
        return c.length >= 5 && new String(c, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    private boolean esJpeg(byte[] c) {
        return c.length >= 2 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8;
    }

    private boolean esPng(byte[] c) {
        return c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G';
    }

    public Resource descargarOrdenMedica(Long planId) {
        PlanKinesiologia plan = obtenerEntidad(planId);
        return fileStorageService.cargar(plan.getOrdenMedicaPath())
                .orElseThrow(() -> new RecursoNoEncontradoException("El archivo ya no está disponible"));
    }

    public String contentTypeOrdenMedica(Long planId) {
        return obtenerEntidad(planId).getOrdenMedicaContentType();
    }

    public PlanKinesiologia obtenerEntidad(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado"));
    }

    /**
     * El admin revisa manualmente la orden médica y, si está en orden, valida el plan: recién
     * ahí se generan los turnos reales, uno por sesión, en los horarios fijos elegidos. Si un
     * horario cae en feriado o el slot ya está ocupado, esa semana se salta (no cuenta como
     * sesión) y se sigue buscando la próxima semana disponible.
     */
    @Transactional
    public PlanKinesiologiaDto validar(Long planId, Usuario admin) {
        PlanKinesiologia plan = obtenerEntidad(planId);
        if (plan.getEstado() != EstadoPlanKinesiologia.PENDIENTE_VALIDACION) {
            throw new ConflictoDeDatosException("Este plan ya fue procesado");
        }

        List<PlanKinesiologiaHorario> horarios = plan.getHorarios().stream()
                .sorted(Comparator.comparingInt((PlanKinesiologiaHorario h) -> h.getDiaSemana().getValue())
                        .thenComparing(PlanKinesiologiaHorario::getHoraInicio))
                .toList();

        LocalDate lunesInicial = plan.getFechaInicio().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Set<LocalDate> feriados = feriadoService.fechasEnRango(lunesInicial, lunesInicial.plusWeeks(LIMITE_SEMANAS_BUSQUEDA));

        Medico medico = plan.getMedico();
        int duracion = medico.getDuracionTurnoMinutos();
        int faltantes = plan.getCantidadSesiones() - plan.getSesionesGeneradas();
        List<Turno> nuevos = new ArrayList<>();

        for (int semana = 0; semana < LIMITE_SEMANAS_BUSQUEDA && faltantes > 0; semana++) {
            LocalDate inicioSemana = lunesInicial.plusWeeks(semana);
            for (PlanKinesiologiaHorario horario : horarios) {
                if (faltantes <= 0) break;
                LocalDate fecha = inicioSemana.with(TemporalAdjusters.nextOrSame(horario.getDiaSemana()));
                if (fecha.isBefore(plan.getFechaInicio())) continue;
                if (feriados.contains(fecha)) continue;
                if (turnoRepository.existsByMedicoIdAndFechaAndHoraInicio(medico.getId(), fecha, horario.getHoraInicio())) continue;

                Turno turno = new Turno();
                turno.setMedico(medico);
                turno.setFecha(fecha);
                turno.setHoraInicio(horario.getHoraInicio());
                turno.setHoraFin(horario.getHoraInicio().plusMinutes(duracion));
                turno.setEstado(EstadoTurno.RESERVADO);
                turno.setPaciente(plan.getPaciente());
                turno.setMotivoConsulta("Kinesiología — plan #" + plan.getId());
                turno.setPlanKinesiologia(plan);
                nuevos.add(turno);
                faltantes--;
            }
        }

        turnoRepository.saveAll(nuevos);
        plan.setSesionesGeneradas(plan.getSesionesGeneradas() + nuevos.size());
        plan.setEstado(EstadoPlanKinesiologia.VALIDADO);
        plan.setValidadoPor(admin);
        plan.setValidadoAt(Instant.now());
        PlanKinesiologia guardado = planRepository.save(plan);
        notificacionTurnoService.avisarPlanKinesiologiaValidado(guardado.getPaciente(), guardado.getSesionesGeneradas(), guardado.getCantidadSesiones());
        return conSesiones(guardado);
    }

    @Transactional
    public PlanKinesiologiaDto rechazar(Long planId, String motivo, Usuario admin) {
        PlanKinesiologia plan = obtenerEntidad(planId);
        if (plan.getEstado() != EstadoPlanKinesiologia.PENDIENTE_VALIDACION) {
            throw new ConflictoDeDatosException("Este plan ya fue procesado");
        }
        plan.setEstado(EstadoPlanKinesiologia.RECHAZADO);
        plan.setMotivoRechazo(motivo);
        plan.setValidadoPor(admin);
        plan.setValidadoAt(Instant.now());
        PlanKinesiologia guardado = planRepository.save(plan);
        notificacionTurnoService.avisarPlanKinesiologiaRechazado(guardado.getPaciente(), motivo);
        return conSesiones(guardado);
    }

    /** El paciente (o la secretaria en su nombre) cancela la serie: libera las sesiones futuras aún no atendidas. */
    @Transactional
    public PlanKinesiologiaDto cancelar(Long planId, Long pacienteIdSiPaciente) {
        PlanKinesiologia plan = obtenerYValidarPropiedad(planId, pacienteIdSiPaciente);
        liberarSesionesFuturas(plan);
        plan.setEstado(EstadoPlanKinesiologia.CANCELADO);
        return conSesiones(planRepository.save(plan));
    }

    @Transactional
    public PlanKinesiologiaDto pausar(Long planId, Long pacienteIdSiPaciente) {
        PlanKinesiologia plan = obtenerYValidarPropiedad(planId, pacienteIdSiPaciente);
        liberarSesionesFuturas(plan);
        plan.setEstado(EstadoPlanKinesiologia.PAUSADO);
        return conSesiones(planRepository.save(plan));
    }

    /** Reanuda un plan pausado generando las sesiones que falten a partir de una nueva fecha. */
    @Transactional
    public PlanKinesiologiaDto reanudar(Long planId, Long pacienteIdSiPaciente, LocalDate nuevaFechaInicio) {
        PlanKinesiologia plan = obtenerYValidarPropiedad(planId, pacienteIdSiPaciente);
        if (plan.getEstado() != EstadoPlanKinesiologia.PAUSADO) {
            throw new ConflictoDeDatosException("Solo se puede reanudar un plan pausado");
        }
        plan.setFechaInicio(nuevaFechaInicio.isBefore(LocalDate.now()) ? LocalDate.now() : nuevaFechaInicio);
        plan.setEstado(EstadoPlanKinesiologia.PENDIENTE_VALIDACION);
        planRepository.save(plan);
        return validar(planId, plan.getValidadoPor());
    }

    private PlanKinesiologia obtenerYValidarPropiedad(Long planId, Long pacienteIdSiPaciente) {
        PlanKinesiologia plan = obtenerEntidad(planId);
        if (pacienteIdSiPaciente != null && !plan.getPaciente().getId().equals(pacienteIdSiPaciente)) {
            throw new RecursoNoEncontradoException("Plan no encontrado");
        }
        return plan;
    }

    private void liberarSesionesFuturas(PlanKinesiologia plan) {
        List<Turno> futuros = turnoRepository.findByPlanKinesiologiaIdAndEstadoAndFechaGreaterThanEqual(
                plan.getId(), EstadoTurno.RESERVADO, LocalDate.now());
        for (Turno t : futuros) {
            t.setEstado(EstadoTurno.DISPONIBLE);
            t.setPaciente(null);
            t.setMotivoConsulta(null);
            t.setPlanKinesiologia(null);
        }
        turnoRepository.saveAll(futuros);
        // Esas sesiones liberadas nunca van a ocurrir — no cuentan más contra la cuota generada,
        // para que un eventual "reanudar" vuelva a generarlas en vez de darlas por hechas.
        plan.setSesionesGeneradas(plan.getSesionesGeneradas() - futuros.size());
    }
}
