package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.arrepentimiento.ArrepentimientoRecibidoDto;
import com.fktlomas.turnos.dto.arrepentimiento.ArrepentimientoRequest;
import com.fktlomas.turnos.service.ArrepentimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Público a propósito (ver SecurityConfig): la Resolución 424/2020 prohíbe pedir registro o
 * inicio de sesión para usar el botón de arrepentimiento. Lo protegen el límite por IP de
 * RateLimitingFilter, el límite por email de ArrepentimientoService y el campo honeypot.
 */
@RestController
@RequestMapping("/api/arrepentimiento")
@RequiredArgsConstructor
public class ArrepentimientoController {

    private final ArrepentimientoService arrepentimientoService;

    @PostMapping
    public ArrepentimientoRecibidoDto solicitar(@Valid @RequestBody ArrepentimientoRequest request) {
        return arrepentimientoService.recibir(request);
    }
}
