package com.fktlomas.turnos.service;

import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apache POI/PDFBox son librerías con superficie grande — algo puede compilar bien y romper
 * recién en runtime (formato de celda, fuente no encontrada, etc). Esto solo confirma que los
 * cuatro generadores producen un archivo no vacío con datos reales y con listas vacías, sin
 * tirar excepción — no valida el contenido byte a byte.
 */
class ReporteServiceTest {

    private final ReporteService reporteService = new ReporteService();

    @Test
    void generaExcelYPdfDeAgendaConDatosYSinDatos() {
        LocalDate hoy = LocalDate.now();
        TurnoAdminDto turno = new TurnoAdminDto(1L, 1L, "Dr. Test", "Kinesiología", hoy, LocalTime.of(10, 0), LocalTime.of(10, 30),
                "RESERVADO", "Control", 1L, "Paciente Test", "30111111", "1122334455", "Particular", "PENDIENTE", false);

        assertThat(reporteService.excelAgenda(List.of(turno), hoy, hoy)).isNotEmpty();
        assertThat(reporteService.excelAgenda(List.of(), hoy, hoy)).isNotEmpty();
        assertThat(reporteService.pdfAgenda(List.of(turno), hoy, hoy)).isNotEmpty();
        assertThat(reporteService.pdfAgenda(List.of(), hoy, hoy)).isNotEmpty();
    }

    @Test
    void generaExcelYPdfDeCajaConDatosYSinDatos() {
        LocalDate hoy = LocalDate.now();
        IngresoDto ingreso = new IngresoDto(1L, 1L, "Dr. Test", null, new BigDecimal("1000"), "EFECTIVO", hoy, "Secretaria", false, null, Instant.now());
        RetiroDto retiro = new RetiroDto(1L, 1L, "Dr. Test", new BigDecimal("300"), "Secretaria", Instant.now(), false, null);
        GastoDto gasto = new GastoDto(1L, 1L, "Insumos", new BigDecimal("100"), hoy, "Guantes", false, "Secretaria", false, null, Instant.now());

        assertThat(reporteService.excelCaja(List.of(ingreso), List.of(retiro), List.of(gasto), hoy, hoy)).isNotEmpty();
        assertThat(reporteService.excelCaja(List.of(), List.of(), List.of(), hoy, hoy)).isNotEmpty();
        assertThat(reporteService.pdfCaja(List.of(ingreso), List.of(retiro), List.of(gasto), hoy, hoy)).isNotEmpty();
        assertThat(reporteService.pdfCaja(List.of(), List.of(), List.of(), hoy, hoy)).isNotEmpty();
    }
}
