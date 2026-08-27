package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.MensajeContactoDto;
import com.fktlomas.turnos.service.ContactoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mensajes-contacto")
@RequiredArgsConstructor
public class AdminContactoController {

    private final ContactoService contactoService;

    @GetMapping
    public List<MensajeContactoDto> listar() {
        return contactoService.listar();
    }

    @PatchMapping("/{id}/leido")
    public MensajeContactoDto marcarLeido(@PathVariable Long id) {
        return contactoService.marcarLeido(id);
    }
}
