package com.fktlomas.turnos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el flujo de login de punta a punta contra una base H2 en memoria (sin Flyway,
 * schema creado por Hibernate) — cubre lo que TurnoRepositoryReservaTest no toca: la
 * cadena completa registro → login → JWT, y el bloqueo por fuerza bruta de
 * LoginRateLimiterService integrado con AuthController/GlobalExceptionHandler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-controller-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        // Sin esto, cada registro de este test le pegaría de verdad a la API de HaveIBeenPwned
        // (ver PasswordFiltradaService) — lento, no determinístico, y depende de tener salida a
        // internet en CI. La contraseña de prueba de este archivo no está filtrada de todos
        // modos, pero no vale la pena depender de un servicio externo para que el test pase.
        "app.seguridad.chequeo-password-filtrada=false"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registroYLoginExitosoDevuelvenTokenYRolPaciente() throws Exception {
        String email = "paciente.ok@test.com";
        registrarPaciente(email, "Segura123!");

        mockMvc.perform(loginRequest(email, "Segura123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("PACIENTE"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401YNoRevelaSiElEmailExiste() throws Exception {
        String email = "paciente.malapass@test.com";
        registrarPaciente(email, "Segura123!");

        mockMvc.perform(loginRequest(email, "OtraClave1!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void trasCincoIntentosFallidosElSextoQuedaBloqueadoAunqueLaPasswordSeaCorrecta() throws Exception {
        String email = "paciente.bloqueo@test.com";
        registrarPaciente(email, "Segura123!");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(loginRequest(email, "OtraClave1!"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest(email, "Segura123!"))
                .andExpect(status().isTooManyRequests());
    }

    private void registrarPaciente(String email, String password) throws Exception {
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni":"%s",
                                  "obraSocial":"Particular",
                                  "nombreCompleto":"Paciente Test",
                                  "email":"%s",
                                  "telefono":"1122334455",
                                  "genero":"PREFIERO_NO_DECIR",
                                  "password":"%s",
                                  "aceptaPoliticaPrivacidad":true
                                }
                                """.formatted(dni, email, password)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password));
    }
}
