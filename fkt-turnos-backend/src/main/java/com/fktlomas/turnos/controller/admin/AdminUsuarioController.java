package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.CuentaStaffDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.AdminUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Activar/desactivar y revocar sesiones de cuentas de staff (médico, contador, secretaria). */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @PutMapping("/{id}/activar")
    public CuentaStaffDto activar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return adminUsuarioService.activar(id, principal.getUsuario());
    }

    @PutMapping("/{id}/desactivar")
    public CuentaStaffDto desactivar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return adminUsuarioService.desactivar(id, principal.getUsuario());
    }

    @PutMapping("/{id}/revocar-sesiones")
    public CuentaStaffDto revocarSesiones(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return adminUsuarioService.revocarSesiones(id, principal.getUsuario());
    }
}
