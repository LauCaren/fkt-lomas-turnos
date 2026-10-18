package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.auth.LoginRequest;
import com.fktlomas.turnos.dto.auth.OlvidePasswordRequest;
import com.fktlomas.turnos.dto.auth.RegistroRequest;
import com.fktlomas.turnos.dto.auth.RestablecerPasswordRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Endurecimientos de autenticación de la revisión de seguridad de octubre 2026: códigos 2FA de
 * un solo uso, email sin distinguir mayúsculas, mensaje de registro que no revela si un DNI es
 * paciente, y token de recuperación guardado hasheado.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-seguridad-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seguridad.chequeo-password-filtrada=false"
})
class AuthServiceSeguridadTest {

    // Vector de prueba del RFC 6238: la clave ASCII "12345678901234567890" en Base32.
    private static final String SECRETO = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Autowired
    private AuthService authService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @Test
    void unCodigo2faSirveUnaSolaVez() {
        crearUsuario("mfa-replay@test.com", Role.PACIENTE, true);
        String codigo = codigoTotpActual();

        authService.login(new LoginRequest("mfa-replay@test.com", "Segura123!", codigo));

        assertThatThrownBy(() -> authService.login(new LoginRequest("mfa-replay@test.com", "Segura123!", codigo)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void elLoginNoDistingueMayusculasEnElEmail() {
        crearUsuario("paciente-mayus@test.com", Role.PACIENTE, false);

        var respuesta = authService.login(new LoginRequest("  Paciente-Mayus@Test.com ", "Segura123!", null));

        assertThat(respuesta.email()).isEqualTo("paciente-mayus@test.com");
    }

    @Test
    void registrarConUnDniExistenteNoRevelaQueEsElDni() {
        Usuario existente = crearUsuario("dni-existente@test.com", Role.PACIENTE, false);

        RegistroRequest conMismoDni = new RegistroRequest(existente.getDni(), "Particular", "Otra Persona",
                "otra.persona@test.com", "1122334455", Genero.FEMENINO, "Segura123!", true, null);

        assertThatThrownBy(() -> authService.registrarPaciente(conMismoDni))
                .isInstanceOf(ConflictoDeDatosException.class)
                .hasMessage(AuthService.MENSAJE_CUENTA_EXISTENTE);
    }

    @Test
    void elTokenDeRecuperacionSeGuardaHasheadoYElDelMailSigueSirviendo() {
        Usuario usuario = crearUsuario("reset-hash@test.com", Role.PACIENTE, false);

        authService.olvidePassword(new OlvidePasswordRequest("reset-hash@test.com"));

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarRecuperacionPassword(any(Usuario.class), link.capture());
        String tokenDelMail = link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
        String guardado = usuarioRepository.findById(usuario.getId()).orElseThrow().getResetToken();
        assertThat(guardado).isNotEqualTo(tokenDelMail).isEqualTo(AuthService.hashDeToken(tokenDelMail));

        authService.restablecerPassword(new RestablecerPasswordRequest(tokenDelMail, "OtraSegura456!"));

        assertThat(passwordEncoder.matches("OtraSegura456!",
                usuarioRepository.findById(usuario.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    private Usuario crearUsuario(String email, Role role, boolean conMfa) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setDni(String.valueOf(10_000_000 + Math.abs(email.hashCode() % 80_000_000)));
        usuario.setNombreCompleto("Usuario Test Seguridad");
        usuario.setTelefono("1122334455");
        usuario.setGenero(Genero.PREFIERO_NO_DECIR);
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash(passwordEncoder.encode("Segura123!"));
        usuario.setRole(role);
        if (conMfa) {
            usuario.setMfaSecret(SECRETO);
            usuario.setMfaHabilitado(true);
        }
        return usuarioRepository.save(usuario);
    }

    /** HOTP del RFC 4226 sobre el paso actual de 30 s — calculado acá a propósito, sin TotpService. */
    private static String codigoTotpActual() {
        try {
            long paso = Instant.now().getEpochSecond() / 30;
            byte[] datos = new byte[8];
            for (int i = 7; i >= 0; i--) {
                datos[i] = (byte) (paso & 0xFF);
                paso >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec("12345678901234567890".getBytes(), "HmacSHA1"));
            byte[] hash = mac.doFinal(datos);
            int offset = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binario % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
