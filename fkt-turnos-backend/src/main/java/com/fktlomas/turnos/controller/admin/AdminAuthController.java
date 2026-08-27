package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.CambiarPasswordRequest;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal AppUserDetails principal,
                                                 @Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(principal.getUsuario(), request);
        return ResponseEntity.ok().build();
    }
}
