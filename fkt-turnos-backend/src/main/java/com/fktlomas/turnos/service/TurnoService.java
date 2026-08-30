package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final String DOMINIO_EMAIL_SINTETICO = "@sin-email.fktlomas.local";

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
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

    @Transactional(readOnly = true)
    public List<TurnoAdminDto> agenda(LocalDate desde, LocalDate hasta, Long medicoId, Long especialidadId) {
        return turnoRepository.findAgenda(desde, hasta, medicoId, especialidadId).stream().map(TurnoAdminDto::de).toList();
    }

    @Transactional
    public TurnoAdminDto reservarParaPaciente(Long turnoId, ReservarParaPacienteRequest request) {
        Usuario paciente = usuarioRepository.findByDni(request.dni())
                .orElseGet(() -> crearPacienteTelefonico(request));

        int filas = turnoRepository.reservarSiDisponible(turnoId, paciente, request.motivoConsulta());
        if (filas == 0) {
            throw new TurnoNoDisponibleException("Este turno ya no está disponible, elegí otro horario");
        }

        Turno actualizado = turnoRepository.findById(turnoId).orElseThrow();
        if (paciente.getEmail() != null && !paciente.getEmail().endsWith(DOMINIO_EMAIL_SINTETICO)) {
            emailService.enviarConfirmacionTurno(paciente, actualizado.getMedico().getNombreCompleto(),
                    actualizado.getMedico().getEspecialidad().getNombre(), actualizado.getFecha(),
                    actualizado.getHoraInicio(), actualizado.getHoraFin());
        }
        return TurnoAdminDto.de(actualizado);
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
