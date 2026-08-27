package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.dto.PostulacionDto;
import com.fktlomas.turnos.service.PostulacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/postulaciones")
@RequiredArgsConstructor
public class AdminPostulacionController {

    private final PostulacionService postulacionService;

    @GetMapping
    public List<PostulacionDto> listar() {
        return postulacionService.listar();
    }

    @PatchMapping("/{id}/revisado")
    public PostulacionDto marcarRevisado(@PathVariable Long id) {
        return postulacionService.marcarRevisado(id);
    }

    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> descargarCv(@PathVariable Long id) {
        Postulacion postulacion = postulacionService.obtenerEntidad(id);
        Resource recurso = postulacionService.descargarCv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + postulacion.getCvNombreOriginal() + "\"")
                .body(recurso);
    }
}
