package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.EstadoResena;
import com.fktlomas.turnos.dto.resena.RechazarResenaRequest;
import com.fktlomas.turnos.dto.resena.ResenaDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.ResenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cola de moderación de reseñas de pacientes — ver ResenaService. */
@RestController
@RequestMapping("/api/admin/resenas")
@RequiredArgsConstructor
public class AdminResenaController {

    private final ResenaService resenaService;

    @GetMapping
    public List<ResenaDto> listar(@RequestParam(required = false) EstadoResena estado) {
        return resenaService.listarParaModeracion(estado);
    }

    @PutMapping("/{id}/aprobar")
    public ResenaDto aprobar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return resenaService.aprobar(id, principal.getUsuario());
    }

    @PutMapping("/{id}/rechazar")
    public ResenaDto rechazar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                               @Valid @RequestBody RechazarResenaRequest request) {
        return resenaService.rechazar(id, principal.getUsuario(), request.motivo());
    }
}
