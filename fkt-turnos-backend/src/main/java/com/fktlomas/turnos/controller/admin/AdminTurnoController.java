package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/turnos")
@RequiredArgsConstructor
public class AdminTurnoController {

    private final TurnoService turnoService;

    @GetMapping
    public List<TurnoAdminDto> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long especialidadId) {
        return turnoService.agenda(desde, hasta, medicoId, especialidadId);
    }

    @PostMapping("/{id}/reservar-para-paciente")
    public TurnoAdminDto reservarParaPaciente(@PathVariable Long id, @Valid @RequestBody ReservarParaPacienteRequest request) {
        return turnoService.reservarParaPaciente(id, request);
    }
}
