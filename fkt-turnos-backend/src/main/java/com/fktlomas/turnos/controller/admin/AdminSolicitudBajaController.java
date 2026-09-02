package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.EstadoSolicitudBaja;
import com.fktlomas.turnos.dto.admin.RechazarSolicitudBajaRequest;
import com.fktlomas.turnos.dto.admin.SolicitudBajaDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.SolicitudBajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cola de pedidos de baja de cuenta de pacientes (derecho de supresión, Ley 25.326) — ver SolicitudBajaService. */
@RestController
@RequestMapping("/api/admin/solicitudes-baja")
@RequiredArgsConstructor
public class AdminSolicitudBajaController {

    private final SolicitudBajaService solicitudBajaService;

    @GetMapping
    public List<SolicitudBajaDto> listar(@RequestParam(required = false) EstadoSolicitudBaja estado) {
        return solicitudBajaService.listar(estado);
    }

    @PutMapping("/{id}/aprobar")
    public SolicitudBajaDto aprobar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return solicitudBajaService.aprobar(id, principal.getUsuario());
    }

    @PutMapping("/{id}/rechazar")
    public SolicitudBajaDto rechazar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                      @Valid @RequestBody RechazarSolicitudBajaRequest request) {
        return solicitudBajaService.rechazar(id, principal.getUsuario(), request.motivo());
    }
}
