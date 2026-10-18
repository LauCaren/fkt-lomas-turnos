package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.EstadoArrepentimiento;
import com.fktlomas.turnos.dto.arrepentimiento.ResolverArrepentimientoRequest;
import com.fktlomas.turnos.dto.arrepentimiento.SolicitudArrepentimientoDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.ArrepentimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Bandeja de pedidos del "Botón de arrepentimiento" — admin y secretaría (ver ArrepentimientoService). */
@RestController
@RequestMapping("/api/admin/arrepentimientos")
@RequiredArgsConstructor
public class AdminArrepentimientoController {

    private final ArrepentimientoService arrepentimientoService;

    @GetMapping
    public List<SolicitudArrepentimientoDto> listar(@RequestParam(required = false) EstadoArrepentimiento estado) {
        return arrepentimientoService.listar(estado);
    }

    @PutMapping("/{id}/resolver")
    public SolicitudArrepentimientoDto resolver(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                                @Valid @RequestBody ResolverArrepentimientoRequest request) {
        return arrepentimientoService.resolver(id, principal.getUsuario(), request.notaResolucion());
    }
}
