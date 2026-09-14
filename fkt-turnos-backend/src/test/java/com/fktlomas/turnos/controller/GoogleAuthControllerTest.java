package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.domain.Genero;
import com.fktlomas.turnos.domain.OrigenConsentimiento;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.GoogleTokenVerifierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Igual que AuthControllerTest pero para /api/auth/google — cubre lo que ese archivo no puede:
 * no hay forma de fabricar un ID token de Google real y válido en un test, así que se mockea
 * únicamente GoogleTokenVerifierService (la única frontera con Google) y se deja que el resto
 * del flujo (AuthController → AuthService → UsuarioRepository/JWT) corra de verdad, igual que
 * un login con email/contraseña normal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:google-auth-controller-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.seguridad.chequeo-password-filtrada=false"
})
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Test
    void cuentaNuevaSinPerfilPideDniObraSocialTelefonoYGenero() throws Exception {
        String googleId = "google-sub-nuevo-1";
        String email = "paciente.nuevo.google@test.com";
        when(googleTokenVerifierService.verificar(eq("credential-valido")))
                .thenReturn(new GoogleTokenVerifierService.DatosGoogle(googleId, email, "Paciente Nuevo"));

        mockMvc.perform(googleRequest("""
                        {"credential":"credential-valido"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requierePerfil").value(true))
                .andExpect(jsonPath("$.nombreSugerido").value("Paciente Nuevo"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").doesNotExist());

        org.junit.jupiter.api.Assertions.assertTrue(usuarioRepository.findByEmail(email).isEmpty(),
                "No debería crearse ninguna cuenta hasta que se manden dni/obraSocial/teléfono/género");
    }

    @Test
    void cuentaNuevaConPerfilCompletoQuedaCreadaComoPaciente() throws Exception {
        String googleId = "google-sub-nuevo-2";
        String email = "paciente.completo.google@test.com";
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        when(googleTokenVerifierService.verificar(eq("credential-valido-2")))
                .thenReturn(new GoogleTokenVerifierService.DatosGoogle(googleId, email, "Paciente Completo"));

        mockMvc.perform(googleRequest("""
                        {"credential":"credential-valido-2","dni":"%s","obraSocial":"Particular",
                         "telefono":"1122334455","genero":"PREFIERO_NO_DECIR","aceptaPoliticaPrivacidad":true}
                        """.formatted(dni)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requierePerfil").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PACIENTE"));

        Usuario creado = usuarioRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(googleId, creado.getGoogleId());
        org.junit.jupiter.api.Assertions.assertEquals(Role.PACIENTE, creado.getRole());
        org.junit.jupiter.api.Assertions.assertNotNull(creado.getPasswordHash(),
                "Aun sin password propia, passwordHash nunca debe quedar null (columna NOT NULL)");
    }

    @Test
    void cuentaExistenteConElMismoEmailQuedaVinculadaYLogueada() throws Exception {
        String email = "paciente.existente.google@test.com";
        Usuario existente = new Usuario();
        existente.setEmail(email);
        existente.setNombreCompleto("Paciente Ya Registrado");
        existente.setDni(String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999)));
        existente.setObraSocial("Particular");
        existente.setTelefono("1122334455");
        existente.setGenero(Genero.PREFIERO_NO_DECIR);
        existente.setPasswordHash(passwordEncoder.encode("Segura123!"));
        existente.setRole(Role.PACIENTE);
        existente.setAceptaPoliticaPrivacidad(true);
        existente.setOrigenConsentimiento(OrigenConsentimiento.WEB);
        usuarioRepository.save(existente);

        String googleId = "google-sub-vinculacion";
        when(googleTokenVerifierService.verificar(eq("credential-vinculacion")))
                .thenReturn(new GoogleTokenVerifierService.DatosGoogle(googleId, email, "Paciente Ya Registrado"));

        mockMvc.perform(googleRequest("""
                        {"credential":"credential-vinculacion"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requierePerfil").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nombreCompleto").value("Paciente Ya Registrado"));

        Usuario actualizado = usuarioRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(googleId, actualizado.getGoogleId());
    }

    @Test
    void tokenDeGoogleInvalidoDevuelve401() throws Exception {
        when(googleTokenVerifierService.verificar(any())).thenReturn(null);

        mockMvc.perform(googleRequest("""
                        {"credential":"credential-invalido"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder googleRequest(String body) {
        return post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
