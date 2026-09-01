package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoPlanKinesiologia;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.admin.AdminResumenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminResumenService {

    private final TurnoService turnoService;
    private final KinesiologiaService kinesiologiaService;
    private final CajaService cajaService;

    @Transactional(readOnly = true)
    public AdminResumenDto resumen() {
        LocalDate hoy = LocalDate.now();
        List<TurnoAdminDto> agendaHoy = turnoService.agenda(hoy, hoy, null, null);
        long reservados = agendaHoy.stream().filter(t -> "RESERVADO".equals(t.estado())).count();
        long disponibles = agendaHoy.stream().filter(t -> "DISPONIBLE".equals(t.estado())).count();
        int enEspera = turnoService.colaEspera(hoy, null).size();
        int planesPendientes = kinesiologiaService.buscarAdmin(EstadoPlanKinesiologia.PENDIENTE_VALIDACION).size();
        BigDecimal ingresosHoy = cajaService.listarIngresos(null, hoy, hoy).stream()
                .filter(i -> !i.anulado())
                .map(i -> i.monto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cajaFisica = cajaService.resumen().cajaFisica();

        return new AdminResumenDto(hoy, (int) reservados, (int) disponibles, enEspera, planesPendientes, ingresosHoy, cajaFisica);
    }
}
