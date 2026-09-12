package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.admin.ReprogramarTurnoResponse;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.RecordatorioRepository;
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
 * Reprogramar (punto 5 del pedido de UX): toma el destino de forma atómica, libera el origen sin
 * avisar una "cancelación", limpia el recordatorio del origen (ya apuntaría a un slot vacío), y
 * queda auditado como dos operaciones separadas para que el "deshacer" genérico funcione sobre
 * cada turno sin código nuevo. También cubre cancelarComoStaff, la cancelación de un turno
 * reservado hecha por secretaría/admin (no existía — solo estaba la del propio paciente logueado).
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:turno-reprogramar-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TurnoReprogramarTest {

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
    @Autowired
    private RecordatorioRepository recordatorioRepository;

    @Test
    void reprogramarMuevePacienteAlDestinoYLiberaElOrigen() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Turno origen = crearTurnoDisponible(medico, LocalTime.of(10, 0));
        Turno destino = crearTurnoDisponible(medico, LocalTime.of(11, 0));

        turnoService.reservar(origen.getId(), paciente, new ReservarTurnoRequest("Control"));

        ReprogramarTurnoResponse resultado = turnoService.reprogramar(origen.getId(), destino.getId(), paciente);

        assertThat(resultado.origen().estado()).isEqualTo("DISPONIBLE");
        assertThat(resultado.origen().pacienteId()).isNull();
        assertThat(resultado.destino().estado()).isEqualTo("RESERVADO");
        assertThat(resultado.destino().pacienteId()).isEqualTo(paciente.getId());
        assertThat(resultado.destino().motivoConsulta()).isEqualTo("Control");
    }

    @Test
    void reprogramarADestinoYaTomadoTiraTurnoNoDisponible() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Usuario otroPaciente = crearPaciente();
        Turno origen = crearTurnoDisponible(medico, LocalTime.of(10, 0));
        Turno destino = crearTurnoDisponible(medico, LocalTime.of(11, 0));

        turnoService.reservar(origen.getId(), paciente, new ReservarTurnoRequest("Control"));
        turnoService.reservar(destino.getId(), otroPaciente, new ReservarTurnoRequest(null));

        assertThatThrownBy(() -> turnoService.reprogramar(origen.getId(), destino.getId(), paciente))
                .isInstanceOf(TurnoNoDisponibleException.class);

        Turno origenSinCambios = turnoRepository.findById(origen.getId()).orElseThrow();
        assertThat(origenSinCambios.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    void reprogramarBorraElRecordatorioDelOrigen() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Turno origen = crearTurnoDisponible(medico, LocalTime.of(10, 0));
        Turno destino = crearTurnoDisponible(medico, LocalTime.of(11, 0));

        turnoService.reservar(origen.getId(), paciente, new ReservarTurnoRequest("Control"));
        recordatorioRepository.save(new Recordatorio(origen, CanalNotificacion.EMAIL));
        assertThat(recordatorioRepository.findByTurnoId(origen.getId())).isPresent();

        turnoService.reprogramar(origen.getId(), destino.getId(), paciente);

        assertThat(recordatorioRepository.findByTurnoId(origen.getId())).isEmpty();
    }

    @Test
    void deshacerRevierteSoloElTurnoIndicado() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Turno origen = crearTurnoDisponible(medico, LocalTime.of(10, 0));
        Turno destino = crearTurnoDisponible(medico, LocalTime.of(11, 0));

        turnoService.reservar(origen.getId(), paciente, new ReservarTurnoRequest("Control"));
        turnoService.reprogramar(origen.getId(), destino.getId(), paciente);

        turnoService.deshacerUltimaOperacion(destino.getId(), paciente);
        Turno destinoDeshecho = turnoRepository.findById(destino.getId()).orElseThrow();
        assertThat(destinoDeshecho.getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);

        turnoService.deshacerUltimaOperacion(origen.getId(), paciente);
        Turno origenDeshecho = turnoRepository.findById(origen.getId()).orElseThrow();
        assertThat(origenDeshecho.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(origenDeshecho.getPaciente().getId()).isEqualTo(paciente.getId());
    }

    @Test
    void reprogramarUnTurnoNoReservadoTiraTurnoNoDisponible() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Turno origenLibre = crearTurnoDisponible(medico, LocalTime.of(10, 0));
        Turno destino = crearTurnoDisponible(medico, LocalTime.of(11, 0));

        assertThatThrownBy(() -> turnoService.reprogramar(origenLibre.getId(), destino.getId(), paciente))
                .isInstanceOf(TurnoNoDisponibleException.class);
    }

    @Test
    void cancelarComoStaffLiberaElTurnoYAvisaAlPaciente() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Usuario staff = crearPaciente();
        Turno turno = crearTurnoDisponible(medico, LocalTime.of(10, 0));

        turnoService.reservar(turno.getId(), paciente, new ReservarTurnoRequest("Control"));

        var resultado = turnoService.cancelarComoStaff(turno.getId(), staff);

        assertThat(resultado.estado()).isEqualTo("DISPONIBLE");
        assertThat(resultado.pacienteId()).isNull();
    }

    @Test
    void cancelarComoStaffUnTurnoNoReservadoTiraTurnoNoDisponible() {
        Medico medico = crearMedico();
        Usuario staff = crearPaciente();
        Turno turnoLibre = crearTurnoDisponible(medico, LocalTime.of(10, 0));

        assertThatThrownBy(() -> turnoService.cancelarComoStaff(turnoLibre.getId(), staff))
                .isInstanceOf(TurnoNoDisponibleException.class);
    }

    @Test
    void asignarTurnoAPacienteLoReservaParaEsePaciente() {
        Medico medico = crearMedico();
        Usuario pacienteEnEspera = crearPaciente();
        Usuario staff = crearPaciente();
        Turno turnoLibre = crearTurnoDisponible(medico, LocalTime.of(10, 0));

        var resultado = turnoService.asignarTurnoAPaciente(turnoLibre.getId(), pacienteEnEspera.getId(), staff);

        assertThat(resultado.estado()).isEqualTo("RESERVADO");
        assertThat(resultado.pacienteId()).isEqualTo(pacienteEnEspera.getId());
    }

    @Test
    void asignarTurnoYaTomadoTiraTurnoNoDisponible() {
        Medico medico = crearMedico();
        Usuario yaReservo = crearPaciente();
        Usuario pacienteEnEspera = crearPaciente();
        Usuario staff = crearPaciente();
        Turno turno = crearTurnoDisponible(medico, LocalTime.of(10, 0));

        turnoService.reservar(turno.getId(), yaReservo, new ReservarTurnoRequest(null));

        assertThatThrownBy(() -> turnoService.asignarTurnoAPaciente(turno.getId(), pacienteEnEspera.getId(), staff))
                .isInstanceOf(TurnoNoDisponibleException.class);
    }

    @Test
    void reprogramarConOrigenInexistenteTiraRecursoNoEncontrado() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Turno destino = crearTurnoDisponible(medico, LocalTime.of(11, 0));

        assertThatThrownBy(() -> turnoService.reprogramar(999999L, destino.getId(), paciente))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Medico crearMedico() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad reprogramar test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Reprogramar Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medicoRepository.save(medico);
    }

    private Turno crearTurnoDisponible(Medico medico, LocalTime horaInicio) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now().plusDays(1));
        turno.setHoraInicio(horaInicio);
        turno.setHoraFin(horaInicio.plusMinutes(30));
        return turnoRepository.save(turno);
    }

    private Usuario crearPaciente() {
        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-reprogramar-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Reprogramar Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
