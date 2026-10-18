package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.ReglaDisponibilidad;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.dto.admin.ReglaDisponibilidadDto;
import com.fktlomas.turnos.dto.admin.ReglaDisponibilidadRequest;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.ReglaDisponibilidadRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Horarios de atención de cada médico (ReglaDisponibilidad: "los martes de 10 a 14") y la
 * generación de los turnos concretos a partir de ellos.
 *
 * <p>Los turnos NO se crean solos: el admin elige un rango de fechas (hasta 90 días) y el sistema
 * corta cada franja horaria en turnos de la duración del médico, salteando feriados y los turnos
 * que ya existan (se puede volver a generar el mismo rango sin duplicar). Es a propósito: así el
 * centro controla hasta cuándo abre la agenda (vacaciones, licencias, cambios de horario). Si no
 * se generan turnos, los pacientes no ven horarios para reservar.
 *
 * <p>Al crear turnos nuevos se avisa a quienes estaban en lista de espera de esos días.
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private static final int MAX_DIAS_GENERACION = 90;
    private static final LocalTime HORA_MINIMA = LocalTime.of(8, 0);
    private static final LocalTime HORA_MAXIMA = LocalTime.of(20, 0);
    private static final Set<DayOfWeek> DIAS_HABILES = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private final ReglaDisponibilidadRepository reglaRepository;
    private final TurnoRepository turnoRepository;
    private final MedicoService medicoService;
    private final FeriadoService feriadoService;
    private final ListaEsperaService listaEsperaService;

    @Transactional(readOnly = true)
    public List<ReglaDisponibilidadDto> listarReglas(Long medicoId) {
        return reglaRepository.findByMedicoId(medicoId).stream().map(ReglaDisponibilidadDto::de).toList();
    }

    public ReglaDisponibilidadDto crearRegla(Long medicoId, ReglaDisponibilidadRequest request) {
        if (!DIAS_HABILES.contains(request.diaSemana())) {
            throw new IllegalArgumentException("Solo se pueden cargar horarios de lunes a viernes");
        }
        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (request.horaInicio().isBefore(HORA_MINIMA) || request.horaFin().isAfter(HORA_MAXIMA)) {
            throw new IllegalArgumentException("El horario debe estar entre las 08:00 y las 20:00");
        }
        Medico medico = medicoService.obtenerEntidad(medicoId);
        ReglaDisponibilidad regla = new ReglaDisponibilidad();
        regla.setMedico(medico);
        regla.setDiaSemana(request.diaSemana());
        regla.setHoraInicio(request.horaInicio());
        regla.setHoraFin(request.horaFin());
        return ReglaDisponibilidadDto.de(reglaRepository.save(regla));
    }

    public void eliminarRegla(Long reglaId) {
        if (!reglaRepository.existsById(reglaId)) {
            throw new RecursoNoEncontradoException("Regla de disponibilidad no encontrada");
        }
        reglaRepository.deleteById(reglaId);
    }

    @Transactional
    public int generarTurnos(Long medicoId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha 'hasta' debe ser posterior a 'desde'");
        }
        if (desde.plusDays(MAX_DIAS_GENERACION).isBefore(hasta)) {
            throw new IllegalArgumentException("El rango no puede superar los " + MAX_DIAS_GENERACION + " días");
        }

        Medico medico = medicoService.obtenerEntidad(medicoId);
        List<ReglaDisponibilidad> reglas = reglaRepository.findByMedicoId(medicoId);
        int duracion = medico.getDuracionTurnoMinutos();
        Set<LocalDate> feriados = feriadoService.fechasEnRango(desde, hasta);

        List<Turno> nuevos = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (feriados.contains(fecha)) {
                continue;
            }
            for (ReglaDisponibilidad regla : reglas) {
                if (regla.getDiaSemana() != fecha.getDayOfWeek()) {
                    continue;
                }
                LocalTime inicio = regla.getHoraInicio();
                while (inicio.plusMinutes(duracion).compareTo(regla.getHoraFin()) <= 0) {
                    LocalTime fin = inicio.plusMinutes(duracion);
                    if (!turnoRepository.existsByMedicoIdAndFechaAndHoraInicio(medicoId, fecha, inicio)) {
                        Turno turno = new Turno();
                        turno.setMedico(medico);
                        turno.setFecha(fecha);
                        turno.setHoraInicio(inicio);
                        turno.setHoraFin(fin);
                        nuevos.add(turno);
                    }
                    inicio = fin;
                }
            }
        }

        turnoRepository.saveAll(nuevos);

        nuevos.stream().map(Turno::getFecha).distinct()
                .forEach(fecha -> listaEsperaService.notificarLiberacion(medicoId, fecha));

        return nuevos.size();
    }
}
