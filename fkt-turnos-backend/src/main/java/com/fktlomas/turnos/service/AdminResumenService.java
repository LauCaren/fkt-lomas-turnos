package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoPlanKinesiologia;
import com.fktlomas.turnos.domain.EstadoSolicitudBaja;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.admin.AdminResumenDto;
import com.fktlomas.turnos.dto.admin.TendenciaDiaDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.repository.MensajeContactoRepository;
import com.fktlomas.turnos.repository.PostulacionRepository;
import com.fktlomas.turnos.repository.SolicitudBajaCuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminResumenService {

    private final TurnoService turnoService;
    private final KinesiologiaService kinesiologiaService;
    private final CajaService cajaService;
    private final MensajeContactoRepository mensajeContactoRepository;
    private final PostulacionRepository postulacionRepository;
    private final SolicitudBajaCuentaRepository solicitudBajaCuentaRepository;

    @Transactional(readOnly = true)
    public AdminResumenDto resumen() {
        LocalDate hoy = LocalDate.now();
        List<TurnoAdminDto> agendaHoy = turnoService.agenda(hoy, hoy, null, null);
        long reservados = agendaHoy.stream().filter(t -> "RESERVADO".equals(t.estado())).count();
        long disponibles = agendaHoy.stream().filter(t -> "DISPONIBLE".equals(t.estado())).count();
        long llegaron = agendaHoy.stream().filter(t -> "LLEGO".equals(t.estadoAsistencia())).count();
        long ausentes = agendaHoy.stream().filter(t -> "AUSENTE".equals(t.estadoAsistencia())).count();
        int enEspera = turnoService.colaEspera(hoy, null).size();
        int planesPendientes = kinesiologiaService.buscarAdmin(EstadoPlanKinesiologia.PENDIENTE_VALIDACION).size();
        BigDecimal ingresosHoy = cajaService.listarIngresos(null, hoy, hoy).stream()
                .filter(i -> !i.anulado())
                .map(i -> i.monto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cajaFisica = cajaService.resumen().cajaFisica();

        return new AdminResumenDto(hoy, (int) reservados, (int) disponibles, enEspera, planesPendientes, ingresosHoy, cajaFisica,
                turnoService.turnosSinConfirmarHoy(hoy),
                (int) mensajeContactoRepository.countByLeidoFalse(),
                (int) postulacionRepository.countByRevisadoFalse(),
                (int) solicitudBajaCuentaRepository.countByEstado(EstadoSolicitudBaja.PENDIENTE),
                (int) llegaron, (int) ausentes);
    }

    private static final int TENDENCIA_DIAS_MAXIMO = 90;

    /** Turnos reservados + ingresos por día de los últimos "dias" días (incluye hoy) — para el gráfico de tendencia del panel. */
    @Transactional(readOnly = true)
    public List<TendenciaDiaDto> tendencia(int dias) {
        int diasEfectivos = Math.min(Math.max(dias, 1), TENDENCIA_DIAS_MAXIMO);
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(diasEfectivos - 1L);

        Map<LocalDate, Long> turnosPorDia = turnoService.agenda(desde, hasta, null, null).stream()
                .filter(t -> "RESERVADO".equals(t.estado()))
                .collect(Collectors.groupingBy(TurnoAdminDto::fecha, Collectors.counting()));

        Map<LocalDate, BigDecimal> ingresosPorDia = cajaService.listarIngresos(null, desde, hasta).stream()
                .filter(i -> !i.anulado())
                .collect(Collectors.groupingBy(IngresoDto::fecha, Collectors.reducing(BigDecimal.ZERO, IngresoDto::monto, BigDecimal::add)));

        List<TendenciaDiaDto> resultado = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            resultado.add(new TendenciaDiaDto(fecha,
                    turnosPorDia.getOrDefault(fecha, 0L).intValue(),
                    ingresosPorDia.getOrDefault(fecha, BigDecimal.ZERO)));
        }
        return resultado;
    }
}
