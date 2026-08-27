package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.MedicoDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.service.MedicoService;
import com.fktlomas.turnos.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
}
