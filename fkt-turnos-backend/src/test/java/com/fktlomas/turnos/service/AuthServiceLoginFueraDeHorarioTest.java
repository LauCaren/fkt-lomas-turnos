package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.auth.LoginRequest;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Cubre la alerta de login fuera de horario (ver AuthService.alertarLoginFueraDeHorarioSiCorresponde):
 * la ventana horaria se fuerza a 00:00-00:01 para que casi cualquier hora real en la que corra
 * el test caiga "fuera de horario", sin depender de a qué hora se ejecute la suite.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-horario-fuera-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seguridad.horario-inicio=00:00",
        "app.seguridad.horario-fin=00:01"
})
class AuthServiceLoginFueraDeHorarioTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @Test
    void loginDeSecretariaFueraDeHorarioAvisaPorMail() {
        crearUsuario("secretaria-horario@test.com", Role.SECRETARIA);

        authService.login(new LoginRequest("secretaria-horario@test.com", "Segura123!", null));

        verify(emailService, times(1)).notificarLoginFueraDeHorario(any(Usuario.class), any(ZonedDateTime.class));
    }

    @Test
    void dosLoginsSeguidosDeLaMismaPersonaSoloAvisanUnaVezPorElEnfriamiento() {
        crearUsuario("secretaria-repetida@test.com", Role.SECRETARIA);

        authService.login(new LoginRequest("secretaria-repetida@test.com", "Segura123!", null));
        authService.login(new LoginRequest("secretaria-repetida@test.com", "Segura123!", null));

        verify(emailService, times(1)).notificarLoginFueraDeHorario(any(Usuario.class), any(ZonedDateTime.class));
    }

    @Test
    void loginDePacienteFueraDeHorarioNoAvisaPorqueElRolNoAplica() {
        crearUsuario("paciente-horario@test.com", Role.PACIENTE);

        authService.login(new LoginRequest("paciente-horario@test.com", "Segura123!", null));

        verify(emailService, never()).notificarLoginFueraDeHorario(any(Usuario.class), any(ZonedDateTime.class));
    }

    @Test
    void loginDeMedicoFueraDeHorarioNoAvisaPorqueElRolNoAplica() {
        crearUsuario("medico-horario@test.com", Role.MEDICO);

        authService.login(new LoginRequest("medico-horario@test.com", "Segura123!", null));

        verify(emailService, never()).notificarLoginFueraDeHorario(any(Usuario.class), any(ZonedDateTime.class));
    }

    private void crearUsuario(String email, Role role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombreCompleto("Usuario Test Horario");
        usuario.setTelefono("1122334455");
        usuario.setGenero(Genero.PREFIERO_NO_DECIR);
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash(passwordEncoder.encode("Segura123!"));
        usuario.setRole(role);
        usuarioRepository.save(usuario);
    }
}
