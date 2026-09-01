package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.kinesiologia.PlanKinesiologiaDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.KinesiologiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Plan de sesiones de kinesiología del propio paciente autenticado. */
@RestController
@RequestMapping("/api/kinesiologia/planes")
@RequiredArgsConstructor
public class KinesiologiaController {

    private final KinesiologiaService kinesiologiaService;

    @GetMapping
    public List<PlanKinesiologiaDto> misPlanes(@AuthenticationPrincipal AppUserDetails principal) {
        return kinesiologiaService.misPlanes(principal.getUsuario().getId());
    }

    @PostMapping(consumes = "multipart/form-data")
    public PlanKinesiologiaDto crear(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam Long medicoId,
            @RequestParam int cantidadSesiones,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam List<DayOfWeek> diasSemana,
            @RequestParam List<String> horasInicio,
            @RequestParam("ordenMedica") MultipartFile ordenMedica) {
        return kinesiologiaService.crearParaPaciente(principal.getUsuario(), medicoId, cantidadSesiones, fechaInicio, diasSemana, KinesiologiaService.parsearHoras(horasInicio), ordenMedica);
    }

    @PutMapping("/{id}/cancelar")
    public PlanKinesiologiaDto cancelar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return kinesiologiaService.cancelar(id, principal.getUsuario().getId());
    }

    @PutMapping("/{id}/pausar")
    public PlanKinesiologiaDto pausar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return kinesiologiaService.pausar(id, principal.getUsuario().getId());
    }

    @PutMapping("/{id}/reanudar")
    public PlanKinesiologiaDto reanudar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio) {
        return kinesiologiaService.reanudar(id, principal.getUsuario().getId(), fechaInicio != null ? fechaInicio : LocalDate.now());
    }
}
