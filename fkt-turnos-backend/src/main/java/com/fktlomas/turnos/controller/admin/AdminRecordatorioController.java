package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.ConfiguracionRecordatoriosDto;
import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.RecordatorioDto;
import com.fktlomas.turnos.dto.admin.CanalPreferidoRequest;
import com.fktlomas.turnos.dto.admin.ConfiguracionRecordatoriosRequest;
import com.fktlomas.turnos.dto.admin.MensajeManualRequest;
import com.fktlomas.turnos.dto.admin.MensajeMasivoRequest;
import com.fktlomas.turnos.dto.admin.MensajeMasivoResponse;
import com.fktlomas.turnos.service.RecordatorioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/recordatorios")
@RequiredArgsConstructor
public class AdminRecordatorioController {

    private final RecordatorioService recordatorioService;

    @GetMapping
    public List<RecordatorioDto> listar() {
        return recordatorioService.listar();
    }

    @PostMapping("/{id}/reenviar")
    public RecordatorioDto reenviar(@PathVariable Long id) {
        return recordatorioService.reenviar(id);
    }

    // La plantilla del mensaje y la anticipación son política del centro, no operación diaria
    // — el resto del controller (ver/reenviar/mandar mensajes) sí es tarea de secretaria.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/configuracion")
    public ConfiguracionRecordatoriosDto obtenerConfiguracion() {
        return recordatorioService.obtenerConfiguracion();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/configuracion")
    public ConfiguracionRecordatoriosDto actualizarConfiguracion(@Valid @RequestBody ConfiguracionRecordatoriosRequest request) {
        return recordatorioService.actualizarConfiguracion(request);
    }

    @GetMapping("/pacientes")
    public List<PacienteBusquedaDto> buscarPacientes(@RequestParam(required = false) String buscar) {
        return recordatorioService.buscarPacientes(buscar);
    }

    @PutMapping("/pacientes/{id}/canal-preferido")
    public PacienteBusquedaDto actualizarCanalPreferido(@PathVariable Long id, @Valid @RequestBody CanalPreferidoRequest request) {
        return recordatorioService.actualizarCanalPreferido(id, request.canal());
    }

    @PostMapping("/mensajes-manuales")
    public ResponseEntity<Void> enviarMensajeManual(@Valid @RequestBody MensajeManualRequest request) {
        recordatorioService.enviarMensajeManual(request.pacienteId(), request.canal(), request.mensaje());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pacientes-en-rango")
    public List<PacienteBusquedaDto> pacientesEnRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long medicoId) {
        return recordatorioService.pacientesEnRango(desde, hasta, medicoId);
    }

    @PostMapping("/mensaje-masivo")
    public MensajeMasivoResponse enviarMensajeMasivo(@Valid @RequestBody MensajeMasivoRequest request) {
        return recordatorioService.enviarMensajeMasivo(request.desde(), request.hasta(), request.medicoId(), request.mensaje());
    }
}
