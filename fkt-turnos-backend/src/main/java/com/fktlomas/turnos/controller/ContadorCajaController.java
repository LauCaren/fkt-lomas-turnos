package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.caja.CajaResumenDto;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import com.fktlomas.turnos.service.CajaService;
import com.fktlomas.turnos.service.GastoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Vista de solo lectura para el rol CONTADOR: únicamente movimientos de caja (ingresos,
 * retiros, gastos). Sin acceso a turnos, pacientes ni historias clínicas — ver SecurityConfig,
 * que además bloquea a nivel de autorización cualquier otro endpoint para este rol.
 */
@RestController
@RequestMapping("/api/contador/caja")
@RequiredArgsConstructor
public class ContadorCajaController {

    private final CajaService cajaService;
    private final GastoService gastoService;

    @GetMapping("/resumen")
    public CajaResumenDto resumen() {
        return cajaService.resumen();
    }

    @GetMapping("/ingresos")
    public List<IngresoDto> listarIngresos(@RequestParam(required = false) Long medicoId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cajaService.listarIngresos(medicoId, desde, hasta);
    }

    @GetMapping("/retiros")
    public List<RetiroDto> listarRetiros(@RequestParam(required = false) Long medicoId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cajaService.listarRetiros(medicoId, desde, hasta);
    }

    @GetMapping("/gastos")
    public List<GastoDto> listarGastos(@RequestParam(required = false) Long categoriaId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return gastoService.listar(categoriaId, desde, hasta);
    }
}
