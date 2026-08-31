package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoListaEspera;
import com.fktlomas.turnos.domain.ListaEspera;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.ListaEsperaDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.ListaEsperaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lista de espera para cuando no hay turnos disponibles: el paciente se anota
 * para un médico/día puntual y, si se libera un horario ahí (cancelación o
 * generación de turnos nuevos), se le avisa por su canal preferido. No
 * reserva nada automáticamente — es "primero que llega, primero que reserva"
 * igual que cualquier otro turno liberado, así que puede avisarle a varios
 * pacientes anotados para el mismo día.
 */
@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;
    private final MedicoService medicoService;
    private final NotificacionTurnoService notificacionTurnoService;

    @Transactional
    public ListaEsperaDto anotarse(Usuario paciente, Long medicoId, LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede anotar en lista de espera para una fecha pasada");
        }
        listaEsperaRepository.findByPacienteIdAndMedicoIdAndFechaAndEstado(paciente.getId(), medicoId, fecha, EstadoListaEspera.ACTIVA)
                .ifPresent(le -> {
                    throw new ConflictoDeDatosException("Ya estás anotado en la lista de espera para ese día");
                });

        Medico medico = medicoService.obtenerEntidad(medicoId);
        ListaEspera le = new ListaEspera();
        le.setPaciente(paciente);
        le.setMedico(medico);
        le.setFecha(fecha);
        return ListaEsperaDto.de(listaEsperaRepository.save(le));
    }

    @Transactional(readOnly = true)
    public Optional<ListaEsperaDto> miAnotacion(Long pacienteId, Long medicoId, LocalDate fecha) {
        return listaEsperaRepository
                .findByPacienteIdAndMedicoIdAndFechaAndEstado(pacienteId, medicoId, fecha, EstadoListaEspera.ACTIVA)
                .map(ListaEsperaDto::de);
    }

    @Transactional
    public void darseDeBaja(Long id, Usuario paciente) {
        ListaEspera le = listaEsperaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró esa anotación"));
        if (!le.getPaciente().getId().equals(paciente.getId())) {
            throw new RecursoNoEncontradoException("No se encontró esa anotación");
        }
        listaEsperaRepository.delete(le);
    }

    /**
     * Se llama cada vez que un turno de un médico/día queda DISPONIBLE de
     * nuevo (cancelación) o se generan turnos nuevos ahí. Best-effort: si el
     * envío falla, ya queda logueado adentro de NotificacionTurnoService y no
     * vuelve a intentarse — el estado ya pasó a NOTIFICADA para no repetir el
     * aviso en cada cancelación posterior del mismo día.
     */
    @Transactional
    public void notificarLiberacion(Long medicoId, LocalDate fecha) {
        List<ListaEspera> anotados = listaEsperaRepository.findByMedicoIdAndFechaAndEstado(medicoId, fecha, EstadoListaEspera.ACTIVA);
        for (ListaEspera le : anotados) {
            le.setEstado(EstadoListaEspera.NOTIFICADA);
            le.setNotificadoAt(Instant.now());
            listaEsperaRepository.save(le);
            notificacionTurnoService.avisarTurnoLiberado(le.getPaciente(), le.getMedico().getNombreCompleto(),
                    le.getMedico().getEspecialidad().getNombre(), fecha);
        }
    }
}
