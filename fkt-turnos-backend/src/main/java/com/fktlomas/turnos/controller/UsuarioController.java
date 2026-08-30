package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.ActualizarPerfilRequest;
import com.fktlomas.turnos.dto.PerfilDto;
import com.fktlomas.turnos.dto.admin.CambiarPasswordRequest;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.AuthService;
import com.fktlomas.turnos.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mi-cuenta")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    @GetMapping
    public PerfilDto miPerfil(@AuthenticationPrincipal AppUserDetails principal) {
        return PerfilDto.de(principal.getUsuario());
    }

    @PutMapping
    public PerfilDto actualizarPerfil(@AuthenticationPrincipal AppUserDetails principal,
                                       @Valid @RequestBody ActualizarPerfilRequest request) {
        return usuarioService.actualizarPerfil(principal.getUsuario(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal AppUserDetails principal,
                                                 @Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(principal.getUsuario(), request);
        return ResponseEntity.ok().build();
    }
}
