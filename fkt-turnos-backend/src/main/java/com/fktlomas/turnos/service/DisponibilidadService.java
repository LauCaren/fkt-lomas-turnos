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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private static final int MAX_DIAS_GENERACION = 90;

    private final ReglaDisponibilidadRepository reglaRepository;
    private final TurnoRepository turnoRepository;
    private final MedicoService medicoService;

    @Transactional(readOnly = true)
    public List<ReglaDisponibilidadDto> listarReglas(Long medicoId) {
        return reglaRepository.findByMedicoId(medicoId).stream().map(ReglaDisponibilidadDto::de).toList();
    }

    public ReglaDisponibilidadDto crearRegla(Long medicoId, ReglaDisponibilidadRequest request) {
        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
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

        List<Turno> nuevos = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
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
        return nuevos.size();
    }
}
