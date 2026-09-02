package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.EstadoPlanKinesiologia;
import com.fktlomas.turnos.dto.kinesiologia.PlanKinesiologiaDto;
import com.fktlomas.turnos.dto.kinesiologia.RechazarPlanRequest;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.KinesiologiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * La alta telefónica (crearPorTelefono) y la consulta son tarea de secretaria, igual que la
 * reserva de turnos comunes. Validar/rechazar/cancelar/pausar un plan sí quedan ADMIN-only:
 * son una decisión sobre un tratamiento, no una tarea administrativa.
 */
@RestController
@RequestMapping("/api/admin/kinesiologia/planes")
@RequiredArgsConstructor
public class AdminKinesiologiaController {

    private final KinesiologiaService kinesiologiaService;

    @GetMapping
    public List<PlanKinesiologiaDto> buscar(@RequestParam(required = false) EstadoPlanKinesiologia estado) {
        return kinesiologiaService.buscarAdmin(estado);
    }

    /**
     * Alta telefónica: la secretaria carga el plan (incluida la orden médica escaneada) en
     * nombre de un paciente que no puede hacerlo por su cuenta — mismo patrón que la reserva
     * de turnos comunes por teléfono.
     */
    @PostMapping(consumes = "multipart/form-data")
    public PlanKinesiologiaDto crearPorTelefono(
            @RequestParam String dni,
            @RequestParam String nombreCompleto,
            @RequestParam String telefono,
            @RequestParam String obraSocial,
            @RequestParam(required = false) String email,
            @RequestParam Long medicoId,
            @RequestParam int cantidadSesiones,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam List<DayOfWeek> diasSemana,
            @RequestParam List<String> horasInicio,
            @RequestParam("ordenMedica") MultipartFile ordenMedica) {
        return kinesiologiaService.crearPorTelefono(dni, nombreCompleto, telefono, obraSocial, email,
                medicoId, cantidadSesiones, fechaInicio, diasSemana, KinesiologiaService.parsearHoras(horasInicio), ordenMedica);
    }

    @GetMapping("/{id}/orden-medica")
    public ResponseEntity<Resource> ordenMedica(@PathVariable Long id) {
        Resource recurso = kinesiologiaService.descargarOrdenMedica(id);
        String contentType = kinesiologiaService.contentTypeOrdenMedica(id);
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(recurso);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/validar")
    public PlanKinesiologiaDto validar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return kinesiologiaService.validar(id, principal.getUsuario());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/rechazar")
    public PlanKinesiologiaDto rechazar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                         @Valid @RequestBody RechazarPlanRequest request) {
        return kinesiologiaService.rechazar(id, request.motivo(), principal.getUsuario());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/cancelar")
    public PlanKinesiologiaDto cancelar(@PathVariable Long id) {
        return kinesiologiaService.cancelar(id, null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/pausar")
    public PlanKinesiologiaDto pausar(@PathVariable Long id) {
        return kinesiologiaService.pausar(id, null);
    }
}
