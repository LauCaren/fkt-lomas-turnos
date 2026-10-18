package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.service.PostulacionService;
import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/postulaciones")
@RequiredArgsConstructor
public class PostulacionController {

    private final PostulacionService postulacionService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> postular(
            @RequestParam @NotBlank @Size(max = 150) String nombreCompleto,
            @RequestParam @NotBlank @Email(message = "El email no es válido") String email,
            @RequestParam(required = false) @Pattern(regexp = Patrones.TELEFONO_OPCIONAL, message = "El teléfono no es válido") String telefono,
            @RequestParam(required = false) @Size(max = 2000) String mensaje,
            @RequestParam(required = false) String sitioWeb,
            @RequestParam("cv") MultipartFile cv) {
        if (sitioWeb != null && !sitioWeb.isBlank()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(postulacionService.recibir(nombreCompleto, email, telefono, mensaje, cv));
    }
}
