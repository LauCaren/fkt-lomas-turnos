package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoPagoOnline;
import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Ingreso;
import com.fktlomas.turnos.domain.MetodoPago;
import com.fktlomas.turnos.domain.PagoOnline;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.pago.PagoOnlineDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.DemasiadosIntentosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.IngresoRepository;
import com.fktlomas.turnos.repository.PagoOnlineRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.security.RateLimiterService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Pago online de una consulta médica común al reservar, vía Checkout Pro de Mercado Pago.
 * Deliberadamente fuera de alcance: kinesiología (flujo propio de orden médica + plan de
 * sesiones, sin tarifa fija por turno) — ver Especialidad.esKinesiologia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private static final int MAX_PREFERENCIAS_POR_HORA = 5;
    private static final List<EstadoPagoOnline> ESTADOS_QUE_BLOQUEAN_NUEVO_INTENTO =
            List.of(EstadoPagoOnline.PENDIENTE, EstadoPagoOnline.APROBADO);

    private final TurnoRepository turnoRepository;
    private final PagoOnlineRepository pagoOnlineRepository;
    private final IngresoRepository ingresoRepository;
    private final RateLimiterService rateLimiterService;
    private final PreferenceClient preferenceClient;
    private final PaymentClient paymentClient;

    @Value("${app.mercadopago.access-token}")
    private String accessToken;

    @Value("${app.mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    private boolean habilitado;

    @PostConstruct
    void inicializar() {
        habilitado = !accessToken.isBlank();
        if (habilitado) {
            MercadoPagoConfig.setAccessToken(accessToken);
        } else {
            log.info("Mercado Pago deshabilitado: falta configurar MERCADOPAGO_ACCESS_TOKEN — el pago online al reservar queda oculto, se sigue pudiendo pagar en el consultorio");
        }
    }

    public boolean estaHabilitado() {
        return habilitado;
    }

    @Transactional
    public PagoOnlineDto crearPreferencia(Long turnoId, Usuario paciente) {
        if (!habilitado) {
            throw new IllegalStateException("El pago online no está disponible en este momento");
        }
        if (!rateLimiterService.permitir("pago-online|" + paciente.getId(), MAX_PREFERENCIAS_POR_HORA, Duration.ofHours(1))) {
            throw new DemasiadosIntentosException("Demasiados intentos de pago. Probá de nuevo más tarde.");
        }

        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (turno.getPaciente() == null || !turno.getPaciente().getId().equals(paciente.getId())) {
            throw new RecursoNoEncontradoException("Turno no encontrado");
        }
        if (turno.getEstado() != EstadoTurno.RESERVADO) {
            throw new ConflictoDeDatosException("Este turno no está reservado");
        }
        if (turno.getMedico().getEspecialidad().isEsKinesiologia()) {
            throw new IllegalArgumentException("El pago online no está disponible para turnos de kinesiología");
        }
        BigDecimal monto = turno.getMedico().getTarifaConsulta();
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("Este médico todavía no tiene cargada una tarifa de consulta");
        }
        if (pagoOnlineRepository.existsByTurnoIdAndEstadoIn(turnoId, ESTADOS_QUE_BLOQUEAN_NUEVO_INTENTO)) {
            throw new ConflictoDeDatosException("Ya hay un pago en curso o confirmado para este turno");
        }

        Preference preference = crearPreferenciaEnMercadoPago(turno, monto);

        PagoOnline pago = new PagoOnline();
        pago.setTurno(turno);
        pago.setPaciente(paciente);
        pago.setMonto(monto);
        pago.setPreferenceId(preference.getId());
        pagoOnlineRepository.save(pago);

        return PagoOnlineDto.de(pago, preference.getInitPoint());
    }

    private Preference crearPreferenciaEnMercadoPago(Turno turno, BigDecimal monto) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Consulta con " + turno.getMedico().getNombreCompleto())
                .description(turno.getMedico().getEspecialidad().getNombre() + " — " + turno.getFecha() + " " + turno.getHoraInicio())
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(monto)
                .build();

        PreferenceRequest.PreferenceRequestBuilder request = PreferenceRequest.builder()
                .items(List.of(item))
                .externalReference(String.valueOf(turno.getId()))
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(frontendUrl + "/turnos/mis-turnos?pago=exito")
                        .pending(frontendUrl + "/turnos/mis-turnos?pago=pendiente")
                        .failure(frontendUrl + "/turnos/mis-turnos?pago=fallo")
                        .build())
                .autoReturn("approved");
        if (!backendUrl.isBlank()) {
            request.notificationUrl(backendUrl + "/api/pagos/mercadopago/webhook");
        }

        try {
            return preferenceClient.create(request.build());
        } catch (MPException | MPApiException e) {
            log.error("Fallo al crear la preferencia de pago en Mercado Pago para el turno {}", turno.getId(), e);
            throw new IllegalStateException("No pudimos iniciar el pago online, probá de nuevo en unos minutos");
        }
    }

    /**
     * Sin la firma secreta configurada, cualquiera que le pegue a esta URL pública podría
     * simular un pago "aprobado" y generar un Ingreso falso — por eso, a diferencia de otras
     * integraciones de este proyecto, acá NO se degrada con gracia: sin webhook-secret
     * configurado, toda notificación se rechaza.
     */
    @Transactional
    public void procesarNotificacion(String tipo, String dataId, String xSignature, String xRequestId) {
        if (!habilitado || !"payment".equalsIgnoreCase(tipo) || dataId == null || dataId.isBlank()) {
            return;
        }
        validarFirma(dataId, xSignature, xRequestId);

        // dataId ES el payment_id de Mercado Pago — un reintento de una notificación ya aplicada
        // se puede descartar sin volver a consultar la API externa.
        if (pagoOnlineRepository.findByPaymentId(dataId).filter(p -> p.getEstado() == EstadoPagoOnline.APROBADO).isPresent()) {
            return;
        }

        Payment payment = obtenerPago(dataId);
        Long turnoId = idDeTurno(payment.getExternalReference());
        if (turnoId == null) {
            log.warn("Notificación de Mercado Pago sin external_reference utilizable, paymentId={}", payment.getId());
            return;
        }

        PagoOnline pago = pagoOnlineRepository.findByTurnoIdOrderByCreatedAtDesc(turnoId).stream().findFirst()
                .orElse(null);
        if (pago == null) {
            log.warn("Notificación de Mercado Pago para el turno {} sin PagoOnline asociado, paymentId={}", turnoId, payment.getId());
            return;
        }
        if (pago.getEstado() == EstadoPagoOnline.APROBADO) {
            return; // Ya procesado — reintento de la misma notificación (MP puede reenviarla varias veces).
        }

        String estadoMp = payment.getStatus();
        pago.setPaymentId(String.valueOf(payment.getId()));
        if ("approved".equalsIgnoreCase(estadoMp) && !montoCoincide(payment, pago)) {
            // Defensa en profundidad: el pago se consulta a la API de Mercado Pago con nuestro
            // access token (no se confía en lo que trae la notificación), pero igual se compara
            // monto y moneda contra lo que se cobró al crear la preferencia antes de registrar
            // un ingreso en caja. Si no coinciden queda PENDIENTE para revisión manual.
            log.warn("Pago aprobado de Mercado Pago con monto/moneda distintos a lo esperado: paymentId={}, turno={}, esperado={} ARS, recibido={} {}",
                    payment.getId(), turnoId, pago.getMonto(), payment.getTransactionAmount(), payment.getCurrencyId());
            pagoOnlineRepository.save(pago);
            return;
        }
        if ("approved".equalsIgnoreCase(estadoMp)) {
            pago.setEstado(EstadoPagoOnline.APROBADO);
            pago.setIngreso(registrarIngresoDePago(pago));
        } else if ("rejected".equalsIgnoreCase(estadoMp)) {
            pago.setEstado(EstadoPagoOnline.RECHAZADO);
        } else if ("cancelled".equalsIgnoreCase(estadoMp)) {
            pago.setEstado(EstadoPagoOnline.CANCELADO);
        }
        // "pending"/"in_process"/"authorized": se deja en PENDIENTE, solo se actualiza paymentId.
        pagoOnlineRepository.save(pago);
    }

    private static boolean montoCoincide(Payment payment, PagoOnline pago) {
        return payment.getTransactionAmount() != null
                && payment.getTransactionAmount().compareTo(pago.getMonto()) >= 0
                && (payment.getCurrencyId() == null || "ARS".equalsIgnoreCase(payment.getCurrencyId()));
    }

    private Ingreso registrarIngresoDePago(PagoOnline pago) {
        Ingreso ingreso = new Ingreso();
        ingreso.setMedico(pago.getTurno().getMedico());
        ingreso.setTurno(pago.getTurno());
        ingreso.setMonto(pago.getMonto());
        ingreso.setMetodoPago(MetodoPago.MERCADOPAGO);
        ingreso.setFecha(LocalDate.now());
        ingreso.setRegistradoPor(null); // Confirmado por el webhook, no por un miembro del staff.
        return ingresoRepository.save(ingreso);
    }

    private Payment obtenerPago(String dataId) {
        try {
            return paymentClient.get(Long.parseLong(dataId));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Notificación de Mercado Pago con data.id inválido");
        } catch (MPException | MPApiException e) {
            log.error("Fallo al consultar el pago {} en Mercado Pago", dataId, e);
            throw new IllegalStateException("No se pudo confirmar el pago con Mercado Pago");
        }
    }

    private Long idDeTurno(String externalReference) {
        try {
            return externalReference != null ? Long.valueOf(externalReference) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Formato documentado por Mercado Pago: manifest "id:{dataId};request-id:{requestId};ts:{ts};"
     * (los segmentos de id/request-id se omiten enteros si faltan), firmado HMAC-SHA256 con el
     * webhook-secret, comparado contra el valor "v1" del header x-signature ("ts=...,v1=...").
     */
    private void validarFirma(String dataId, String xSignature, String xRequestId) {
        if (webhookSecret.isBlank()) {
            throw new AccessDeniedException(
                    "Falta configurar MERCADOPAGO_WEBHOOK_SECRET — se rechaza la notificación por seguridad");
        }
        if (xSignature == null || xSignature.isBlank()) {
            throw new AccessDeniedException("Falta el header x-signature");
        }

        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.split("=", 2);
            if (kv.length != 2) continue;
            String clave = kv[0].trim();
            String valor = kv[1].trim();
            if ("ts".equals(clave)) ts = valor;
            if ("v1".equals(clave)) v1 = valor;
        }
        if (ts == null || v1 == null) {
            throw new AccessDeniedException("Header x-signature con formato inválido");
        }

        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifest.append("request-id:").append(xRequestId).append(';');
        }
        manifest.append("ts:").append(ts).append(';');

        String calculado = hmacSha256Hex(manifest.toString(), webhookSecret);
        if (!java.security.MessageDigest.isEqual(
                calculado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Firma de Mercado Pago inválida");
        }
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo calcular la firma HMAC", e);
        }
    }
}
