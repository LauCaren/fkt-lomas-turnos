package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.caja.CrearIngresoRequest;
import com.fktlomas.turnos.dto.caja.CrearRetiroRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre el guard contra doble-submit agregado a CajaService: un doble click o un reintento de
 * red no debería poder cargar el mismo cobro o retiro dos veces, pero dos operaciones
 * legítimas y distintas que coinciden en monto sí tienen que poder registrarse.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:caja-duplicado-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class CajaDuplicadoTest {

    @Autowired
    private CajaService cajaService;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void dosIngresosIdenticosParaElMismoTurnoEnSegundosTiranConflicto() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medico = crearMedico("Dr. Duplicado Ingreso");
        Turno turno = crearTurno(medico);

        cajaService.registrarIngreso(new CrearIngresoRequest(medico.getId(), turno.getId(), new BigDecimal("500"), MetodoPago.EFECTIVO, null), secretaria);

        assertThatThrownBy(() -> cajaService.registrarIngreso(
                new CrearIngresoRequest(medico.getId(), turno.getId(), new BigDecimal("500"), MetodoPago.EFECTIVO, null), secretaria))
                .isInstanceOf(ConflictoDeDatosException.class);
    }

    @Test
    void dosIngresosParaTurnosDistintosConElMismoMontoNoSonDuplicados() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medico = crearMedico("Dr. Turnos Distintos");
        Turno turnoA = crearTurno(medico);
        Turno turnoB = crearTurno(medico);

        cajaService.registrarIngreso(new CrearIngresoRequest(medico.getId(), turnoA.getId(), new BigDecimal("500"), MetodoPago.EFECTIVO, null), secretaria);

        assertThat(cajaService.registrarIngreso(
                new CrearIngresoRequest(medico.getId(), turnoB.getId(), new BigDecimal("500"), MetodoPago.EFECTIVO, null), secretaria))
                .isNotNull();
    }

    @Test
    void dosIngresosSinTurnoAsociadoNuncaSeConsideranDuplicados() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medico = crearMedico("Dr. Sin Turno");

        cajaService.registrarIngreso(new CrearIngresoRequest(medico.getId(), null, new BigDecimal("500"), MetodoPago.EFECTIVO, null), secretaria);

        assertThat(cajaService.registrarIngreso(
                new CrearIngresoRequest(medico.getId(), null, new BigDecimal("500"), MetodoPago.EFECTIVO, null), secretaria))
                .isNotNull();
    }

    @Test
    void dosRetirosIdenticosParaElMismoMedicoEnSegundosTiranConflicto() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medico = crearMedico("Dr. Duplicado Retiro");
        cajaService.registrarIngreso(new CrearIngresoRequest(medico.getId(), null, new BigDecimal("1000"), MetodoPago.EFECTIVO, null), secretaria);

        cajaService.registrarRetiro(new CrearRetiroRequest(medico.getId(), new BigDecimal("300")), secretaria);

        assertThatThrownBy(() -> cajaService.registrarRetiro(new CrearRetiroRequest(medico.getId(), new BigDecimal("300")), secretaria))
                .isInstanceOf(ConflictoDeDatosException.class);
    }

    @Test
    void retirosDeMedicosDistintosConElMismoMontoNoSonDuplicados() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medicoA = crearMedico("Dr. Retiro A");
        Medico medicoB = crearMedico("Dr. Retiro B");
        cajaService.registrarIngreso(new CrearIngresoRequest(medicoA.getId(), null, new BigDecimal("1000"), MetodoPago.EFECTIVO, null), secretaria);
        cajaService.registrarIngreso(new CrearIngresoRequest(medicoB.getId(), null, new BigDecimal("1000"), MetodoPago.EFECTIVO, null), secretaria);

        cajaService.registrarRetiro(new CrearRetiroRequest(medicoA.getId(), new BigDecimal("300")), secretaria);

        assertThat(cajaService.registrarRetiro(new CrearRetiroRequest(medicoB.getId(), new BigDecimal("300")), secretaria))
                .isNotNull();
    }

    private Turno crearTurno(Medico medico) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now().plusDays(1));
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        return turnoRepository.save(turno);
    }

    private Medico crearMedico(String nombre) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(nombre + " especialidad " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto(nombre);
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medicoRepository.save(medico);
    }

    private Usuario crearUsuarioAdmin() {
        Usuario usuario = new Usuario();
        usuario.setEmail("secretaria-dup-" + System.nanoTime() + "@test.com");
        usuario.setNombreCompleto("Secretaria Test");
        usuario.setTelefono("1122334455");
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash("hash");
        usuario.setRole(Role.ADMIN);
        return usuarioRepository.save(usuario);
    }
}
