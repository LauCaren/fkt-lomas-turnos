package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un turno de HOY generado a una hora que ya pasó (DisponibilidadService genera los del día
 * completo sin importar qué hora es "ahora") sigue quedando DISPONIBLE en la base — no debería
 * ni ofrecerse ni poder reservarse. Ver TurnoService.yaPaso.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:horario-pasado-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TurnoServiceHorarioPasadoTest {

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
    void disponiblesDeMedicoExcluyeHorariosDeHoyQueYaPasaron() {
        Medico medico = crearMedico();
        Turno pasado = crearTurno(medico, LocalDate.now(), LocalTime.MIN);
        Turno futuro = crearTurno(medico, LocalDate.now(), LocalTime.of(23, 59));

        List<TurnoDto> disponibles = turnoService.disponiblesDeMedico(medico.getId(), LocalDate.now());

        assertThat(disponibles).extracting(TurnoDto::id).contains(futuro.getId()).doesNotContain(pasado.getId());
    }

    @Test
    void noSePuedeReservarUnTurnoDeHoyConHorarioYaPasado() {
        Medico medico = crearMedico();
        Turno pasado = crearTurno(medico, LocalDate.now(), LocalTime.MIN);
        Usuario paciente = crearPaciente();

        assertThatThrownBy(() -> turnoService.reservar(pasado.getId(), paciente, new ReservarTurnoRequest("Control")))
                .isInstanceOf(TurnoNoDisponibleException.class);

        Turno sinTocar = turnoRepository.findById(pasado.getId()).orElseThrow();
        assertThat(sinTocar.getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);
        assertThat(sinTocar.getPaciente()).isNull();
    }

    @Test
    void noSePuedeReservarParaUnPacienteUnTurnoDeHoyConHorarioYaPasadoNiSeCreaUnPacienteNuevo() {
        Medico medico = crearMedico();
        Turno pasado = crearTurno(medico, LocalDate.now(), LocalTime.MIN);
        String dniInexistente = "9" + System.nanoTime() % 1000000;
        ReservarParaPacienteRequest request = new ReservarParaPacienteRequest(
                dniInexistente, "Paciente Telefonico Test", "1122334455", "Particular", null, "Control");
        Usuario secretaria = crearPaciente(); // el actor de auditoría no necesita ser staff real para este test

        assertThatThrownBy(() -> turnoService.reservarParaPaciente(pasado.getId(), request, secretaria))
                .isInstanceOf(TurnoNoDisponibleException.class);

        Turno sinTocar = turnoRepository.findById(pasado.getId()).orElseThrow();
        assertThat(sinTocar.getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);
        assertThat(usuarioRepository.existsByDni(dniInexistente)).isFalse();
    }

    private Medico crearMedico() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad horario pasado test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Horario Pasado Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medicoRepository.save(medico);
    }

    private Turno crearTurno(Medico medico, LocalDate fecha, LocalTime horaInicio) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(fecha);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFin(horaInicio.equals(LocalTime.MIN) ? LocalTime.of(0, 30) : LocalTime.MAX);
        return turnoRepository.save(turno);
    }

    private Usuario crearPaciente() {
        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-horario-pasado-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Horario Pasado Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
