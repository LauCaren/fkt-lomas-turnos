package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirmación de asistencia del día anterior (link sin login, ver ConfirmacionTurnoController):
 * confirmar solo marca el estado; rechazar tiene que liberar el turno igual que una
 * cancelación común, para que otro paciente pueda tomarlo.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:confirmacion-previa-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ConfirmacionPreviaTest {

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
    void confirmarPorTokenMarcaConfirmadoSinTocarElEstadoDelTurno() {
        Turno turno = reservarTurnoConToken();

        turnoService.confirmarPorToken(turno.getTokenConfirmacion(), true);

        Turno actualizado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(actualizado.getConfirmacionPrevia()).isEqualTo(ConfirmacionPrevia.CONFIRMADO);
        assertThat(actualizado.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(actualizado.getPaciente()).isNotNull();
    }

    @Test
    void rechazarPorTokenLiberaElTurnoComoUnaCancelacion() {
        Turno turno = reservarTurnoConToken();

        turnoService.confirmarPorToken(turno.getTokenConfirmacion(), false);

        Turno actualizado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(actualizado.getConfirmacionPrevia()).isEqualTo(ConfirmacionPrevia.RECHAZADO);
        assertThat(actualizado.getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);
        assertThat(actualizado.getPaciente()).isNull();
    }

    @Test
    void elRechazoQuedaVisibleMientrasElTurnoSigueLibrePeroSeLimpiaAlReservarloOtroPaciente() {
        Turno turno = reservarTurnoConToken();
        turnoService.confirmarPorToken(turno.getTokenConfirmacion(), false);

        Turno liberado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(liberado.getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);
        assertThat(liberado.getConfirmacionPrevia()).isEqualTo(ConfirmacionPrevia.RECHAZADO);

        Usuario otroPaciente = new Usuario();
        otroPaciente.setEmail("otro-paciente-" + System.nanoTime() + "@test.com");
        otroPaciente.setNombreCompleto("Otro Paciente Test");
        otroPaciente.setTelefono("1133445566");
        otroPaciente.setGenero(Genero.PREFIERO_NO_DECIR);
        otroPaciente.setObraSocial("Particular");
        otroPaciente.setPasswordHash("hash");
        otroPaciente.setRole(Role.PACIENTE);
        otroPaciente = usuarioRepository.save(otroPaciente);

        turnoService.reservar(turno.getId(), otroPaciente, new ReservarTurnoRequest("Otra consulta"));

        Turno reReservado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(reReservado.getConfirmacionPrevia()).isEqualTo(ConfirmacionPrevia.SIN_RESPUESTA);
        assertThat(reReservado.getTokenConfirmacion()).isNull();
        assertThat(reReservado.getPaciente().getId()).isEqualTo(otroPaciente.getId());
    }

    @Test
    void unTokenInvalidoTiraRecursoNoEncontrado() {
        assertThatThrownBy(() -> turnoService.confirmarPorToken("token-inexistente", true))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Turno reservarTurnoConToken() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad confirmacion test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Confirmacion Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        medico = medicoRepository.save(medico);

        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now().plusDays(1));
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        turno = turnoRepository.save(turno);

        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-confirmacion-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Confirmacion Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        paciente = usuarioRepository.save(paciente);

        turnoService.reservar(turno.getId(), paciente, new ReservarTurnoRequest("Control"));

        Turno reservado = turnoRepository.findById(turno.getId()).orElseThrow();
        reservado.setTokenConfirmacion(UUID.randomUUID().toString());
        return turnoRepository.save(reservado);
    }
}
