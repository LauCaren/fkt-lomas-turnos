package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.PostulacionDto;
import com.fktlomas.turnos.service.PostulacionService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/postulaciones")
@RequiredArgsConstructor
public class PostulacionController {

    private final PostulacionService postulacionService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PostulacionDto> postular(
            @RequestParam @NotBlank String nombreCompleto,
            @RequestParam @NotBlank String email,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String mensaje,
            @RequestParam("cv") MultipartFile cv) {
        return ResponseEntity.ok(postulacionService.recibir(nombreCompleto, email, telefono, mensaje, cv));
    }
}
