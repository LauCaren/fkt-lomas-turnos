package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.admin.ReglaDisponibilidadRequest;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.FeriadoRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Cubre la generación de turnos a partir de reglas de disponibilidad: es la parte con más
 * lógica de fechas del sistema (saltear feriados, no duplicar si ya corrió antes, no salirse
 * del rango horario de la regla) y hasta ahora no tenía ningún test.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:disponibilidad-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class DisponibilidadServiceTest {

    @Autowired
    private DisponibilidadService disponibilidadService;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private FeriadoRepository feriadoRepository;

    @Test
    void generarTurnosRespetaLaReglaSalteaFeriadosYNoDuplicaSiCorreDeNuevo() {
        Medico medico = crearMedico(30);
        LocalDate primerLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate segundoLunes = primerLunes.plusWeeks(1);

        disponibilidadService.crearRegla(medico.getId(),
                new ReglaDisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        Feriado feriado = new Feriado();
        feriado.setFecha(segundoLunes);
        feriado.setNombre("Feriado de prueba");
        feriado.setTipo(TipoFeriado.CENTRO);
        feriadoRepository.save(feriado);

        int creados = disponibilidadService.generarTurnos(medico.getId(), primerLunes, segundoLunes.plusDays(1));

        // Una hora dividida en turnos de 30 min = 2 turnos, solo el primer lunes (el segundo es feriado).
        assertThat(creados).isEqualTo(2);

        List<Turno> turnosPrimerLunes = turnoRepository.findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(
                medico.getId(), primerLunes, EstadoTurno.DISPONIBLE);
        assertThat(turnosPrimerLunes).extracting(Turno::getHoraInicio)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30));

        List<Turno> turnosSegundoLunes = turnoRepository.findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(
                medico.getId(), segundoLunes, EstadoTurno.DISPONIBLE);
        assertThat(turnosSegundoLunes).isEmpty();

        // Correrlo de nuevo sobre el mismo rango no debe duplicar los turnos ya generados.
        int segundaCorrida = disponibilidadService.generarTurnos(medico.getId(), primerLunes, segundoLunes.plusDays(1));
        assertThat(segundaCorrida).isZero();
        assertThat(turnoRepository.findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(medico.getId(), primerLunes, EstadoTurno.DISPONIBLE))
                .hasSize(2);
    }

    @Test
    void generarTurnosNoCreaNadaFueraDelDiaDeLaRegla() {
        Medico medico = crearMedico(30);
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate martes = lunes.plusDays(1);

        disponibilidadService.crearRegla(medico.getId(),
                new ReglaDisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 30)));

        disponibilidadService.generarTurnos(medico.getId(), martes, martes);

        assertThat(turnoRepository.findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(medico.getId(), martes, EstadoTurno.DISPONIBLE))
                .isEmpty();
    }

    @Test
    void generarTurnosConHastaAnteriorADesdeTiraIllegalArgumentException() {
        Medico medico = crearMedico(30);
        LocalDate hoy = LocalDate.now();

        assertThatThrownBy(() -> disponibilidadService.generarTurnos(medico.getId(), hoy, hoy.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generarTurnosConRangoMayorA90DiasTiraIllegalArgumentException() {
        Medico medico = crearMedico(30);
        LocalDate hoy = LocalDate.now();

        assertThatThrownBy(() -> disponibilidadService.generarTurnos(medico.getId(), hoy, hoy.plusDays(91)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearReglaEnFinDeSemanaTiraIllegalArgumentException() {
        Medico medico = crearMedico(30);

        assertThatThrownBy(() -> disponibilidadService.crearRegla(medico.getId(),
                new ReglaDisponibilidadRequest(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearReglaConHoraInicioNoAnteriorAHoraFinTiraIllegalArgumentException() {
        Medico medico = crearMedico(30);

        assertThatThrownBy(() -> disponibilidadService.crearRegla(medico.getId(),
                new ReglaDisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearReglaFueraDelHorarioPermitidoTiraIllegalArgumentException() {
        Medico medico = crearMedico(30);

        assertThatThrownBy(() -> disponibilidadService.crearRegla(medico.getId(),
                new ReglaDisponibilidadRequest(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(9, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Medico crearMedico(int duracionTurnoMinutos) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad disponibilidad test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Disponibilidad Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(duracionTurnoMinutos);
        return medicoRepository.save(medico);
    }
}
