package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.ListaEsperaAdminDto;
import com.fktlomas.turnos.service.ListaEsperaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/lista-espera")
@RequiredArgsConstructor
public class AdminListaEsperaController {

    private final ListaEsperaService listaEsperaService;

    /** Pacientes anotados para este médico/día — para ofrecerles un horario recién liberado. */
    @GetMapping
    public List<ListaEsperaAdminDto> compatiblesParaSlot(
            @RequestParam Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return listaEsperaService.compatiblesParaSlot(medicoId, fecha);
    }
}
