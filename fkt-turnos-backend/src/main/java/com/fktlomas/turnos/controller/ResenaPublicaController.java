package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.resena.ResenaPublicaDto;
import com.fktlomas.turnos.service.ResenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Público (ver SecurityConfig) — últimas reseñas aprobadas de cualquier médico, para el sitio de marketing (FKT Pagina/opiniones.html). Las de un médico puntual están en /api/medicos/{id}/resenas. */
@RestController
@RequestMapping("/api/resenas")
@RequiredArgsConstructor
public class ResenaPublicaController {

    private static final int LIMITE_MAXIMO = 50;

    private final ResenaService resenaService;

    @GetMapping
    public List<ResenaPublicaDto> ultimas(@RequestParam(defaultValue = "12") int limite) {
        return resenaService.ultimasAprobadas(Math.min(limite, LIMITE_MAXIMO));
    }
}
