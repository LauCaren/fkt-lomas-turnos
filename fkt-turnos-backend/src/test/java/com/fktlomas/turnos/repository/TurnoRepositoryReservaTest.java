package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TurnoRepositoryReservaTest {

    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void unSoloPacientePuedeReservarUnTurnoDisponible() {
        Especialidad especialidad = especialidadRepository.save(nuevaEspecialidad());
        Medico medico = medicoRepository.save(nuevoMedico(especialidad));
        Turno turno = turnoRepository.save(nuevoTurno(medico));

        Usuario paciente1 = usuarioRepository.save(nuevoPaciente("paciente1@test.com", "11111111"));
        Usuario paciente2 = usuarioRepository.save(nuevoPaciente("paciente2@test.com", "22222222"));

        int filasPrimeraReserva = turnoRepository.reservarSiDisponible(turno.getId(), paciente1, "Control");
        assertThat(filasPrimeraReserva).isEqualTo(1);

        int filasSegundaReserva = turnoRepository.reservarSiDisponible(turno.getId(), paciente2, "Otra consulta");
        assertThat(filasSegundaReserva).isEqualTo(0);

        Turno actualizado = turnoRepository.findById(turno.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(actualizado.getPaciente().getId()).isEqualTo(paciente1.getId());
    }

    @Test
    void proximasFechasConDisponibilidadDevuelveSoloFechasFuturasConTurnosLibresOrdenadas() {
        Especialidad especialidad = especialidadRepository.save(nuevaEspecialidad());
        Medico medico = medicoRepository.save(nuevoMedico(especialidad));
        LocalDate hoy = LocalDate.now();

        // Sin lugar hoy (referencia): no debería aparecer en el resultado.
        turnoRepository.save(turnoEnFecha(medico, hoy));
        // Reservado en el futuro: no cuenta como disponible.
        Usuario paciente = usuarioRepository.save(nuevoPaciente("ocupante@test.com", "44444444"));
        Turno reservadoFuturo = turnoRepository.save(turnoEnFecha(medico, hoy.plusDays(2)));
        turnoRepository.reservarSiDisponible(reservadoFuturo.getId(), paciente, null);
        // Disponibles en el futuro, fuera de orden de inserción a propósito.
        turnoRepository.save(turnoEnFecha(medico, hoy.plusDays(10)));
        turnoRepository.save(turnoEnFecha(medico, hoy.plusDays(5)));
        turnoRepository.save(turnoEnFecha(medico, hoy.plusDays(3)));

        List<LocalDate> resultado = turnoRepository.proximasFechasConDisponibilidad(medico.getId(), hoy, PageRequest.of(0, 5));

        assertThat(resultado).containsExactly(hoy.plusDays(3), hoy.plusDays(5), hoy.plusDays(10));
    }

    private Turno turnoEnFecha(Medico medico, LocalDate fecha) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(fecha);
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        return turno;
    }

    private Especialidad nuevaEspecialidad() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Kinesiología");
        return especialidad;
    }

    private Medico nuevoMedico(Especialidad especialidad) {
        Medico medico = new Medico();
        medico.setNombreCompleto("Dra. Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medico;
    }

    private Turno nuevoTurno(Medico medico) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now().plusDays(1));
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        return turno;
    }

    private Usuario nuevoPaciente(String email, String dni) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setDni(dni);
        usuario.setNombreCompleto("Paciente Test");
        usuario.setTelefono("1122334455");
        usuario.setGenero(Genero.PREFIERO_NO_DECIR);
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash("hash");
        usuario.setRole(Role.PACIENTE);
        return usuario;
    }
}
