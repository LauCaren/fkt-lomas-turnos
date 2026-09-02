package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.CrearSecretariaRequest;
import com.fktlomas.turnos.dto.admin.CuentaStaffDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.SecretariaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Alta de cuentas de secretaria — solo Fernando (ADMIN) puede crear o listar estas cuentas. */
@RestController
@RequestMapping("/api/admin/secretarias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSecretariaController {

    private final SecretariaService secretariaService;

    @GetMapping
    public List<CuentaStaffDto> listar() {
        return secretariaService.listar();
    }

    @PostMapping
    public ResponseEntity<CuentaStaffDto> crear(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody CrearSecretariaRequest request) {
        return ResponseEntity.ok(secretariaService.crear(request, principal.getUsuario()));
    }
}
