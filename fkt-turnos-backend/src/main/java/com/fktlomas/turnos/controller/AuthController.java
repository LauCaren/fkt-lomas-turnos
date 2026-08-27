package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.auth.AuthResponse;
import com.fktlomas.turnos.dto.auth.LoginRequest;
import com.fktlomas.turnos.dto.auth.RegistroRequest;
import com.fktlomas.turnos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(authService.registrarPaciente(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
