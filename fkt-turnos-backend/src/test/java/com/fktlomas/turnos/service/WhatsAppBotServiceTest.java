package com.fktlomas.turnos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fktlomas.turnos.domain.ConversacionWhatsApp;
import com.fktlomas.turnos.domain.EstadoConversacion;
import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.EspecialidadDto;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.repository.ConversacionWhatsAppRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unidad pura con Mockito (no @SpringBootTest) — misma razón que MercadoPagoServiceTest: nada
 * de contexto de Spring que ganar acá, y las dependencias externas reales (WhatsAppService
 * habla con Twilio) no deben ejecutarse en un test.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppBotServiceTest {

    private static final String TELEFONO = "+5491123456789";

    @Mock private ConversacionWhatsAppRepository conversacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EspecialidadService especialidadService;
    @Mock private MedicoService medicoService;
    @Mock private TurnoService turnoService;
    @Mock private ListaEsperaService listaEsperaService;
    @Mock private WhatsAppService whatsAppService;
    @Mock private RateLimiterService rateLimiterService;

    private WhatsAppBotService service;
    private Usuario bot;
    private Usuario paciente;

    @BeforeEach
    void setUp() {
        service = new WhatsAppBotService(conversacionRepository, usuarioRepository, especialidadService,
                medicoService, turnoService, listaEsperaService, whatsAppService, rateLimiterService, new ObjectMapper());

        bot = new Usuario();
        bot.setId(1L);
        lenient().when(usuarioRepository.findByEmail(WhatsAppBotAccountInitializer.EMAIL_BOT)).thenReturn(Optional.of(bot));

        paciente = new Usuario();
        paciente.setId(50L);
        paciente.setDni("30111222");
        paciente.setNombreCompleto("Juana Pérez");
        paciente.setObraSocial("OSDE");

        lenient().when(conversacionRepository.findByTelefono(TELEFONO)).thenAnswer(inv -> Optional.empty());
    }

    private ConversacionWhatsApp conversacionEn(EstadoConversacion estado, Usuario pacienteIdentificado, String contextoJson) {
        ConversacionWhatsApp c = new ConversacionWhatsApp();
        c.setTelefono(TELEFONO);
        c.setEstado(estado);
        c.setPaciente(pacienteIdentificado);
        c.setContextoJson(contextoJson);
        c.setUltimaActividad(Instant.now());
        when(conversacionRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(c));
        return c;
    }

    private void enviarMensaje(String body) {
        service.procesarMensajeEntrante(Map.of("From", "whatsapp:" + TELEFONO, "Body", body));
    }

    private String ultimoMensajeEnviado() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(whatsAppService, org.mockito.Mockito.atLeastOnce()).enviar(eq(TELEFONO), captor.capture());
        return captor.getValue();
    }

    // ---------- Kinesiología excluida del menú de especialidades ----------

    @Test
    void laListaDeEspecialidadesNuncaIncluyeKinesiologia() {
        conversacionEn(EstadoConversacion.MENU_PRINCIPAL, paciente, null);
        when(especialidadService.listar()).thenReturn(List.of(
                new EspecialidadDto(1L, "Kinesiología", true),
                new EspecialidadDto(2L, "Traumatología", false)
        ));

        enviarMensaje("1");

        String mensaje = ultimoMensajeEnviado();
        assertThat(mensaje).contains("Traumatología");
        assertThat(mensaje).doesNotContain("Kinesiología");
    }

    // ---------- Rate limit de intentos de DNI ----------

    @Test
    void seBloqueaTrasCincoDniQueNoSeEncuentran() {
        ConversacionWhatsApp conversacion = conversacionEn(EstadoConversacion.ESPERANDO_DNI, null, null);
        lenient().when(rateLimiterService.permitir(anyString(), anyInt(), any())).thenReturn(true);
        when(usuarioRepository.findByDni(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < 4; i++) {
            enviarMensaje("3011122" + i);
            assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacion.ESPERANDO_DNI);
        }
        enviarMensaje("30111229");

        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacion.BLOQUEADO_TEMPORAL);
    }

    @Test
    void siElRateLimiterYaCortoNiSiquieraConsultaLaBaseDeDnis() {
        conversacionEn(EstadoConversacion.ESPERANDO_DNI, null, null);
        when(rateLimiterService.permitir(eq("whatsapp-dni|" + TELEFONO), eq(5), any())).thenReturn(false);

        enviarMensaje("30111222");

        verify(usuarioRepository, never()).findByDni(anyString());
        assertThat(ultimoMensajeEnviado()).containsIgnoringCase("demasiados intentos");
    }

    @Test
    void unDniQueSiExisteIdentificaAlPacienteYMuestraElMenu() {
        ConversacionWhatsApp conversacion = conversacionEn(EstadoConversacion.ESPERANDO_DNI, null, null);
        when(rateLimiterService.permitir(anyString(), anyInt(), any())).thenReturn(true);
        when(usuarioRepository.findByDni("30111222")).thenReturn(Optional.of(paciente));

        enviarMensaje("30111222");

        assertThat(conversacion.getEstado()).isEqualTo(EstadoConversacion.MENU_PRINCIPAL);
        assertThat(conversacion.getPaciente()).isEqualTo(paciente);
        assertThat(ultimoMensajeEnviado()).contains("Juana");
    }

    // ---------- Confirmación de reserva ----------

    @Test
    void confirmarLaReservaLlamaAReservarParaPacienteConElActorBot() {
        conversacionEn(EstadoConversacion.CONFIRMANDO_RESERVA, paciente,
                "{\"turnoId\":999,\"opcionesIds\":[],\"opcionesFechas\":[]}");
        TurnoAdminDto reservado = turnoAdminDtoDe(paciente.getId(), 77L, false);
        when(turnoService.reservarParaPaciente(eq(999L), any(ReservarParaPacienteRequest.class), eq(bot)))
                .thenReturn(reservado);
        when(usuarioRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoService.obtenerEntidad(77L)).thenReturn(medicoDe(77L, false));

        enviarMensaje("si");

        ArgumentCaptor<ReservarParaPacienteRequest> captor = ArgumentCaptor.forClass(ReservarParaPacienteRequest.class);
        verify(turnoService).reservarParaPaciente(eq(999L), captor.capture(), eq(bot));
        assertThat(captor.getValue().dni()).isEqualTo(paciente.getDni());
        assertThat(ultimoMensajeEnviado()).contains("confirmado");
    }

    @Test
    void elAvisoDeCopagoIomaApareceSoloCuandoElMedicoLoCobraYElPacienteTieneIoma() {
        paciente.setObraSocial("IOMA");
        conversacionEn(EstadoConversacion.CONFIRMANDO_RESERVA, paciente,
                "{\"turnoId\":999,\"opcionesIds\":[],\"opcionesFechas\":[]}");
        when(turnoService.reservarParaPaciente(eq(999L), any(), eq(bot))).thenReturn(turnoAdminDtoDe(paciente.getId(), 77L, true));
        when(usuarioRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoService.obtenerEntidad(77L)).thenReturn(medicoDe(77L, true));

        enviarMensaje("si");

        assertThat(ultimoMensajeEnviado()).containsIgnoringCase("copago");
    }

    @Test
    void noHayAvisoDeCopagoSiElPacienteNoTieneIoma() {
        conversacionEn(EstadoConversacion.CONFIRMANDO_RESERVA, paciente, // obraSocial = OSDE, ver setUp
                "{\"turnoId\":999,\"opcionesIds\":[],\"opcionesFechas\":[]}");
        when(turnoService.reservarParaPaciente(eq(999L), any(), eq(bot))).thenReturn(turnoAdminDtoDe(paciente.getId(), 77L, true));
        when(usuarioRepository.findById(paciente.getId())).thenReturn(Optional.of(paciente));
        when(medicoService.obtenerEntidad(77L)).thenReturn(medicoDe(77L, true));

        enviarMensaje("si");

        assertThat(ultimoMensajeEnviado()).doesNotContainIgnoringCase("copago");
    }

    private static TurnoAdminDto turnoAdminDtoDe(Long pacienteId, Long medicoId, boolean cobraCopago) {
        return new TurnoAdminDto(999L, medicoId, "Dra. Gómez", "Traumatología", LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(10, 30), "RESERVADO", null,
                pacienteId, "Juana Pérez", "30111222", TELEFONO, cobraCopago ? "IOMA" : "OSDE",
                "PENDIENTE", false, "SIN_CONFIRMAR", null);
    }

    private static Medico medicoDe(Long id, boolean cobraCopagoIoma) {
        Especialidad especialidad = new Especialidad();
        especialidad.setId(2L);
        especialidad.setNombre("Traumatología");
        Medico medico = new Medico();
        medico.setId(id);
        medico.setNombreCompleto("Dra. Gómez");
        medico.setEspecialidad(especialidad);
        medico.setCobraCopagoIoma(cobraCopagoIoma);
        return medico;
    }
}
