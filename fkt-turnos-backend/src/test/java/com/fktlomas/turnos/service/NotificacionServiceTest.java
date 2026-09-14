package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.TipoNotificacion;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.NotificacionDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Notificaciones dentro de la app — campanita del header (punto "notificaciones" del pedido de UX). */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notificacion-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class NotificacionServiceTest {

    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void creaYListaLasPropiasOrdenadasPorMasNueva() {
        Usuario paciente = crearPaciente();

        notificacionService.crear(paciente, TipoNotificacion.TURNO_CANCELADO_STAFF, "Primera", null);
        notificacionService.crear(paciente, TipoNotificacion.TURNO_REPROGRAMADO, "Segunda", 55L);

        Page<NotificacionDto> pagina = notificacionService.mias(paciente.getId(), PageRequest.of(0, 10));

        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getContent().get(0).mensaje()).isEqualTo("Segunda");
        assertThat(pagina.getContent().get(0).turnoId()).isEqualTo(55L);
        assertThat(pagina.getContent().get(0).leida()).isFalse();
    }

    @Test
    void contarNoLeidasSoloCuentaLasNoLeidas() {
        Usuario paciente = crearPaciente();
        notificacionService.crear(paciente, TipoNotificacion.TURNO_CANCELADO_STAFF, "A", null);
        notificacionService.crear(paciente, TipoNotificacion.TURNO_CANCELADO_STAFF, "B", null);
        Long idPrimera = notificacionService.mias(paciente.getId(), PageRequest.of(0, 10)).getContent()
                .stream().min((a, b) -> a.mensaje().compareTo(b.mensaje())).orElseThrow().id();

        assertThat(notificacionService.contarNoLeidas(paciente.getId())).isEqualTo(2);

        notificacionService.marcarLeida(idPrimera, paciente.getId());

        assertThat(notificacionService.contarNoLeidas(paciente.getId())).isEqualTo(1);
    }

    @Test
    void marcarLeidaDeOtroUsuarioTiraRecursoNoEncontrado() {
        Usuario dueno = crearPaciente();
        Usuario otro = crearPaciente();
        notificacionService.crear(dueno, TipoNotificacion.TURNO_CANCELADO_STAFF, "Solo para el dueño", null);
        Long id = notificacionService.mias(dueno.getId(), PageRequest.of(0, 10)).getContent().get(0).id();

        assertThatThrownBy(() -> notificacionService.marcarLeida(id, otro.getId()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void marcarTodasLeidasLasDejaTodasEnCero() {
        Usuario paciente = crearPaciente();
        notificacionService.crear(paciente, TipoNotificacion.TURNO_CANCELADO_STAFF, "A", null);
        notificacionService.crear(paciente, TipoNotificacion.TURNO_REPROGRAMADO, "B", null);
        assertThat(notificacionService.contarNoLeidas(paciente.getId())).isEqualTo(2);

        notificacionService.marcarTodasLeidas(paciente.getId());

        assertThat(notificacionService.contarNoLeidas(paciente.getId())).isZero();
    }

    private Usuario crearPaciente() {
        Usuario paciente = new Usuario();
        paciente.setEmail("paciente-notif-" + System.nanoTime() + "@test.com");
        paciente.setNombreCompleto("Paciente Notificacion Test");
        paciente.setTelefono("1122334455");
        paciente.setGenero(Genero.PREFIERO_NO_DECIR);
        paciente.setObraSocial("Particular");
        paciente.setPasswordHash("hash");
        paciente.setRole(Role.PACIENTE);
        return usuarioRepository.save(paciente);
    }
}
