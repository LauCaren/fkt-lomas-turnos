package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirmación de asistencia del día anterior — a propósito sin login: el paciente hace clic
 * desde el mail/WhatsApp del recordatorio. El token (UUID no adivinable) es la credencial.
 *
 * <p>POST y no GET: el link del mensaje abre la página del frontend (ConfirmarTurnoPage), que
 * es la que llama a este endpoint — y para cancelar pide un clic explícito. Así un antivirus de
 * correo o una vista previa que "abre" el link para revisarlo no puede cancelar el turno solo.
 */
@RestController
@RequestMapping("/api/turnos/confirmacion")
@RequiredArgsConstructor
public class ConfirmacionTurnoController {

    private final TurnoService turnoService;

    @PostMapping
    public ResponseEntity<Void> confirmar(@RequestParam String token, @RequestParam String respuesta) {
        turnoService.confirmarPorToken(token, "si".equalsIgnoreCase(respuesta));
        return ResponseEntity.noContent().build();
    }
}
