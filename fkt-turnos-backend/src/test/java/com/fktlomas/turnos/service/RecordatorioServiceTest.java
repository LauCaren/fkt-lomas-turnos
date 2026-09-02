package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
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

    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private RecordatorioRepository recordatorioRepository;
    @Autowired
    private ConfiguracionRecordatoriosRepository configuracionRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private EmailService emailService;
    @MockBean
    private WhatsAppService whatsAppService;

    @Test
    void siFaltaLaConfiguracionElJobNoTiraLaExcepcionHaciaAfueraYAvisaUnaSolaVez() {
        // Sin Flyway no existe la fila de configuración (id=1) que en producción siembra la
        // migración — es exactamente la condición real que hace fallar a obtenerConfiguracionEntidad().
        assertThatCode(() -> recordatorioService.procesarPendientes()).doesNotThrowAnyException();
        assertThatCode(() -> recordatorioService.procesarPendientes()).doesNotThrowAnyException();

        // Dos corridas fallidas seguidas: una sola alerta (enfriamiento de 1 hora).
        verify(emailService, times(1)).notificarFalloJobRecordatorios(anyString());
    }

    @Test
    void siFallaElCanalPreferidoReintentaPorElOtroCanalYQuedaReflejadoEnElHistorial() {
        sembrarConfiguracion();
        Especialidad especialidad = especialidadRepository.save(nuevaEspecialidad());
        Medico medico = medicoRepository.save(nuevoMedico(especialidad));
        Usuario paciente = usuarioRepository.save(nuevoPaciente());
        Turno turno = turnoRepository.save(nuevoTurnoReservado(medico, paciente));
        Recordatorio recordatorio = recordatorioRepository.save(new Recordatorio(turno, CanalNotificacion.EMAIL));

        doThrow(new IllegalStateException("SMTP caído"))
                .when(emailService).enviarRecordatorioTurnoHtmlSync(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
        // whatsAppService.enviar(...) es void y no está stubbeado — con Mockito, eso ya es "no hace nada" (éxito).

        var resultado = recordatorioService.reenviar(recordatorio.getId());

        assertThat(resultado.estado()).isEqualTo("ENVIADO");
        assertThat(resultado.canal()).isEqualTo("WHATSAPP");
        verify(whatsAppService).enviar(eq(paciente.getTelefono()), anyString());
    }

    @Test
    void siFallaElCanalPreferidoYNoHayContactoAlternativoQuedaFallidoSinReintentar() {
        sembrarConfiguracion();
        Especialidad especialidad = especialidadRepository.save(nuevaEspecialidad());
        Medico medico = medicoRepository.save(nuevoMedico(especialidad));
        Usuario paciente = usuarioRepository.save(nuevoPaciente());
        paciente.setTelefono(null);
        usuarioRepository.save(paciente);
        Turno turno = turnoRepository.save(nuevoTurnoReservado(medico, paciente));
        Recordatorio recordatorio = recordatorioRepository.save(new Recordatorio(turno, CanalNotificacion.EMAIL));

        doThrow(new IllegalStateException("SMTP caído"))
                .when(emailService).enviarRecordatorioTurnoHtmlSync(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

        var resultado = recordatorioService.reenviar(recordatorio.getId());

        assertThat(resultado.estado()).isEqualTo("PENDIENTE");
        assertThat(resultado.canal()).isEqualTo("EMAIL");
        verify(whatsAppService, org.mockito.Mockito.never()).enviar(anyString(), anyString());
    }

    private void sembrarConfiguracion() {
        ConfiguracionRecordatorios config = new ConfiguracionRecordatorios();
        config.setPlantillaMensaje("Hola {paciente}, tu turno es el {fecha} a las {hora}.");
        config.setHorasAnticipacion(24);
        config.setUpdatedAt(Instant.now());
        configuracionRepository.save(config);
    }

    private Especialidad nuevaEspecialidad() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Kinesiología");
        return especialidad;
    }

    private Medico nuevoMedico(Especialidad especialidad) {
        Medico medico = new Medico();
        medico.setNombreCompleto("Dra. Test");
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medico;
    }

    private Usuario nuevoPaciente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("paciente-fallback@test.com");
        usuario.setDni("33333333");
        usuario.setNombreCompleto("Paciente Fallback");
        usuario.setTelefono("1122334455");
        usuario.setGenero(Genero.PREFIERO_NO_DECIR);
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash("hash");
        usuario.setRole(Role.PACIENTE);
        return usuario;
    }

    private Turno nuevoTurnoReservado(Medico medico, Usuario paciente) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setFecha(LocalDate.now().plusDays(1));
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        turno.setEstado(EstadoTurno.RESERVADO);
        turno.setPaciente(paciente);
        return turno;
    }
}
