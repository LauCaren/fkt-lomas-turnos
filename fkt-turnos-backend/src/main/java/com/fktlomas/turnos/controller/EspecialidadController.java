package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.EspecialidadDto;
import com.fktlomas.turnos.service.EspecialidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<EspecialidadDto> listar() {
        return especialidadService.listar();
    }
}
