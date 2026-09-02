package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.EspecialidadDto;
import com.fktlomas.turnos.dto.admin.CrearEspecialidadRequest;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/especialidades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<EspecialidadDto> listar() {
        return especialidadService.listar();
    }

    @PostMapping
    public ResponseEntity<EspecialidadDto> crear(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody CrearEspecialidadRequest request) {
        return ResponseEntity.ok(especialidadService.crear(request, principal.getUsuario()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        especialidadService.eliminar(id, principal.getUsuario());
        return ResponseEntity.noContent().build();
    }
}
