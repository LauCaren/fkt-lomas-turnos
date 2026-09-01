package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.caja.*;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.CajaService;
import com.fktlomas.turnos.service.GastoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/caja")
@RequiredArgsConstructor
public class AdminCajaController {

    private final CajaService cajaService;
    private final GastoService gastoService;

    @GetMapping("/resumen")
    public CajaResumenDto resumen() {
        return cajaService.resumen();
    }

    @GetMapping("/configuracion")
    public ConfiguracionCajaDto obtenerConfiguracion() {
        return cajaService.obtenerConfiguracion();
    }

    @PutMapping("/configuracion")
    public ConfiguracionCajaDto actualizarConfiguracion(@Valid @RequestBody ActualizarConfiguracionCajaRequest request) {
        return cajaService.actualizarConfiguracion(request);
    }

    @GetMapping("/ingresos")
    public List<IngresoDto> listarIngresos(@RequestParam(required = false) Long medicoId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cajaService.listarIngresos(medicoId, desde, hasta);
    }

    @PostMapping("/ingresos")
    public IngresoDto registrarIngreso(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody CrearIngresoRequest request) {
        return cajaService.registrarIngreso(request, principal.getUsuario());
    }

    @PutMapping("/ingresos/{id}/anular")
    public IngresoDto anularIngreso(@PathVariable Long id, @Valid @RequestBody AnularRequest request) {
        return cajaService.anularIngreso(id, request.motivo());
    }

    @GetMapping("/retiros")
    public List<RetiroDto> listarRetiros(@RequestParam(required = false) Long medicoId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cajaService.listarRetiros(medicoId, desde, hasta);
    }

    @PostMapping("/retiros")
    public RetiroDto registrarRetiro(@AuthenticationPrincipal AppUserDetails principal, @Valid @RequestBody CrearRetiroRequest request) {
        return cajaService.registrarRetiro(request, principal.getUsuario());
    }

    @PutMapping("/retiros/{id}/anular")
    public RetiroDto anularRetiro(@PathVariable Long id, @Valid @RequestBody AnularRequest request) {
        return cajaService.anularRetiro(id, request.motivo());
    }

    @GetMapping("/gastos/categorias")
    public List<CategoriaGastoDto> listarCategorias() {
        return gastoService.listarCategorias();
    }

    @PostMapping("/gastos/categorias")
    public CategoriaGastoDto crearCategoria(@Valid @RequestBody CrearCategoriaGastoRequest request) {
        return gastoService.crearCategoria(request);
    }

    @GetMapping("/gastos")
    public List<GastoDto> listarGastos(@RequestParam(required = false) Long categoriaId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return gastoService.listar(categoriaId, desde, hasta);
    }

    @PostMapping(value = "/gastos", consumes = "multipart/form-data")
    public GastoDto registrarGasto(@AuthenticationPrincipal AppUserDetails principal,
                                    @RequestParam Long categoriaId,
                                    @RequestParam BigDecimal monto,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                    @RequestParam(required = false) String descripcion,
                                    @RequestParam(value = "comprobante", required = false) MultipartFile comprobante) {
        return gastoService.registrar(categoriaId, monto, fecha, descripcion, comprobante, principal.getUsuario());
    }

    @PutMapping("/gastos/{id}/anular")
    public GastoDto anularGasto(@PathVariable Long id, @Valid @RequestBody AnularRequest request) {
        return gastoService.anular(id, request.motivo());
    }

    @GetMapping("/gastos/{id}/comprobante")
    public ResponseEntity<Resource> comprobante(@PathVariable Long id) {
        Resource recurso = gastoService.descargarComprobante(id);
        String contentType = gastoService.contentTypeComprobante(id);
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(recurso);
    }
}
