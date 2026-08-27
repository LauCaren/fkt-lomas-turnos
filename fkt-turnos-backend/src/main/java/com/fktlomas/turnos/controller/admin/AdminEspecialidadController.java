package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.EspecialidadDto;
import com.fktlomas.turnos.dto.admin.CrearEspecialidadRequest;
import com.fktlomas.turnos.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/especialidades")
@RequiredArgsConstructor
public class AdminEspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<EspecialidadDto> listar() {
        return especialidadService.listar();
    }

    @PostMapping
    public ResponseEntity<EspecialidadDto> crear(@Valid @RequestBody CrearEspecialidadRequest request) {
        return ResponseEntity.ok(especialidadService.crear(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        especialidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
