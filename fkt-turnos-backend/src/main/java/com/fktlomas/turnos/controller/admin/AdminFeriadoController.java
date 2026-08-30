package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.FeriadoDto;
import com.fktlomas.turnos.dto.admin.FeriadoRequest;
import com.fktlomas.turnos.service.FeriadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/feriados")
@RequiredArgsConstructor
public class AdminFeriadoController {

    private final FeriadoService feriadoService;

    @GetMapping
    public List<FeriadoDto> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return feriadoService.listar(desde, hasta);
    }

    @PostMapping
    public FeriadoDto crear(@Valid @RequestBody FeriadoRequest request) {
        return feriadoService.crear(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        feriadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
