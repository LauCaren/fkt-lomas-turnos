package com.fktlomas.turnos.service;

import com.fktlomas.turnos.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterServiceTest {

    @Test
    void noBloqueaAntesDeCincoIntentosFallidos() {
        LoginRateLimiterService rateLimiter = new LoginRateLimiterService();
        String email = "paciente@test.com";

        for (int i = 0; i < 4; i++) {
            rateLimiter.registrarFallo(email);
        }

        rateLimiter.verificarNoBloqueado(email);
    }

    @Test
    void bloqueaAlQuintoIntentoFallidoSeguido() {
        LoginRateLimiterService rateLimiter = new LoginRateLimiterService();
        String email = "paciente@test.com";

        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFallo(email);
        }

        assertThatThrownBy(() -> rateLimiter.verificarNoBloqueado(email))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void unLoginExitosoLimpiaElContadorDeFallos() {
        LoginRateLimiterService rateLimiter = new LoginRateLimiterService();
        String email = "paciente@test.com";

        for (int i = 0; i < 4; i++) {
            rateLimiter.registrarFallo(email);
        }
        rateLimiter.registrarExito(email);

        rateLimiter.registrarFallo(email);
        rateLimiter.verificarNoBloqueado(email);
    }

    @Test
    void elBloqueoEsPorEmailYNoAfectaAOtrosUsuarios() {
        LoginRateLimiterService rateLimiter = new LoginRateLimiterService();

        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFallo("bloqueado@test.com");
        }

        assertThatThrownBy(() -> rateLimiter.verificarNoBloqueado("bloqueado@test.com"))
                .isInstanceOf(DemasiadosIntentosException.class);
        rateLimiter.verificarNoBloqueado("otro@test.com");
    }
}
