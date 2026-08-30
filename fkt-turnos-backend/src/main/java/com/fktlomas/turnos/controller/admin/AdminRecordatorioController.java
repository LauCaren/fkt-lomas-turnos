package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.ConfiguracionRecordatoriosDto;
import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.RecordatorioDto;
import com.fktlomas.turnos.dto.admin.CanalPreferidoRequest;
import com.fktlomas.turnos.dto.admin.ConfiguracionRecordatoriosRequest;
import com.fktlomas.turnos.dto.admin.MensajeManualRequest;
import com.fktlomas.turnos.service.RecordatorioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/configuracion")
    public ConfiguracionRecordatoriosDto obtenerConfiguracion() {
        return recordatorioService.obtenerConfiguracion();
    }

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
}
