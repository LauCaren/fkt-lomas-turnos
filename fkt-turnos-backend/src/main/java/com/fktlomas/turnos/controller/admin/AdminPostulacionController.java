package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.dto.PostulacionDto;
import com.fktlomas.turnos.service.PostulacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/postulaciones")
@RequiredArgsConstructor
public class AdminPostulacionController {

    private final PostulacionService postulacionService;

    @GetMapping
    public Page<PostulacionDto> listar(@PageableDefault(size = 20) Pageable pageable) {
        return postulacionService.listar(pageable);
    }

    @PatchMapping("/{id}/revisado")
    public PostulacionDto marcarRevisado(@PathVariable Long id) {
        return postulacionService.marcarRevisado(id);
    }

    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> descargarCv(@PathVariable Long id) {
        Postulacion postulacion = postulacionService.obtenerEntidad(id);
        Resource recurso = postulacionService.descargarCv(id);
        // El nombre original lo eligió quien subió el CV: nunca se concatena crudo en el
        // header, se arma con el builder de Spring para que quede correctamente escapado.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(postulacion.getCvNombreOriginal(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(recurso);
    }
}
