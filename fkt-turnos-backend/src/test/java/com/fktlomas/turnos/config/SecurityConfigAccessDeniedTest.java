package com.fktlomas.turnos.config;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre el bug real que MockMvc NO reproduce (ver AutorizacionRolesTest — sus mismos casos
 * pasan igual con o sin el fix de este archivo, porque el dispatcher simulado de MockMvc no
 * pasa por la misma resolución de ExceptionTranslationFilter que un servidor real): una cuenta
 * de staff con 2FA obligatorio pero todavía no activado (rol "pendiente", ver AppUserDetails)
 * que pisa /api/admin/** tiene que recibir 403, no 401 — un 401 hace que el frontend la
 * deslogueé en el acto (axiosClient.js, evento fkt:sesion-vencida) en vez de dejarla ver la
 * pantalla para activar el 2FA. Reproducido primero a mano con curl contra un servidor real
 * antes de escribir este test; @SpringBootTest con WebEnvironment.RANDOM_PORT + TestRestTemplate
 * es lo que hace falta para que el test pegue contra un Tomcat de verdad en vez del dispatcher
 * simulado de MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-access-denied-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SecurityConfigAccessDeniedTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CLAVE = "Segura123!";

    @Test
    void secretariaConMfaPendienteRecibe403NoUnAutenticadoAlPisarAdmin() {
        Usuario secretaria = new Usuario();
        secretaria.setEmail("secretaria-pending-" + System.nanoTime() + "@test.com");
        secretaria.setNombreCompleto("Secretaria Pending Test");
        secretaria.setPasswordHash(passwordEncoder.encode(CLAVE));
        secretaria.setRole(Role.SECRETARIA);
        secretaria.setMfaHabilitado(false); // rol real ROLE_SECRETARIA_MFA_PENDIENTE, no ROLE_SECRETARIA
        secretaria = usuarioRepository.save(secretaria);

        String url = "http://localhost:" + port;
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                url + "/api/auth/login",
                Map.of("email", secretaria.getEmail(), "password", CLAVE),
                Map.class
        );
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) loginResp.getBody().get("token");
        assertThat((Boolean) loginResp.getBody().get("mfaSetupRequerido")).isTrue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> resp = restTemplate.exchange(
                url + "/api/admin/medicos", HttpMethod.GET, new HttpEntity<>(headers), Map.class
        );

        // La garantía puntual: 403 (autenticado, rol insuficiente), NUNCA 401 (que el
        // frontend confunde con "sesión vencida" y desloguea sin explicación).
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
