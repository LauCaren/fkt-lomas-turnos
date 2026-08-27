package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.ContactoRequest;
import com.fktlomas.turnos.service.ContactoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacto")
@RequiredArgsConstructor
public class ContactoController {

    private final ContactoService contactoService;

    @PostMapping
    public ResponseEntity<Void> enviar(@Valid @RequestBody ContactoRequest request) {
        if (request.pareceSpam()) {
            return ResponseEntity.ok().build();
        }
        contactoService.recibirMensaje(request);
        return ResponseEntity.ok().build();
    }
}
