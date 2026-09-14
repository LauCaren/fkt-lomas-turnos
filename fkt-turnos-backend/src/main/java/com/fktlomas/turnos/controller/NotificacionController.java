package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.NotificacionDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Sin restricción de rol — cualquier usuario autenticado ve sus propias notificaciones, sobre su propia cuenta (ver NotificacionService.marcarLeida). */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public Page<NotificacionDto> mias(@AuthenticationPrincipal AppUserDetails principal,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return notificacionService.mias(principal.getUsuario().getId(), pageable);
    }

    // Separado de mias() a propósito: la campanita del header pollea solo este número cada
    // tanto (ver useNotificaciones.js) sin traer ni paginar la lista completa cada vez.
    @GetMapping("/no-leidas/cantidad")
    public Map<String, Long> cantidadNoLeidas(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("cantidad", notificacionService.contarNoLeidas(principal.getUsuario().getId()));
    }

    @PutMapping("/{id}/leida")
    public void marcarLeida(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        notificacionService.marcarLeida(id, principal.getUsuario().getId());
    }

    @PutMapping("/marcar-todas-leidas")
    public void marcarTodasLeidas(@AuthenticationPrincipal AppUserDetails principal) {
        notificacionService.marcarTodasLeidas(principal.getUsuario().getId());
    }
}
