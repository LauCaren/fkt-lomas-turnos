package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.caja.SaldoMedicoDto;
import com.fktlomas.turnos.dto.medico.CrearEvolucionRequest;
import com.fktlomas.turnos.dto.medico.EvolucionClinicaDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.CajaService;
import com.fktlomas.turnos.service.MedicoPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Vista propia del médico logueado — nunca expone datos de otros médicos. */
@RestController
@RequestMapping("/api/medico")
@RequiredArgsConstructor
public class MedicoPortalController {

    private final MedicoPortalService medicoPortalService;
    private final CajaService cajaService;

    @GetMapping("/agenda-hoy")
    public List<TurnoAdminDto> agendaHoy(@AuthenticationPrincipal AppUserDetails principal) {
        return medicoPortalService.agendaHoy(principal.getUsuario().getId());
    }

    @GetMapping("/cola-espera")
    public List<TurnoAdminDto> colaEspera(@AuthenticationPrincipal AppUserDetails principal) {
        return medicoPortalService.colaEspera(principal.getUsuario().getId());
    }

    @PutMapping("/turnos/{turnoId}/atendido")
    public ResponseEntity<Void> marcarAtendido(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long turnoId) {
        medicoPortalService.marcarAtendido(principal.getUsuario().getId(), turnoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/turnos/{turnoId}/evolucion")
    public EvolucionClinicaDto crearEvolucion(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long turnoId,
                                               @Valid @RequestBody CrearEvolucionRequest request) {
        return medicoPortalService.crearEvolucion(principal.getUsuario().getId(), turnoId, request);
    }

    @GetMapping("/pacientes/{pacienteId}/evoluciones")
    public List<EvolucionClinicaDto> evolucionesDePaciente(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long pacienteId) {
        return medicoPortalService.evolucionesDePaciente(principal.getUsuario(), pacienteId);
    }

    @GetMapping("/caja/saldo")
    public SaldoMedicoDto saldo(@AuthenticationPrincipal AppUserDetails principal) {
        Long medicoId = medicoPortalService.medicoDe(principal.getUsuario().getId()).getId();
        return cajaService.saldoDeMedico(medicoId);
    }
}
