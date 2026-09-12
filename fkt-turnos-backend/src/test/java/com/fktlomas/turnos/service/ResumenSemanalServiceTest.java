package com.fktlomas.turnos.service;

import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.caja.CajaResumenDto;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit test puro: ResumenSemanalService solo agrega números de otros services y arma un mail — Mockito alcanza. */
class ResumenSemanalServiceTest {

    private TurnoService turnoService;
    private CajaService cajaService;
    private GastoService gastoService;
    private EmailService emailService;
    private ResumenSemanalService resumenSemanalService;

    @BeforeEach
    void setUp() {
        turnoService = mock(TurnoService.class);
        cajaService = mock(CajaService.class);
        gastoService = mock(GastoService.class);
        emailService = mock(EmailService.class);
        resumenSemanalService = new ResumenSemanalService(turnoService, cajaService, gastoService, emailService);
    }

    @Test
    void agregaTurnosReservadosIngresosGastosYRetirosDeLaSemana() {
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 7);

        when(turnoService.agenda(desde, hasta, null, null)).thenReturn(List.of(
                turno("RESERVADO"), turno("RESERVADO"), turno("DISPONIBLE"), turno("CANCELADO")));
        when(cajaService.listarIngresos(null, desde, hasta)).thenReturn(List.of(
                ingreso(new BigDecimal("1000"), false), ingreso(new BigDecimal("500"), true)));
        when(gastoService.listar(null, desde, hasta)).thenReturn(List.of(gasto(new BigDecimal("200"), false)));
        when(cajaService.listarRetiros(null, desde, hasta)).thenReturn(List.of(retiro(new BigDecimal("300"), false)));
        when(cajaService.resumen()).thenReturn(new CajaResumenDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000"), List.of()));

        resumenSemanalService.enviarResumen(desde, hasta);

        // El ingreso anulado (500) no debería sumar — solo cuentan los 2 turnos RESERVADO y el ingreso válido.
        verify(emailService).enviarResumenSemanalSync(eq(desde), eq(hasta), eq(2),
                eq(new BigDecimal("1000")), eq(new BigDecimal("200")), eq(new BigDecimal("300")), eq(new BigDecimal("5000")));
    }

    @Test
    void siFallaAlgunDatoNoTiraLaExcepcionHaciaAfuera() {
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 7);
        when(turnoService.agenda(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));

        resumenSemanalService.enviarResumen(desde, hasta);

        verifyNoInteractions(emailService);
    }

    private TurnoAdminDto turno(String estado) {
        return new TurnoAdminDto(1L, 1L, "Dr. Test", "Kinesiología", LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(10, 30),
                estado, null, null, null, null, null, null, "PENDIENTE", false, "SIN_RESPUESTA", null);
    }

    private IngresoDto ingreso(BigDecimal monto, boolean anulado) {
        return new IngresoDto(1L, 1L, "Dr. Test", null, monto, "EFECTIVO", LocalDate.now(), "Admin", anulado, null, Instant.now());
    }

    private GastoDto gasto(BigDecimal monto, boolean anulado) {
        return new GastoDto(1L, 1L, "Insumos", monto, LocalDate.now(), "desc", false, "Admin", anulado, null, Instant.now());
    }

    private RetiroDto retiro(BigDecimal monto, boolean anulado) {
        return new RetiroDto(1L, 1L, "Dr. Test", monto, "Admin", Instant.now(), anulado, null);
    }
}
