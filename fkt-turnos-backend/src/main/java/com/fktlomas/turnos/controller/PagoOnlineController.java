package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoints públicos (ver SecurityConfig): el estado lo consulta el wizard de reserva antes de loguearse necesariamente, y el webhook lo llama Mercado Pago, no un usuario. */
@RestController
@RequestMapping("/api/pagos/mercadopago")
@RequiredArgsConstructor
public class PagoOnlineController {

    private final MercadoPagoService mercadoPagoService;

    @GetMapping("/estado")
    public Map<String, Boolean> estado() {
        return Map.of("habilitado", mercadoPagoService.estaHabilitado());
    }

    /**
     * Mercado Pago reintenta hasta 8 veces si no recibe 200/201 en 22 segundos — por eso esto
     * responde 200 aun para notificaciones irrelevantes (ver MercadoPagoService.procesarNotificacion),
     * y solo deja pasar un 4xx real cuando la firma no valida (ver GlobalExceptionHandler).
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(name = "type", required = false) String type,
                                         @RequestParam(name = "topic", required = false) String topic,
                                         @RequestParam(name = "data.id", required = false) String dataId,
                                         @RequestParam(name = "id", required = false) String idLegacy,
                                         @RequestHeader(name = "x-signature", required = false) String xSignature,
                                         @RequestHeader(name = "x-request-id", required = false) String xRequestId) {
        String tipoNotificacion = type != null ? type : topic;
        String idDato = dataId != null ? dataId : idLegacy;
        mercadoPagoService.procesarNotificacion(tipoNotificacion, idDato, xSignature, xRequestId);
        return ResponseEntity.ok().build();
    }
}
