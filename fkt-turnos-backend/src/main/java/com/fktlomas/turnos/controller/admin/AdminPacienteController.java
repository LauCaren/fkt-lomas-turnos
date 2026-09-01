package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.admin.FichaPacienteDto;
import com.fktlomas.turnos.service.AdminPacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pacientes")
@RequiredArgsConstructor
public class AdminPacienteController {

    private final AdminPacienteService adminPacienteService;

    @GetMapping("/buscar")
    public List<PacienteBusquedaDto> buscar(@RequestParam(required = false) String q) {
        return adminPacienteService.buscar(q);
    }

    @GetMapping("/{id}/ficha")
    public FichaPacienteDto ficha(@PathVariable Long id) {
        return adminPacienteService.ficha(id);
    }
}
