package com.fktlomas.turnos.service;

import com.fktlomas.turnos.dto.caja.ConfiguracionCajaDto;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * El último día de cada mes, si hay un email de reportes configurado (ver
 * ConfiguracionCaja/CajaService), manda un Excel con todos los ingresos, retiros y gastos del
 * mes — pedido explícito de Lautaro. El destinatario se configura desde el panel de admin
 * (pestaña Caja), no hay ninguno hardcodeado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteCajaProgramadoService {

    private final CajaService cajaService;
    private final GastoService gastoService;
    private final ReporteService reporteService;
    private final EmailService emailService;

    /** Segundo minuto hora día-de-mes mes día-de-semana — "L" es el último día del mes. */
    @Scheduled(cron = "0 0 20 L * *")
    public void enviarReporteMensualSiCorresponde() {
        ConfiguracionCajaDto configuracion = cajaService.obtenerConfiguracion();
        String destinatario = configuracion.emailReporteMensual();
        if (destinatario == null || destinatario.isBlank()) {
            log.info("Reporte mensual de caja: no hay email configurado, no se envía nada este mes.");
            return;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        enviarReporte(destinatario, primerDiaMes, hoy);
    }

    /** Extraído para poder probarlo con una fecha fija sin esperar a fin de mes. */
    void enviarReporte(String destinatario, LocalDate desde, LocalDate hasta) {
        List<IngresoDto> ingresos = cajaService.listarIngresos(null, desde, hasta);
        List<RetiroDto> retiros = cajaService.listarRetiros(null, desde, hasta);
        List<GastoDto> gastos = gastoService.listar(null, desde, hasta);

        byte[] excel = reporteService.excelCaja(ingresos, retiros, gastos, desde, hasta);
        String nombreMes = hasta.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
        String asunto = "Caja de %s %d — FKT Lomas".formatted(nombreMes, hasta.getYear());
        String cuerpo = """
                Hola,

                Adjunto el detalle de ingresos, retiros y gastos de caja de %s %d.

                Este mail se genera y se envía automáticamente el último día de cada mes.

                FKT Lomas
                """.formatted(nombreMes, hasta.getYear());

        try {
            emailService.enviarConAdjuntoSync(destinatario, asunto, cuerpo,
                    "caja_%s.xlsx".formatted(hasta), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);
            log.info("Reporte mensual de caja enviado a {} ({} a {})", destinatario, desde, hasta);
        } catch (Exception e) {
            log.error("No se pudo enviar el reporte mensual de caja a {}: {}", destinatario, e.getMessage());
        }
    }
}
