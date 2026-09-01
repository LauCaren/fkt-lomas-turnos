package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EvolucionClinica;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.medico.CrearEvolucionRequest;
import com.fktlomas.turnos.dto.medico.EvolucionClinicaDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.EvolucionClinicaRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista propia del médico logueado: su agenda de hoy, su cola de espera, y la historia
 * clínica de los pacientes que él mismo atendió. Todo queda siempre acotado a su propio
 * medicoId — un médico nunca ve turnos ni evoluciones de otro médico, ni bajo suplencia.
 */
@Service
@RequiredArgsConstructor
public class MedicoPortalService {

    private final MedicoRepository medicoRepository;
    private final TurnoRepository turnoRepository;
    private final EvolucionClinicaRepository evolucionClinicaRepository;

    @Transactional(readOnly = true)
    public Medico medicoDe(Long usuarioId) {
        return medicoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta cuenta no tiene un médico asociado"));
    }

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> agendaHoy(Long usuarioId) {
        Long medicoId = medicoDe(usuarioId).getId();
        LocalDate hoy = LocalDate.now();
        return conEvolucion(turnoRepository.findAgenda(hoy, hoy, medicoId, null));
    }

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> colaEspera(Long usuarioId) {
        Long medicoId = medicoDe(usuarioId).getId();
        return conEvolucion(turnoRepository.colaEspera(LocalDate.now(), medicoId));
    }

    private List<TurnoAdminDto> conEvolucion(List<Turno> turnos) {
        Set<Long> turnoIds = turnos.stream().map(Turno::getId).collect(Collectors.toSet());
        Set<Long> conEvolucion = turnoIds.isEmpty() ? Set.of() : evolucionClinicaRepository.findByTurnoIdIn(turnoIds)
                .stream().map(e -> e.getTurno().getId()).collect(Collectors.toSet());
        return turnos.stream().map(t -> TurnoAdminDto.de(t, conEvolucion.contains(t.getId()))).toList();
    }

    @Transactional
    public void marcarAtendido(Long usuarioId, Long turnoId) {
        Long medicoId = medicoDe(usuarioId).getId();
        int filas = turnoRepository.marcarAtendidoSiLlego(turnoId, medicoId);
        if (filas == 0) {
            throw new RecursoNoEncontradoException("El turno no está en tu cola de espera");
        }
    }

    @Transactional
    public EvolucionClinicaDto crearEvolucion(Long usuarioId, Long turnoId, CrearEvolucionRequest request) {
        Medico medico = medicoDe(usuarioId);
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (!turno.getMedico().getId().equals(medico.getId()) || turno.getPaciente() == null) {
            throw new RecursoNoEncontradoException("Turno no encontrado");
        }
        if (evolucionClinicaRepository.existsByTurnoId(turnoId)) {
            throw new com.fktlomas.turnos.exception.ConflictoDeDatosException("Ya cargaste una evolución para este turno");
        }
        EvolucionClinica evolucion = new EvolucionClinica();
        evolucion.setPaciente(turno.getPaciente());
        evolucion.setMedico(medico);
        evolucion.setTurno(turno);
        evolucion.setTexto(request.texto());
        return EvolucionClinicaDto.de(evolucionClinicaRepository.save(evolucion));
    }

    @Transactional(readOnly = true)
    public List<EvolucionClinicaDto> evolucionesDePaciente(Long usuarioId, Long pacienteId) {
        Long medicoId = medicoDe(usuarioId).getId();
        return evolucionClinicaRepository.findByMedicoIdAndPacienteIdOrderByCreatedAtDesc(medicoId, pacienteId)
                .stream().map(EvolucionClinicaDto::de).toList();
    }
}
