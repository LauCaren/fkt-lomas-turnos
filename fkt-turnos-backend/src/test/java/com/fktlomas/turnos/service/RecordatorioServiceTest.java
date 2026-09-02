package com.fktlomas.turnos.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Antes de este cambio, si procesarPendientes() fallaba antes de mandar un solo recordatorio
 * (ej. falta la fila de configuración, como pasa acá al no correr Flyway), el job quedaba
 * roto en silencio: Spring solo lo logueaba con su LoggingErrorHandler por defecto y nadie se
 * enteraba hasta que un paciente faltaba a un turno por no recibir el aviso. Cubre las dos
 * partes de la corrección: que el job no vuelve a tirar la excepción hacia afuera (sigue
 * "corriendo", aunque no procese nada esa vez) y que sí avisa al admin — pero no en cada
 * corrida si la falla persiste.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:recordatorio-service-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class RecordatorioServiceTest {

    @Autowired
    private RecordatorioService recordatorioService;

    @MockBean
    private EmailService emailService;

    @Test
    void siFaltaLaConfiguracionElJobNoTiraLaExcepcionHaciaAfueraYAvisaUnaSolaVez() {
        // Sin Flyway no existe la fila de configuración (id=1) que en producción siembra la
        // migración — es exactamente la condición real que hace fallar a obtenerConfiguracionEntidad().
        assertThatCode(() -> recordatorioService.procesarPendientes()).doesNotThrowAnyException();
        assertThatCode(() -> recordatorioService.procesarPendientes()).doesNotThrowAnyException();

        // Dos corridas fallidas seguidas: una sola alerta (enfriamiento de 1 hora).
        verify(emailService, times(1)).notificarFalloJobRecordatorios(anyString());
    }
}
