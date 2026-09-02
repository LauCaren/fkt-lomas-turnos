package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.auditoria.RegistroAuditoriaDto;
import com.fktlomas.turnos.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Historial de quién hizo qué en el sistema — solo ADMIN, incluye cambios de caja y de cuentas de staff. */
@RestController
@RequestMapping("/api/admin/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditoriaController {

    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final AuditoriaService auditoriaService;

    @GetMapping
    public Page<RegistroAuditoriaDto> buscar(
            @RequestParam(required = false) String tipoEntidad,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = TAMANIO_PAGINA_MAXIMO) Pageable pageable) {
        Pageable pageableAcotado = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANIO_PAGINA_MAXIMO));
        return auditoriaService.buscarGeneral(tipoEntidad, usuarioId, desde, hasta, pageableAcotado);
    }
}
