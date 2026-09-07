package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.CheckInDto;
import com.fktlomas.turnos.dto.ListaEsperaDto;
import com.fktlomas.turnos.dto.ListaEsperaRequest;
import com.fktlomas.turnos.dto.ReservarTurnoRequest;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.pago.PagoOnlineDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.ListaEsperaService;
import com.fktlomas.turnos.service.MercadoPagoService;
import com.fktlomas.turnos.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;
    private final ListaEsperaService listaEsperaService;
    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/{id}/reservar")
    public ResponseEntity<TurnoDto> reservar(@PathVariable Long id,
                                              @AuthenticationPrincipal AppUserDetails principal,
                                              @Valid @RequestBody(required = false) ReservarTurnoRequest request) {
        ReservarTurnoRequest cuerpo = request != null ? request : new ReservarTurnoRequest(null);
        return ResponseEntity.ok(turnoService.reservar(id, principal.getUsuario(), cuerpo));
    }

    /** Paso opcional después de reservar — el turno ya queda confirmado sin esto, se puede pagar en el consultorio igual. */
    @PostMapping("/{id}/pago-online")
    public PagoOnlineDto pagoOnline(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        return mercadoPagoService.crearPreferencia(id, principal.getUsuario());
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

    @PostMapping("/lista-espera")
    public ListaEsperaDto anotarseListaEspera(@AuthenticationPrincipal AppUserDetails principal,
                                               @Valid @RequestBody ListaEsperaRequest request) {
        return listaEsperaService.anotarse(principal.getUsuario(), request.medicoId(), request.fecha());
    }

    @GetMapping("/lista-espera")
    public ResponseEntity<ListaEsperaDto> miAnotacionListaEspera(@AuthenticationPrincipal AppUserDetails principal,
                                                                  @RequestParam Long medicoId, @RequestParam LocalDate fecha) {
        return listaEsperaService.miAnotacion(principal.getUsuario().getId(), medicoId, fecha)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/lista-espera/{id}")
    public ResponseEntity<Void> darseDeBajaListaEspera(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
        listaEsperaService.darseDeBaja(id, principal.getUsuario());
        return ResponseEntity.noContent().build();
    }

    /** Lo dispara el propio paciente al escanear el QR de la sala de espera. */
    @PostMapping("/checkin")
    public CheckInDto checkIn(@AuthenticationPrincipal AppUserDetails principal) {
        return turnoService.checkInPorQr(principal.getUsuario());
    }
}
