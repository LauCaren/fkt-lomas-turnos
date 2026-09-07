package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.CheckInDto;
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

/**
 * Cubre el check-in por QR de sala de espera (ver TurnoService.checkInPorQr): sin turno para
 * hoy, ya registrado, y el caso de copago de IOMA que obliga a pasar por el mostrador en vez
 * de autoregistrarse. Ninguna de estas ramas tenía test.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:checkin-qr-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class CheckInPorQrTest {

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
    void sinTurnoHoyDevuelveSinTurnoHoy() {
        Usuario paciente = crearPaciente("Particular");

        CheckInDto resultado = turnoService.checkInPorQr(paciente);

        assertThat(resultado.resultado()).isEqualTo(CheckInDto.Resultado.SIN_TURNO_HOY);
    }

    @Test
    void conTurnoPendienteHoyMarcaLlegoYActualizaEstadoAsistencia() {
        Medico medico = crearMedico(false);
        Usuario paciente = crearPaciente("Particular");
        Turno turno = reservarTurnoHoy(medico, paciente);

        CheckInDto resultado = turnoService.checkInPorQr(paciente);

        assertThat(resultado.resultado()).isEqualTo(CheckInDto.Resultado.LLEGO);
        Turno actualizado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(actualizado.getEstadoAsistencia()).isEqualTo(EstadoAsistencia.LLEGO);
    }

    @Test
    void siYaHizoCheckInHoyElSegundoIntentoDevuelveYaRegistrado() {
        Medico medico = crearMedico(false);
        Usuario paciente = crearPaciente("Particular");
        reservarTurnoHoy(medico, paciente);

        turnoService.checkInPorQr(paciente);
        CheckInDto segundoIntento = turnoService.checkInPorQr(paciente);

        assertThat(segundoIntento.resultado()).isEqualTo(CheckInDto.Resultado.YA_REGISTRADO);
    }

    @Test
    void pacienteIomaConMedicoQueCobraCopagoRequiereMostradorYNoAutoregistra() {
        Medico medico = crearMedico(true);
        Usuario paciente = crearPaciente("IOMA");
        Turno turno = reservarTurnoHoy(medico, paciente);

        CheckInDto resultado = turnoService.checkInPorQr(paciente);

        assertThat(resultado.resultado()).isEqualTo(CheckInDto.Resultado.REQUIERE_MOSTRADOR);
        Turno sinTocar = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(sinTocar.getEstadoAsistencia()).isEqualTo(EstadoAsistencia.PENDIENTE);
    }

    @Test
    void pacienteIomaConMedicoQueNoCobraCopagoSeAutoregistraNormalmente() {
        Medico medico = crearMedico(false);
        Usuario paciente = crearPaciente("IOMA");
        reservarTurnoHoy(medico, paciente);

        CheckInDto resultado = turnoService.checkInPorQr(paciente);

        assertThat(resultado.resultado()).isEqualTo(CheckInDto.Resultado.LLEGO);
    }

    private Turno reservarTurnoHoy(Medico medico, Usuario paciente) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now());
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        turno.setEstado(EstadoTurno.RESERVADO);
        turno.setPaciente(paciente);
        return turnoRepository.save(turno);
    }

    private Medico crearMedico(boolean cobraCopagoIoma) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad checkin test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Checkin Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        medico.setCobraCopagoIoma(cobraCopagoIoma);
        return medicoRepository.save(medico);
    }

    private Usuario crearPaciente(String obraSocial) {
        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-checkin-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Checkin Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial(obraSocial);
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
