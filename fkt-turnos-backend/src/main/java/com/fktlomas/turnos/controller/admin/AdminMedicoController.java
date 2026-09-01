package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.*;
import com.fktlomas.turnos.service.DisponibilidadService;
import com.fktlomas.turnos.service.MedicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/medicos")
@RequiredArgsConstructor
public class AdminMedicoController {

    private final MedicoService medicoService;
    private final DisponibilidadService disponibilidadService;

    @GetMapping
    public List<MedicoAdminDto> listar() {
        return medicoService.listarTodosAdmin();
    }

    @PostMapping
    public ResponseEntity<MedicoAdminDto> crear(@Valid @RequestBody CrearMedicoRequest request) {
        return ResponseEntity.ok(medicoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicoAdminDto> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarMedicoRequest request) {
        return ResponseEntity.ok(medicoService.actualizar(id, request));
    }

    @PutMapping("/{id}/credenciales")
    public ResponseEntity<MedicoAdminDto> crearOActualizarCredenciales(@PathVariable Long id, @Valid @RequestBody CredencialesMedicoRequest request) {
        return ResponseEntity.ok(medicoService.crearOActualizarCredenciales(id, request));
    }

    @GetMapping("/{id}/reglas-disponibilidad")
    public List<ReglaDisponibilidadDto> listarReglas(@PathVariable Long id) {
        return disponibilidadService.listarReglas(id);
    }

    @PostMapping("/{id}/reglas-disponibilidad")
    public ResponseEntity<ReglaDisponibilidadDto> crearRegla(@PathVariable Long id, @Valid @RequestBody ReglaDisponibilidadRequest request) {
        return ResponseEntity.ok(disponibilidadService.crearRegla(id, request));
    }

    @DeleteMapping("/reglas-disponibilidad/{reglaId}")
    public ResponseEntity<Void> eliminarRegla(@PathVariable Long reglaId) {
        disponibilidadService.eliminarRegla(reglaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generar-turnos")
    public ResponseEntity<Map<String, Integer>> generarTurnos(@PathVariable Long id, @Valid @RequestBody GenerarTurnosRequest request) {
        int generados = disponibilidadService.generarTurnos(id, request.desde(), request.hasta());
        return ResponseEntity.ok(Map.of("turnosGenerados", generados));
    }

    @PostMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    public MedicoAdminDto subirFoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) {
        return medicoService.subirFoto(id, foto);
    }

    @PostMapping(value = "/{id}/titulo", consumes = "multipart/form-data")
    public MedicoAdminDto subirTitulo(@PathVariable Long id, @RequestParam("imagen") MultipartFile imagen) {
        return medicoService.subirTitulo(id, imagen);
    }
}
