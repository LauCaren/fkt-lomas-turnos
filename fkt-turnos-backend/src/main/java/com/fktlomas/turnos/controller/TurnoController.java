package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping("/{id}/reservar")
    public ResponseEntity<TurnoDto> reservar(@PathVariable Long id,
                                              @AuthenticationPrincipal AppUserDetails principal,
                                              @Valid @RequestBody(required = false) ReservarTurnoRequest request) {
        ReservarTurnoRequest cuerpo = request != null ? request : new ReservarTurnoRequest(null);
        return ResponseEntity.ok(turnoService.reservar(id, principal.getUsuario(), cuerpo));
    }

    @GetMapping("/mios")
    public List<TurnoDto> misTurnos(@AuthenticationPrincipal AppUserDetails principal) {
        return turnoService.misTurnos(principal.getUsuario().getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        turnoService.cancelar(id, principal.getUsuario());
        return ResponseEntity.noContent().build();
    }
}
