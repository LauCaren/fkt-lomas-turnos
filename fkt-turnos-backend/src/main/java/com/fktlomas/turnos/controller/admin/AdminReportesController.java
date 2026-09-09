package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import com.fktlomas.turnos.service.CajaService;
import com.fktlomas.turnos.service.GastoService;
import com.fktlomas.turnos.service.ReporteService;
import com.fktlomas.turnos.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Exportación a Excel/PDF que usan las secretarias cuando el dueño (u otra persona) pide ver
 * qué movimientos hubo en el consultorio o en la caja un día/rango dado — no un reemplazo de
 * las pantallas, un archivo para mandar por fuera de la app.
 */
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReportesController {

    private static final MediaType EXCEL = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final TurnoService turnoService;
    private final CajaService cajaService;
    private final GastoService gastoService;
    private final ReporteService reporteService;

    @GetMapping("/agenda")
    public ResponseEntity<byte[]> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "excel") String formato) {
        List<TurnoAdminDto> turnos = turnoService.agenda(desde, hasta, null, null);
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] archivo = pdf ? reporteService.pdfAgenda(turnos, desde, hasta) : reporteService.excelAgenda(turnos, desde, hasta);
        String nombre = "agenda_" + desde + "_a_" + hasta + (pdf ? ".pdf" : ".xlsx");
        return archivoDescargable(archivo, nombre, pdf);
    }

    @GetMapping("/caja")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> caja(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "excel") String formato) {
        List<IngresoDto> ingresos = cajaService.listarIngresos(null, desde, hasta);
        List<RetiroDto> retiros = cajaService.listarRetiros(null, desde, hasta);
        List<GastoDto> gastos = gastoService.listar(null, desde, hasta);
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] archivo = pdf ? reporteService.pdfCaja(ingresos, retiros, gastos, desde, hasta) : reporteService.excelCaja(ingresos, retiros, gastos, desde, hasta);
        String nombre = "caja_" + desde + "_a_" + hasta + (pdf ? ".pdf" : ".xlsx");
        return archivoDescargable(archivo, nombre, pdf);
    }

    private ResponseEntity<byte[]> archivoDescargable(byte[] archivo, String nombre, boolean pdf) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombre).build());
        return ResponseEntity.ok().contentType(pdf ? MediaType.APPLICATION_PDF : EXCEL).headers(headers).body(archivo);
    }
}
