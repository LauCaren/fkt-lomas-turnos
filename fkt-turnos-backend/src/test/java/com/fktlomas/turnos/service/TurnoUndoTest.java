package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre el undo acotado a turnos/agenda (punto 11 del pedido original): "deshacer" tiene que
 * devolver el turno al estado justo antes de la última operación registrada en auditoría, y
 * deshacer el "deshacer" en sí mismo debe actuar como un redo — es una propiedad natural de
 * apoyarse en el historial de auditoría en vez de un campo ad hoc, vale la pena confirmar que
 * de verdad se comporta así.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:turno-undo-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TurnoUndoTest {

    @Autowired
    private TurnoService turnoService;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void deshacerUnaCancelacionVuelveAReservarConElMismoPaciente() {
        Usuario paciente = crearPaciente();
        Turno turno = crearTurnoDisponible();

        turnoService.reservar(turno.getId(), paciente, new ReservarTurnoRequest("Control"));
        turnoService.cancelar(turno.getId(), paciente);

        Turno canceladoDeVuelta = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(canceladoDeVuelta.getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);

        TurnoAdminDto deshecho = turnoService.deshacerUltimaOperacion(turno.getId(), paciente);

        assertThat(deshecho.estado()).isEqualTo("RESERVADO");
        assertThat(deshecho.pacienteId()).isEqualTo(paciente.getId());
        assertThat(deshecho.motivoConsulta()).isEqualTo("Control");
    }

    @Test
    void deshacerElDeshacerActuaComoUnRedo() {
        Usuario paciente = crearPaciente();
        Turno turno = crearTurnoDisponible();

        turnoService.reservar(turno.getId(), paciente, new ReservarTurnoRequest("Control"));
        turnoService.cancelar(turno.getId(), paciente);
        turnoService.deshacerUltimaOperacion(turno.getId(), paciente);

        TurnoAdminDto redo = turnoService.deshacerUltimaOperacion(turno.getId(), paciente);

        assertThat(redo.estado()).isEqualTo("DISPONIBLE");
        assertThat(redo.pacienteId()).isNull();
    }

    @Test
    void deshacerSinHistorialTiraRecursoNoEncontrado() {
        Turno turno = crearTurnoDisponible();

        assertThatThrownBy(() -> turnoService.deshacerUltimaOperacion(turno.getId(), crearPaciente()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Turno crearTurnoDisponible() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad undo test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Undo Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        medico = medicoRepository.save(medico);

        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now().plusDays(1));
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        return turnoRepository.save(turno);
    }

    private Usuario crearPaciente() {
        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-undo-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Undo Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
