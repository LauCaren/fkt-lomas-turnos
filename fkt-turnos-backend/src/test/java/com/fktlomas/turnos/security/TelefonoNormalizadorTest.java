package com.fktlomas.turnos.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TelefonoNormalizadorTest {

    @ParameterizedTest
    @CsvSource({
            "'whatsapp:+5491123456789', '+5491123456789'",
            "'whatsapp:+54 9 11 4970-3857', '+5491149703857'",
            "'+54 9 11 4970-3857', '+5491149703857'",
            "'(011) 4970-3857', '+01149703857'",
            "'5491123456789', '+5491123456789'",
    })
    void normalizaVariasFormasAlMismoFormato(String entrada, String esperado) {
        assertThat(TelefonoNormalizador.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    void devuelveNullSiElTelefonoEsNull() {
        assertThat(TelefonoNormalizador.normalizar(null)).isNull();
    }

    @Test
    void dosFormasDeEscribirElMismoNumeroNormalizanIgual() {
        String a = TelefonoNormalizador.normalizar("whatsapp:+54 9 11 4970-3857");
        String b = TelefonoNormalizador.normalizar("+5491149703857");
        assertThat(a).isEqualTo(b);
    }
}
