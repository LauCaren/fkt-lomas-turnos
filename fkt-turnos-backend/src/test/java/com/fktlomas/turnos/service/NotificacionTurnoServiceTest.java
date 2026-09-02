package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.CanalNotificacion;
import com.fktlomas.turnos.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test puro (sin contexto de Spring): NotificacionTurnoService no toca la base, solo
 * decide qué canal usar y a quién delegar — Mockito alcanza y corre en milisegundos.
 */
class NotificacionTurnoServiceTest {

    private EmailService emailService;
    private WhatsAppService whatsAppService;
    private NotificacionTurnoService notificacionTurnoService;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        whatsAppService = mock(WhatsAppService.class);
        notificacionTurnoService = new NotificacionTurnoService(emailService, whatsAppService, new CanalFallbackService());
    }

    @Test
    void siFallaElEmailYHayTelefonoReintentaPorWhatsapp() {
        Usuario paciente = pacienteConCanal(CanalNotificacion.EMAIL, "1122334455");
        doThrow(new IllegalStateException("SMTP caído")).when(emailService)
                .enviarCancelacionTurnoHtmlSync(any(), anyString(), anyString(), any(), any());

        notificacionTurnoService.confirmarCancelacion(paciente, "Dr. Test", "Kinesiología",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30));

        verify(whatsAppService).enviar(eq("1122334455"), anyString());
    }

    @Test
    void siFallaElEmailYNoHayTelefonoNoReintentaPorWhatsapp() {
        Usuario paciente = pacienteConCanal(CanalNotificacion.EMAIL, null);
        doThrow(new IllegalStateException("SMTP caído")).when(emailService)
                .enviarCancelacionTurnoHtmlSync(any(), anyString(), anyString(), any(), any());

        notificacionTurnoService.confirmarCancelacion(paciente, "Dr. Test", "Kinesiología",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30));

        verify(whatsAppService, never()).enviar(anyString(), anyString());
    }

    @Test
    void siElCanalPreferidoFuncionaNoTocaElOtroCanal() {
        Usuario paciente = pacienteConCanal(CanalNotificacion.EMAIL, "1122334455");

        notificacionTurnoService.confirmarCancelacion(paciente, "Dr. Test", "Kinesiología",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30));

        verify(emailService).enviarCancelacionTurnoHtmlSync(eq(paciente), eq("Dr. Test"), eq("Kinesiología"), any(), any());
        verifyNoInteractions(whatsAppService);
    }

    @Test
    void siFallaWhatsappYHayEmailReintentaPorEmail() {
        Usuario paciente = pacienteConCanal(CanalNotificacion.WHATSAPP, "1122334455");
        doThrow(new IllegalStateException("Twilio no configurado")).when(whatsAppService).enviar(anyString(), anyString());

        notificacionTurnoService.avisarTurnoLiberado(paciente, "Dr. Test", "Kinesiología", LocalDate.now().plusDays(2));

        verify(emailService).enviarTurnoLiberadoHtmlSync(eq(paciente), eq("Dr. Test"), eq("Kinesiología"), any());
    }

    private Usuario pacienteConCanal(CanalNotificacion canal, String telefono) {
        Usuario paciente = new Usuario();
        paciente.setNombreCompleto("Paciente Test");
        paciente.setEmail("paciente@test.com");
        paciente.setTelefono(telefono);
        paciente.setCanalNotificacionPreferido(canal);
        return paciente;
    }
}
