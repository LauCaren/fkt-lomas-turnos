package com.fktlomas.turnos.service;

import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Todos los lunes a la mañana, un resumen liviano de la semana anterior (turnos reservados,
 * ingresos, gastos, retiros, caja física) al mail interno de la clínica — para que Fernando
 * tenga un pantallazo sin entrar al panel. A diferencia del reporte mensual de caja (Excel
 * adjunto, destinatario configurable desde "Caja" — ver ReporteCajaProgramadoService), este
 * siempre va al mismo mail interno (ADMIN_NOTIFY_EMAIL) y el resumen va en el cuerpo, sin adjunto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumenSemanalService {

    private final TurnoService turnoService;
    private final CajaService cajaService;
    private final GastoService gastoService;
    private final EmailService emailService;

    /** Todos los lunes a las 8am — resume la semana anterior (lunes a domingo). */
    @Scheduled(cron = "0 0 8 * * MON", zone = "America/Argentina/Buenos_Aires")
    @SchedulerLock(name = "resumenSemanal")
    public void enviarResumenSemanal() {
        LocalDate hasta = LocalDate.now().minusDays(1);
        LocalDate desde = hasta.minusDays(6);
        enviarResumen(desde, hasta);
    }

    /** Extraído del método programado para poder probarlo con una semana fija sin esperar al lunes. */
    void enviarResumen(LocalDate desde, LocalDate hasta) {
        try {
            long turnosReservados = turnoService.agenda(desde, hasta, null, null).stream()
                    .filter(t -> "RESERVADO".equals(t.estado()))
                    .count();
            BigDecimal ingresos = cajaService.listarIngresos(null, desde, hasta).stream()
                    .filter(i -> !i.anulado())
                    .map(IngresoDto::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gastos = gastoService.listar(null, desde, hasta).stream()
                    .filter(g -> !g.anulado())
                    .map(GastoDto::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal retiros = cajaService.listarRetiros(null, desde, hasta).stream()
                    .filter(r -> !r.anulado())
                    .map(RetiroDto::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cajaFisica = cajaService.resumen().cajaFisica();

            emailService.enviarResumenSemanalSync(desde, hasta, (int) turnosReservados, ingresos, gastos, retiros, cajaFisica);
            log.info("Resumen semanal enviado ({} a {})", desde, hasta);
        } catch (Exception e) {
            log.error("No se pudo enviar el resumen semanal ({} a {}): {}", desde, hasta, e.getMessage());
        }
    }
}
