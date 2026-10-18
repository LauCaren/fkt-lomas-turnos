package com.fktlomas.turnos.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private final RateLimitingFilter filtro = new RateLimitingFilter(new RateLimiterService());

    @Test
    void codificarUnaLetraDeLaRutaNoSaltaElLimiteDelLogin() throws Exception {
        // 15 intentos permitidos por IP cada 5 minutos (ver REGLAS): mezclando la ruta normal con
        // la codificada, el intento 16 tiene que cortarse igual.
        for (int i = 0; i < 15; i++) {
            assertThat(pedir(i % 2 == 0 ? "/api/auth/login" : "/api/auth/%6Cogin").getStatus()).isEqualTo(200);
        }
        assertThat(pedir("/api/auth/%6Cogin").getStatus()).isEqualTo(429);
    }

    @Test
    void otrasRutasNoSeLimitan() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertThat(pedir("/api/especialidades").getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse pedir(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr("203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
