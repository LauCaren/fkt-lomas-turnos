package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.domain.ListaEspera;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.admin.ListaEsperaAdminDto;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.ListaEsperaRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.sql.Timestamp;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matching de lista de espera para un horario liberado (punto 11 del pedido de UX): con los
 * datos reales que existen (medico+fecha, sin hora ni prioridad), "compatible" es simplemente
 * "anotado para ese médico ese día", ordenado por quién se anotó primero.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:lista-espera-admin-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ListaEsperaAdminTest {

    @Autowired
    private ListaEsperaService listaEsperaService;
    @Autowired
    private ListaEsperaRepository listaEsperaRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void compatiblesParaSlotDevuelveOrdenadosPorAntiguedad() {
        Medico medico = crearMedico();
        LocalDate fecha = LocalDate.now().plusDays(3);
        Usuario segundo = crearPaciente();
        Usuario primero = crearPaciente();

        // Guardado en orden inverso al esperado (segundo antes que primero) y con el created_at
        // reescrito por SQL directo después: created_at es `updatable = false` en la entidad
        // (a propósito, para que nadie lo pise sin querer), así que un save() con setCreatedAt()
        // no alcanza — hay que tocar la columna por fuera de JPA. Esto es lo que hace que el test
        // verifique de verdad el ORDER BY createdAt, en vez de depender de la resolución del
        // reloj entre dos inserts consecutivos de un test rápido.
        ListaEspera deSegundo = anotar(segundo, medico, fecha);
        ListaEspera dePrimero = anotar(primero, medico, fecha);
        Instant ahora = Instant.now();
        jdbcTemplate.update("update lista_espera set created_at = ? where id = ?", Timestamp.from(ahora), deSegundo.getId());
        jdbcTemplate.update("update lista_espera set created_at = ? where id = ?", Timestamp.from(ahora.minusSeconds(60)), dePrimero.getId());

        var resultado = listaEsperaService.compatiblesParaSlot(medico.getId(), fecha);

        assertThat(resultado).extracting(ListaEsperaAdminDto::pacienteId).containsExactly(primero.getId(), segundo.getId());
    }

    @Test
    void compatiblesParaSlotNoIncluyeOtroMedicoUOtroDia() {
        Medico medico = crearMedico();
        Medico otroMedico = crearMedico();
        Usuario paciente = crearPaciente();
        LocalDate fecha = LocalDate.now().plusDays(3);

        anotar(paciente, medico, fecha);
        anotar(paciente, otroMedico, fecha);
        anotar(paciente, medico, fecha.plusDays(1));

        var resultado = listaEsperaService.compatiblesParaSlot(medico.getId(), fecha);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).pacienteId()).isEqualTo(paciente.getId());
    }

    private ListaEspera anotar(Usuario paciente, Medico medico, LocalDate fecha) {
        ListaEspera le = new ListaEspera();
        le.setPaciente(paciente);
        le.setMedico(medico);
        le.setFecha(fecha);
        return listaEsperaRepository.save(le);
    }

    private Medico crearMedico() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad lista espera test " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Lista Espera Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medicoRepository.save(medico);
    }

    private Usuario crearPaciente() {
        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-lista-espera-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Lista Espera Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
