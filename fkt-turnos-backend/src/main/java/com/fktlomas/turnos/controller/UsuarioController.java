package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.ActualizarPerfilRequest;
import com.fktlomas.turnos.dto.EvolucionPacienteDto;
import com.fktlomas.turnos.dto.ExportarDatosDto;
import com.fktlomas.turnos.dto.PerfilDto;
import com.fktlomas.turnos.dto.SolicitarBajaRequest;
import com.fktlomas.turnos.dto.admin.CambiarPasswordRequest;
import com.fktlomas.turnos.dto.admin.SolicitudBajaDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.AuthService;
import com.fktlomas.turnos.service.CajaService;
import com.fktlomas.turnos.service.SolicitudBajaService;
import com.fktlomas.turnos.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mi-cuenta")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final SolicitudBajaService solicitudBajaService;
    private final CajaService cajaService;

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

    @PutMapping("/revocar-sesiones")
    public ResponseEntity<Void> revocarMisSesiones(@AuthenticationPrincipal AppUserDetails principal) {
        usuarioService.revocarMisSesiones(principal.getUsuario());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/evoluciones")
    public List<EvolucionPacienteDto> misEvoluciones(@AuthenticationPrincipal AppUserDetails principal) {
        return usuarioService.misEvoluciones(principal.getUsuario());
    }

    @GetMapping("/exportar-datos")
    public ExportarDatosDto exportarMisDatos(@AuthenticationPrincipal AppUserDetails principal) {
        return usuarioService.exportarMisDatos(principal.getUsuario());
    }

    @PostMapping("/solicitar-baja")
    public SolicitudBajaDto solicitarBaja(@AuthenticationPrincipal AppUserDetails principal,
                                           @Valid @RequestBody(required = false) SolicitarBajaRequest request) {
        return solicitudBajaService.solicitar(principal.getUsuario(), request != null ? request : new SolicitarBajaRequest(null));
    }

    @GetMapping("/pagos")
    public List<IngresoDto> misPagos(@AuthenticationPrincipal AppUserDetails principal) {
        return cajaService.ingresosDePaciente(principal.getUsuario().getId());
    }

    @GetMapping("/pagos/{id}/comprobante")
    public ResponseEntity<byte[]> comprobante(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        byte[] pdf = cajaService.comprobantePdf(id, principal.getUsuario().getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("comprobante_" + id + ".pdf").build());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).headers(headers).body(pdf);
    }
}
