package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.admin.FeriadoRequest;
import com.fktlomas.turnos.dto.kinesiologia.PlanKinesiologiaDto;
import com.fktlomas.turnos.dto.kinesiologia.SesionKinesiologiaDto;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.ReglaDisponibilidadRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre la parte más delicada de kinesiología: la generación de sesiones al validar un plan
 * (KinesiologiaService.validar) tiene que saltear semanas con feriado o con el horario ya
 * ocupado SIN contarlas contra la cuota, y seguir buscando hasta completar cantidadSesiones —
 * la regla de negocio que Lautaro confirmó explícitamente.
 *
 * {@code @Transactional} evita que un feriado u otro dato creado en un test quede en la
 * base H2 compartida (el contexto de Spring se cachea entre tests de la clase) y contamine
 * los demás — cada test hace rollback al terminar.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kinesiologia-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.uploads.dir=target/test-uploads-kinesiologia"
})
class KinesiologiaServiceTest {

    @Autowired
    private KinesiologiaService kinesiologiaService;
    @Autowired
    private FeriadoService feriadoService;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private ReglaDisponibilidadRepository reglaDisponibilidadRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TurnoRepository turnoRepository;

    private static final LocalTime HORA = LocalTime.of(10, 0);

    @Test
    void unFeriadoEnMedioSeSalteaYNoCuentaContraLaCuota() {
        Medico kinesiologo = crearKinesiologoConReglaLunes();
        Usuario paciente = crearPaciente("paciente-feriado@test.com", "30111111");
        LocalDate primerLunes = proximoLunes();

        feriadoService.crear(new FeriadoRequest(primerLunes.plusWeeks(1), "Feriado de prueba"));

        PlanKinesiologiaDto plan = crearPlan(paciente, kinesiologo, 3, primerLunes);
        PlanKinesiologiaDto validado = kinesiologiaService.validar(plan.id(), null);

        assertThat(validado.estado()).isEqualTo("VALIDADO");
        assertThat(validado.sesionesGeneradas()).isEqualTo(3);

        List<LocalDate> fechasGeneradas = validado.sesiones().stream().map(SesionKinesiologiaDto::fecha).sorted().toList();
        assertThat(fechasGeneradas).containsExactly(
                primerLunes,
                primerLunes.plusWeeks(2),
                primerLunes.plusWeeks(3)
        );
        assertThat(fechasGeneradas).doesNotContain(primerLunes.plusWeeks(1));
    }

    @Test
    void unHorarioYaOcupadoPorOtroTurnoSeSalteaYNoCuentaContraLaCuota() {
        Medico kinesiologo = crearKinesiologoConReglaLunes();
        Usuario paciente = crearPaciente("paciente-ocupado@test.com", "30222222");
        LocalDate primerLunes = proximoLunes();

        Turno turnoAjeno = new Turno();
        turnoAjeno.setMedico(kinesiologo);
        turnoAjeno.setFecha(primerLunes.plusWeeks(1));
        turnoAjeno.setHoraInicio(HORA);
        turnoAjeno.setHoraFin(HORA.plusMinutes(30));
        turnoRepository.save(turnoAjeno);

        PlanKinesiologiaDto plan = crearPlan(paciente, kinesiologo, 2, primerLunes);
        PlanKinesiologiaDto validado = kinesiologiaService.validar(plan.id(), null);

        assertThat(validado.sesionesGeneradas()).isEqualTo(2);
        List<LocalDate> fechasDelPlan = validado.sesiones().stream().map(SesionKinesiologiaDto::fecha).sorted().toList();
        assertThat(fechasDelPlan).containsExactly(primerLunes, primerLunes.plusWeeks(2));
    }

    @Test
    void horariosDisponiblesDeUnMedicoSoloListaLosSuyos() {
        Medico kinesiologo = crearKinesiologoConReglaLunes();

        List<com.fktlomas.turnos.dto.kinesiologia.HorarioDisponibleDto> horarios = kinesiologiaService.horariosDisponibles(kinesiologo.getId());

        assertThat(horarios).isNotEmpty();
        assertThat(horarios).allMatch(h -> h.medicoId().equals(kinesiologo.getId()) && h.diaSemana() == DayOfWeek.MONDAY);
        // 08:00 a 12:00 en turnos de 30' → 8 horarios reales, no un solo texto libre
        assertThat(horarios).hasSize(8);
    }

    @Test
    void unHorarioFueraDeLaReglaTiraErrorConElDiaEnEspanol() {
        Medico kinesiologo = crearKinesiologoConReglaLunes();
        Usuario paciente = crearPaciente("paciente-horario-invalido@test.com", "30444444");
        MockMultipartFile ordenMedica = new MockMultipartFile(
                "ordenMedica", "orden.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03, 0x04});

        assertThatThrownBy(() -> kinesiologiaService.crearParaPaciente(
                paciente, kinesiologo.getId(), 1, proximoLunes(),
                List.of(DayOfWeek.TUESDAY), List.of(HORA), ordenMedica))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("martes")
                .hasMessageNotContaining("TUESDAY");
    }

    @Test
    void horariosDisponiblesSinMedicoJuntaTodosLosKinesiologosActivos() {
        Medico kinesiologo1 = crearKinesiologoConReglaLunes();
        Medico kinesiologo2 = crearKinesiologoConReglaLunes();

        List<com.fktlomas.turnos.dto.kinesiologia.HorarioDisponibleDto> horarios = kinesiologiaService.horariosDisponibles(null);

        assertThat(horarios.stream().map(com.fktlomas.turnos.dto.kinesiologia.HorarioDisponibleDto::medicoId).distinct().toList())
                .contains(kinesiologo1.getId(), kinesiologo2.getId());
    }

    @Test
    void pausarLibraSesionesFuturasYReanudarGeneraLasQueFaltan() {
        Medico kinesiologo = crearKinesiologoConReglaLunes();
        Usuario paciente = crearPaciente("paciente-pausa@test.com", "30333333");
        LocalDate primerLunes = proximoLunes();

        PlanKinesiologiaDto plan = crearPlan(paciente, kinesiologo, 2, primerLunes);
        kinesiologiaService.validar(plan.id(), null);

        PlanKinesiologiaDto pausado = kinesiologiaService.pausar(plan.id(), paciente.getId());
        assertThat(pausado.estado()).isEqualTo("PAUSADO");
        assertThat(pausado.sesiones()).allMatch(s -> !"RESERVADO".equals(s.estado()));

        PlanKinesiologiaDto reanudado = kinesiologiaService.reanudar(plan.id(), paciente.getId(), LocalDate.now());
        assertThat(reanudado.estado()).isEqualTo("VALIDADO");
        assertThat(reanudado.sesionesGeneradas()).isEqualTo(2);
    }

    private PlanKinesiologiaDto crearPlan(Usuario paciente, Medico kinesiologo, int sesiones, LocalDate fechaInicio) {
        MockMultipartFile ordenMedica = new MockMultipartFile(
                "ordenMedica", "orden.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03, 0x04});
        return kinesiologiaService.crearParaPaciente(
                paciente, kinesiologo.getId(), sesiones, fechaInicio,
                List.of(DayOfWeek.MONDAY), List.of(HORA), ordenMedica);
    }

    private Medico crearKinesiologoConReglaLunes() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Kinesiología " + System.nanoTime());
        especialidad.setEsKinesiologia(true);
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Kinesiólogo Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        medico = medicoRepository.save(medico);

        ReglaDisponibilidad regla = new ReglaDisponibilidad();
        regla.setMedico(medico);
        regla.setDiaSemana(DayOfWeek.MONDAY);
        regla.setHoraInicio(LocalTime.of(8, 0));
        regla.setHoraFin(LocalTime.of(12, 0));
        reglaDisponibilidadRepository.save(regla);

        return medico;
    }

    private Usuario crearPaciente(String email, String dni) {
        Usuario paciente = new Usuario();
        paciente.setEmail(email);
        paciente.setDni(dni);
        paciente.setNombreCompleto("Paciente Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }

    private LocalDate proximoLunes() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
}
