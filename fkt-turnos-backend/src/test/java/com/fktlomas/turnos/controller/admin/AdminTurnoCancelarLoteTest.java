package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.admin.CancelarLoteRequest;
import com.fktlomas.turnos.dto.admin.CancelarLoteResponse;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.TurnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cancelar varios turnos de una (AdminAgendaTab.jsx — ej. un médico avisa que no viene un día
 * entero). A propósito SIN @Transactional a nivel de clase: cada llamada a
 * cancelarLote pasa por AdminTurnoController real (bean con proxy de Spring), así cada
 * turnoService.cancelarComoStaff adentro del loop corre en su propia transacción de verdad —
 * exactamente lo que hace falta probar (que un turno inválido en el medio de la lista no arrastra
 * al resto a un rollback compartido). Con @Transactional acá, todo el test correría en una sola
 * transacción compartida con el controller y ese comportamiento no se podría distinguir.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-cancelar-lote-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AdminTurnoCancelarLoteTest {

    @Autowired
    private AdminTurnoController adminTurnoController;
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
    void cancelaLosValidosYReportaLosQueFallanSinAbortarElResto() {
        Medico medico = crearMedico();
        Usuario paciente = crearPaciente();
        Usuario staff = crearStaff();
        Turno turnoA = crearTurnoDisponible(medico, LocalTime.of(9, 0));
        Turno turnoB = crearTurnoDisponible(medico, LocalTime.of(10, 0));
        Turno turnoLibre = crearTurnoDisponible(medico, LocalTime.of(11, 0)); // nunca reservado

        turnoService.reservar(turnoA.getId(), paciente, new ReservarTurnoRequest(null));
        turnoService.reservar(turnoB.getId(), paciente, new ReservarTurnoRequest(null));

        CancelarLoteResponse resultado = adminTurnoController.cancelarLote(
                new AppUserDetails(staff),
                new CancelarLoteRequest(List.of(turnoA.getId(), turnoLibre.getId(), turnoB.getId(), 999999L))
        );

        assertThat(resultado.cancelados()).containsExactlyInAnyOrder(turnoA.getId(), turnoB.getId());
        assertThat(resultado.errores()).hasSize(2);
        assertThat(resultado.errores()).extracting(CancelarLoteResponse.ErrorCancelacion::turnoId)
                .containsExactlyInAnyOrder(turnoLibre.getId(), 999999L);

        assertThat(turnoRepository.findById(turnoA.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);
        assertThat(turnoRepository.findById(turnoB.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTurno.DISPONIBLE);
    }

    private Medico crearMedico() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad cancelar-lote test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Cancelar Lote Test");
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
        paciente.setEmail("paciente-cancelar-lote-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Cancelar Lote Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }

    private Usuario crearStaff() {
        Usuario staff = new Usuario();
        staff.setEmail("staff-cancelar-lote-" + System.nanoTime() + "@test.com");
        staff.setNombreCompleto("Secretaria Cancelar Lote Test");
        staff.setPasswordHash("hash");
        staff.setRole(Role.SECRETARIA);
        staff.setMfaHabilitado(true);
        return usuarioRepository.save(staff);
    }
}
