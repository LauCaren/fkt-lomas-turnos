package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.EstadoPagoOnline;
import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Ingreso;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.PagoOnline;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.pago.PagoOnlineDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.IngresoRepository;
import com.fktlomas.turnos.repository.PagoOnlineRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.security.RateLimiterService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unidad pura con Mockito (no @SpringBootTest): a diferencia del resto de los tests de este
 * proyecto, acá no hay nada que ganar levantando el contexto completo — PreferenceClient y
 * PaymentClient (ver MercadoPagoClientConfig) son las únicas dos cosas que hablan con una API
 * externa, y se mockean directo.
 */
@ExtendWith(MockitoExtension.class)
class MercadoPagoServiceTest {

    private static final String WEBHOOK_SECRET = "un-secreto-de-prueba";

    // Payment/Preference tienen varios campos java.time.* que Gson no puede leer por reflexión
    // en Java 17 sin --add-opens (java.base no abre java.time al módulo sin nombre) — como estos
    // fixtures de test nunca los necesitan, deserializadores que los dejan en null evitan que
    // Gson intente construirles un adaptador reflexivo.
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<OffsetDateTime>) (json, type, ctx) -> null)
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, ctx) -> null)
            .registerTypeAdapter(java.time.LocalDateTime.class, (JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) -> null)
            .create();

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private PagoOnlineRepository pagoOnlineRepository;
    @Mock
    private IngresoRepository ingresoRepository;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private PreferenceClient preferenceClient;
    @Mock
    private PaymentClient paymentClient;

    private MercadoPagoService service;
    private Usuario paciente;
    private Medico medico;
    private Turno turno;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoService(turnoRepository, pagoOnlineRepository, ingresoRepository,
                rateLimiterService, preferenceClient, paymentClient);
        ReflectionTestUtils.setField(service, "accessToken", "TEST-token");
        ReflectionTestUtils.setField(service, "webhookSecret", WEBHOOK_SECRET);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "backendUrl", "http://localhost:8080");
        ReflectionTestUtils.invokeMethod(service, "inicializar");

        Especialidad especialidad = new Especialidad();
        especialidad.setId(1L);
        especialidad.setNombre("Traumatología");
        especialidad.setEsKinesiologia(false);

        medico = new Medico();
        medico.setId(10L);
        medico.setNombreCompleto("Dr. Test");
        medico.setEspecialidad(especialidad);
        medico.setTarifaConsulta(new BigDecimal("15000"));

        paciente = new Usuario();
        paciente.setId(100L);

        turno = new Turno();
        turno.setId(500L);
        turno.setMedico(medico);
        turno.setPaciente(paciente);
        turno.setEstado(EstadoTurno.RESERVADO);
        turno.setFecha(LocalDate.now());
        turno.setHoraInicio(LocalTime.of(10, 0));

        lenient().when(rateLimiterService.permitir(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void sinAccessTokenConfiguradoElServicioQuedaDeshabilitado() {
        MercadoPagoService deshabilitado = new MercadoPagoService(turnoRepository, pagoOnlineRepository,
                ingresoRepository, rateLimiterService, preferenceClient, paymentClient);
        ReflectionTestUtils.setField(deshabilitado, "accessToken", "");
        ReflectionTestUtils.invokeMethod(deshabilitado, "inicializar");

        assertThat(deshabilitado.estaHabilitado()).isFalse();
        assertThatThrownBy(() -> deshabilitado.crearPreferencia(500L, paciente))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noSeCreaPreferenciaParaUnTurnoDeOtroPaciente() throws Exception {
        Usuario otroPaciente = new Usuario();
        otroPaciente.setId(999L);
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));

        assertThatThrownBy(() -> service.crearPreferencia(500L, otroPaciente))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(preferenceClient, never()).create(any());
    }

    @Test
    void noSeCreaPreferenciaParaUnTurnoNoReservado() {
        turno.setEstado(EstadoTurno.CANCELADO);
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));

        assertThatThrownBy(() -> service.crearPreferencia(500L, paciente))
                .isInstanceOf(ConflictoDeDatosException.class);
    }

    @Test
    void noSeCreaPreferenciaParaUnTurnoDeKinesiologia() {
        medico.getEspecialidad().setEsKinesiologia(true);
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));

        assertThatThrownBy(() -> service.crearPreferencia(500L, paciente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kinesiología");
    }

    @Test
    void noSeCreaPreferenciaSinTarifaDeConsultaCargada() {
        medico.setTarifaConsulta(null);
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));

        assertThatThrownBy(() -> service.crearPreferencia(500L, paciente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tarifa");
    }

    @Test
    void noSeCreaUnSegundoPagoSiYaHayUnoPendienteOAprobadoParaElMismoTurno() throws Exception {
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));
        when(pagoOnlineRepository.existsByTurnoIdAndEstadoIn(500L,
                List.of(EstadoPagoOnline.PENDIENTE, EstadoPagoOnline.APROBADO))).thenReturn(true);

        assertThatThrownBy(() -> service.crearPreferencia(500L, paciente))
                .isInstanceOf(ConflictoDeDatosException.class);
        verify(preferenceClient, never()).create(any());
    }

    @Test
    void creaLaPreferenciaConElExternalReferenceDelTurnoYPersisteElPagoPendiente() throws Exception {
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));
        when(pagoOnlineRepository.existsByTurnoIdAndEstadoIn(500L,
                List.of(EstadoPagoOnline.PENDIENTE, EstadoPagoOnline.APROBADO))).thenReturn(false);
        Preference preferenceCreada = GSON.fromJson(
                "{\"id\":\"pref-123\",\"initPoint\":\"https://mp.example/checkout/pref-123\"}", Preference.class);
        when(preferenceClient.create(any())).thenReturn(preferenceCreada);
        when(pagoOnlineRepository.save(any(PagoOnline.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoOnlineDto dto = service.crearPreferencia(500L, paciente);

        assertThat(dto.initPoint()).isEqualTo("https://mp.example/checkout/pref-123");
        assertThat(dto.monto()).isEqualByComparingTo(new BigDecimal("15000"));

        var captor = org.mockito.ArgumentCaptor.forClass(PreferenceRequest.class);
        verify(preferenceClient).create(captor.capture());
        assertThat(captor.getValue().getExternalReference()).isEqualTo("500");

        var pagoCaptor = org.mockito.ArgumentCaptor.forClass(PagoOnline.class);
        verify(pagoOnlineRepository).save(pagoCaptor.capture());
        assertThat(pagoCaptor.getValue().getPreferenceId()).isEqualTo("pref-123");
        assertThat(pagoCaptor.getValue().getEstado()).isEqualTo(EstadoPagoOnline.PENDIENTE);
    }

    @Test
    void unaFallaDeMercadoPagoAlCrearLaPreferenciaSeConvierteEnUnErrorClaroParaElPaciente() throws Exception {
        when(turnoRepository.findById(500L)).thenReturn(Optional.of(turno));
        when(preferenceClient.create(any())).thenThrow(new MPException("timeout"));

        assertThatThrownBy(() -> service.crearPreferencia(500L, paciente))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------- Webhook ----------

    @Test
    void elWebhookIgnoraNotificacionesQueNoSonDePago() {
        service.procesarNotificacion("merchant_order", "1", firmaValida("1", "req-1"), "req-1");

        verifyNoMercadoPagoCallMade();
    }

    @Test
    void elWebhookSeRechazaSinWebhookSecretConfigurado() {
        ReflectionTestUtils.setField(service, "webhookSecret", "");

        assertThatThrownBy(() -> service.procesarNotificacion("payment", "1", "ts=1,v1=abc", "req-1"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoMercadoPagoCallMade();
    }

    @Test
    void elWebhookSeRechazaConUnaFirmaQueNoCoincide() {
        assertThatThrownBy(() -> service.procesarNotificacion("payment", "1", "ts=1700000000,v1=firmafalsa", "req-1"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoMercadoPagoCallMade();
    }

    @Test
    void unPagoAprobadoConFirmaValidaGeneraElIngresoYQuedaAsociado() throws Exception {
        PagoOnline pagoPendiente = pagoPendienteDe(turno, paciente);
        when(pagoOnlineRepository.findByTurnoIdOrderByCreatedAtDesc(500L)).thenReturn(List.of(pagoPendiente));
        when(pagoOnlineRepository.save(any(PagoOnline.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ingresoRepository.save(any(Ingreso.class))).thenAnswer(inv -> inv.getArgument(0));
        Payment pagoAprobado = GSON.fromJson(
                "{\"id\":777,\"status\":\"approved\",\"externalReference\":\"500\"}", Payment.class);
        when(paymentClient.get(777L)).thenReturn(pagoAprobado);

        service.procesarNotificacion("payment", "777", firmaValida("777", "req-2"), "req-2");

        assertThat(pagoPendiente.getEstado()).isEqualTo(EstadoPagoOnline.APROBADO);
        assertThat(pagoPendiente.getPaymentId()).isEqualTo("777");
        assertThat(pagoPendiente.getIngreso()).isNotNull();
        assertThat(pagoPendiente.getIngreso().getMetodoPago().name()).isEqualTo("MERCADOPAGO");
        assertThat(pagoPendiente.getIngreso().getRegistradoPor()).isNull();
        verify(ingresoRepository).save(any(Ingreso.class));
    }

    @Test
    void unPagoRechazadoNoGeneraIngreso() throws Exception {
        PagoOnline pagoPendiente = pagoPendienteDe(turno, paciente);
        when(pagoOnlineRepository.findByTurnoIdOrderByCreatedAtDesc(500L)).thenReturn(List.of(pagoPendiente));
        when(pagoOnlineRepository.save(any(PagoOnline.class))).thenAnswer(inv -> inv.getArgument(0));
        Payment pagoRechazado = GSON.fromJson(
                "{\"id\":778,\"status\":\"rejected\",\"externalReference\":\"500\"}", Payment.class);
        when(paymentClient.get(778L)).thenReturn(pagoRechazado);

        service.procesarNotificacion("payment", "778", firmaValida("778", "req-3"), "req-3");

        assertThat(pagoPendiente.getEstado()).isEqualTo(EstadoPagoOnline.RECHAZADO);
        assertThat(pagoPendiente.getIngreso()).isNull();
        verify(ingresoRepository, never()).save(any());
    }

    @Test
    void unReintentoDeLaMismaNotificacionYaAprobadaEsIdempotente() throws Exception {
        PagoOnline pagoYaAprobado = pagoPendienteDe(turno, paciente);
        pagoYaAprobado.setEstado(EstadoPagoOnline.APROBADO);
        pagoYaAprobado.setPaymentId("777");
        pagoYaAprobado.setIngreso(new Ingreso());
        // dataId (el payment_id que manda el webhook) alcanza para detectar el reintento, sin
        // necesidad de volver a pedirle el pago a Mercado Pago — ver MercadoPagoService.procesarNotificacion.
        when(pagoOnlineRepository.findByPaymentId("777")).thenReturn(Optional.of(pagoYaAprobado));

        service.procesarNotificacion("payment", "777", firmaValida("777", "req-4"), "req-4");

        verify(paymentClient, never()).get(anyLong());
        verify(ingresoRepository, never()).save(any());
    }

    private void verifyNoMercadoPagoCallMade() {
        try {
            verify(paymentClient, never()).get(anyLong());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static PagoOnline pagoPendienteDe(Turno turno, Usuario paciente) {
        PagoOnline pago = new PagoOnline();
        pago.setTurno(turno);
        pago.setPaciente(paciente);
        pago.setMonto(new BigDecimal("15000"));
        pago.setPreferenceId("pref-abc");
        pago.setEstado(EstadoPagoOnline.PENDIENTE);
        return pago;
    }

    /** Mismo algoritmo que MercadoPagoService.validarFirma, para poder generar una firma que sí pase. */
    private static String firmaValida(String dataId, String requestId) {
        long ts = 1700000000L;
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String v1 = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            return "ts=" + ts + ",v1=" + v1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
