package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
import com.fktlomas.turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<TurnoDto> disponiblesDeMedico(Long medicoId, LocalDate fecha) {
        return turnoRepository.findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(medicoId, fecha, EstadoTurno.DISPONIBLE)
                .stream().map(TurnoDto::de).toList();
    }

    @Transactional
    public TurnoDto reservar(Long turnoId, Usuario paciente, ReservarTurnoRequest request) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        int filas = turnoRepository.reservarSiDisponible(turnoId, paciente, request.motivoConsulta());
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }

        Turno actualizado = turnoRepository.findById(turnoId).orElseThrow();
        emailService.enviarConfirmacionTurno(paciente, actualizado.getMedico().getNombreCompleto(),
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

        turno.setEstado(EstadoTurno.DISPONIBLE);
        turno.setPaciente(null);
        turno.setMotivoConsulta(null);
        turnoRepository.save(turno);
    }
}
