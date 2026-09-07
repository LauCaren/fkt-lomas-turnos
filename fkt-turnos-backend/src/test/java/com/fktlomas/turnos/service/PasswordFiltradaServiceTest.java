package com.fktlomas.turnos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SHA-1("Segura123!") = A8A943B5F7A8A84FC0FCB1EA1856FF6F13F04C0F (verificado con `sha1sum`) —
 * el sufijo de abajo es esos 40 caracteres menos el prefijo de 5 que arma la URL de la consulta.
 */
@ExtendWith(MockitoExtension.class)
class PasswordFiltradaServiceTest {

    private static final String PASSWORD = "Segura123!";
    private static final String SUFIJO = "3B5F7A8A84FC0FCB1EA1856FF6F13F04C0F";

    @Mock
    private java.net.http.HttpClient httpClient;

    private PasswordFiltradaService service;

    @BeforeEach
    void setUp() {
        service = new PasswordFiltradaService(httpClient);
        ReflectionTestUtils.setField(service, "habilitado", true);
    }

    @Test
    void noHaceNingunaLlamadaCuandoElChequeoEstaDeshabilitado() throws Exception {
        ReflectionTestUtils.setField(service, "habilitado", false);

        assertThatCode(() -> service.verificar(PASSWORD)).doesNotThrowAnyException();
        org.mockito.Mockito.verifyNoInteractions(httpClient);
    }

    @Test
    void rechazaUnaContraseñaCuyoSufijoApareceEnLaRespuesta() throws Exception {
        HttpResponse<String> respuesta = respuestaCon(200, SUFIJO + ":3730471\r\nOTROSUFIJODEPRUEBA0000000000000:1");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(respuesta);

        assertThatThrownBy(() -> service.verificar(PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filtraciones");
    }

    @Test
    void aceptaUnaContraseñaCuyoSufijoNoApareceEnLaRespuesta() throws Exception {
        HttpResponse<String> respuesta = respuestaCon(200, "OTROSUFIJODEPRUEBA0000000000000:1");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(respuesta);

        assertThatCode(() -> service.verificar(PASSWORD)).doesNotThrowAnyException();
    }

    @Test
    void fallaAbiertoSiHaveIBeenPwnedResponde500() throws Exception {
        HttpResponse<String> respuesta = respuestaCon(500, "");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(respuesta);

        assertThatCode(() -> service.verificar(PASSWORD)).doesNotThrowAnyException();
    }

    @Test
    void fallaAbiertoSiNoHayConexion() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("sin red"));

        assertThatCode(() -> service.verificar(PASSWORD)).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> respuestaCon(int status, String body) {
        HttpResponse<String> respuesta = org.mockito.Mockito.mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(status);
        // lenient: con status != 200 el servicio nunca llega a leer body() — no todos los usos de este helper lo necesitan.
        org.mockito.Mockito.lenient().when(respuesta.body()).thenReturn(body);
        return respuesta;
    }
}
