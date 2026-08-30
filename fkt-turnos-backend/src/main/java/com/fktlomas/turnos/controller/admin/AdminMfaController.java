package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.CodigoMfaRequest;
import com.fktlomas.turnos.dto.admin.IniciarMfaResponse;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/mfa")
@RequiredArgsConstructor
public class AdminMfaController {

    private final AuthService authService;

    @PostMapping("/iniciar")
    public IniciarMfaResponse iniciar(@AuthenticationPrincipal AppUserDetails principal) {
        return authService.iniciarMfa(principal.getUsuario());
    }

    @PostMapping("/activar")
    public ResponseEntity<Void> activar(@AuthenticationPrincipal AppUserDetails principal,
                                         @Valid @RequestBody CodigoMfaRequest request) {
        authService.activarMfa(principal.getUsuario(), request.codigo());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/desactivar")
    public ResponseEntity<Void> desactivar(@AuthenticationPrincipal AppUserDetails principal,
                                            @Valid @RequestBody CodigoMfaRequest request) {
        authService.desactivarMfa(principal.getUsuario(), request.codigo());
        return ResponseEntity.ok().build();
    }
}
