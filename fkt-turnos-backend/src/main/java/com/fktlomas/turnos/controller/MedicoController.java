package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.MedicoDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.service.MedicoService;
import com.fktlomas.turnos.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoService medicoService;
    private final TurnoService turnoService;

    @GetMapping
    public List<MedicoDto> listar(@RequestParam(required = false) Long especialidadId) {
        return medicoService.listar(especialidadId);
    }

    @GetMapping("/{id}/turnos-disponibles")
    public List<TurnoDto> turnosDisponibles(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.disponiblesDeMedico(id, fecha);
    }

    /**
     * Público a propósito (bajo /api/medicos/**, ya permitAll en SecurityConfig):
     * la ficha del profesional en el sitio de marketing (equipo.html) necesita
     * poder mostrar la foto sin que el visitante esté logueado.
     */
    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) {
        Resource recurso = medicoService.obtenerFoto(id);
        return ResponseEntity.ok().contentType(contentTypeDe(medicoService.obtenerFotoContentType(id))).body(recurso);
    }

    @GetMapping("/{id}/titulo")
    public ResponseEntity<Resource> titulo(@PathVariable Long id) {
        Resource recurso = medicoService.obtenerTitulo(id);
        return ResponseEntity.ok().contentType(contentTypeDe(medicoService.obtenerTituloContentType(id))).body(recurso);
    }

    private MediaType contentTypeDe(String contentType) {
        return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
    }
}
