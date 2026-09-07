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
import static org.mockito.Mockito.verify;

/**
 * Complemento de AuthServiceLoginFueraDeHorarioTest: acá la ventana horaria se fuerza a
 * 00:00-23:59 (prácticamente todo el día) para confirmar que un login normal, dentro de
 * horario, no dispara ninguna alerta.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-horario-dentro-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seguridad.horario-inicio=00:00",
        "app.seguridad.horario-fin=23:59"
})
class AuthServiceLoginDentroDeHorarioTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @Test
    void loginDeSecretariaDentroDeHorarioNoAvisa() {
        Usuario usuario = new Usuario();
        usuario.setEmail("secretaria-dentro@test.com");
        usuario.setNombreCompleto("Secretaria Dentro Horario");
        usuario.setTelefono("1122334455");
        usuario.setGenero(Genero.PREFIERO_NO_DECIR);
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash(passwordEncoder.encode("Segura123!"));
        usuario.setRole(Role.SECRETARIA);
        usuarioRepository.save(usuario);

        authService.login(new LoginRequest("secretaria-dentro@test.com", "Segura123!", null));

        verify(emailService, never()).notificarLoginFueraDeHorario(any(Usuario.class), any(ZonedDateTime.class));
    }
}
