package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.MensajeContactoDto;
import com.fktlomas.turnos.service.ContactoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mensajes-contacto")
@RequiredArgsConstructor
public class AdminContactoController {

    private final ContactoService contactoService;

    @GetMapping
    public Page<MensajeContactoDto> listar(@PageableDefault(size = 20) Pageable pageable) {
        return contactoService.listar(pageable);
    }

    @PatchMapping("/{id}/leido")
    public MensajeContactoDto marcarLeido(@PathVariable Long id) {
        return contactoService.marcarLeido(id);
    }
}
