package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.ConfiguracionCaja;
import com.fktlomas.turnos.domain.Gasto;
import com.fktlomas.turnos.domain.Ingreso;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Retiro;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.caja.*;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.ConfiguracionCajaRepository;
import com.fktlomas.turnos.repository.GastoRepository;
import com.fktlomas.turnos.repository.IngresoRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.RetiroRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CajaService {

    private final IngresoRepository ingresoRepository;
    private final RetiroRepository retiroRepository;
    private final GastoRepository gastoRepository;
    private final MedicoRepository medicoRepository;
    private final TurnoRepository turnoRepository;
    private final ConfiguracionCajaRepository configuracionCajaRepository;
    private final AuditoriaService auditoriaService;

    private static final String AUDITORIA_TIPO_INGRESO = "INGRESO";
    private static final String AUDITORIA_TIPO_RETIRO = "RETIRO";

    // Postgres no puede inferir el tipo de un parámetro cuyo único uso en la consulta es
    // "? is null" (a diferencia de medicoId, que sí resuelve bien) — se evita pasando
    // siempre un rango concreto en vez de nulls "abiertos".
    private static final LocalDate DESDE_MINIMO = LocalDate.of(2000, 1, 1);
    private static final LocalDate HASTA_MAXIMO = LocalDate.of(2099, 12, 31);

    @Transactional(readOnly = true)
    public ConfiguracionCajaDto obtenerConfiguracion() {
        return ConfiguracionCajaDto.de(configuracionCajaRepository.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de configuración de caja (id=1)")));
    }

    @Transactional
    public ConfiguracionCajaDto actualizarConfiguracion(ActualizarConfiguracionCajaRequest request) {
        ConfiguracionCaja configuracion = configuracionCajaRepository.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de configuración de caja (id=1)"));
        String email = request.emailReporteMensual();
        configuracion.setEmailReporteMensual(email != null && !email.isBlank() ? email.trim() : null);
        return ConfiguracionCajaDto.de(configuracionCajaRepository.save(configuracion));
    }

    @Transactional(readOnly = true)
    public List<IngresoDto> listarIngresos(Long medicoId, LocalDate desde, LocalDate hasta) {
        LocalDate desdeEfectivo = desde != null ? desde : DESDE_MINIMO;
        LocalDate hastaEfectivo = hasta != null ? hasta : HASTA_MAXIMO;
        return ingresoRepository.buscar(medicoId, desdeEfectivo, hastaEfectivo).stream().map(IngresoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<IngresoDto> ingresosDePaciente(Long pacienteId) {
        return ingresoRepository.findByTurnoPacienteId(pacienteId).stream().map(IngresoDto::de).toList();
    }

    @Transactional
    public IngresoDto registrarIngreso(CrearIngresoRequest request, Usuario registradoPor) {
        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        Turno turno = null;
        if (request.turnoId() != null) {
            turno = turnoRepository.findById(request.turnoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        }
        Ingreso ingreso = new Ingreso();
        ingreso.setMedico(medico);
        ingreso.setTurno(turno);
        ingreso.setMonto(request.monto());
        ingreso.setMetodoPago(request.metodoPago());
        ingreso.setFecha(request.fecha() != null ? request.fecha() : LocalDate.now());
        ingreso.setRegistradoPor(registradoPor);
        return IngresoDto.de(ingresoRepository.save(ingreso));
    }

    @Transactional
    public IngresoDto anularIngreso(Long id, String motivo, Usuario actor) {
        Ingreso ingreso = ingresoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Ingreso no encontrado"));
        if (ingreso.isAnulado()) {
            throw new ConflictoDeDatosException("Este ingreso ya está anulado");
        }
        IngresoDto antes = IngresoDto.de(ingreso);
        ingreso.setAnulado(true);
        ingreso.setMotivoAnulacion(motivo);
        IngresoDto anulado = IngresoDto.de(ingresoRepository.save(ingreso));
        auditoriaService.registrar(actor, AUDITORIA_TIPO_INGRESO, id, "ANULAR", antes, anulado);
        return anulado;
    }

    @Transactional(readOnly = true)
    public List<RetiroDto> listarRetiros(Long medicoId, LocalDate desde, LocalDate hasta) {
        LocalDate desdeEfectivo = desde != null ? desde : DESDE_MINIMO;
        LocalDate hastaEfectivo = hasta != null ? hasta : HASTA_MAXIMO;
        Instant desdeInstant = desdeEfectivo.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant hastaInstant = hastaEfectivo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return retiroRepository.buscar(medicoId, desdeInstant, hastaInstant).stream().map(RetiroDto::de).toList();
    }

    @Transactional
    public RetiroDto registrarRetiro(CrearRetiroRequest request, Usuario entregadoPor) {
        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));

        BigDecimal saldoActual = ingresoRepository.totalIngresos(medico.getId()).subtract(retiroRepository.totalRetiros(medico.getId()));
        if (request.monto().compareTo(saldoActual) > 0) {
            throw new IllegalArgumentException("El monto supera el saldo acumulado del médico ($" + saldoActual + ")");
        }

        Retiro retiro = new Retiro();
        retiro.setMedico(medico);
        retiro.setMonto(request.monto());
        retiro.setEntregadoPor(entregadoPor);
        return RetiroDto.de(retiroRepository.save(retiro));
    }

    @Transactional
    public RetiroDto anularRetiro(Long id, String motivo, Usuario actor) {
        Retiro retiro = retiroRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Retiro no encontrado"));
        if (retiro.isAnulado()) {
            throw new ConflictoDeDatosException("Este retiro ya está anulado");
        }
        RetiroDto antes = RetiroDto.de(retiro);
        retiro.setAnulado(true);
        retiro.setMotivoAnulacion(motivo);
        RetiroDto anulado = RetiroDto.de(retiroRepository.save(retiro));
        auditoriaService.registrar(actor, AUDITORIA_TIPO_RETIRO, id, "ANULAR", antes, anulado);
        return anulado;
    }

    /** Saldo propio de un médico (para su panel) — nunca la caja física ni la de otros médicos. */
    @Transactional(readOnly = true)
    public SaldoMedicoDto saldoDeMedico(Long medicoId) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
        BigDecimal ingresos = ingresoRepository.totalIngresos(medicoId);
        BigDecimal retiros = retiroRepository.totalRetiros(medicoId);
        return new SaldoMedicoDto(medico.getId(), medico.getNombreCompleto(), ingresos, retiros, ingresos.subtract(retiros));
    }

    @Transactional(readOnly = true)
    public CajaResumenDto resumen() {
        BigDecimal ingresosTotal = ingresoRepository.totalIngresos(null);
        BigDecimal retirosTotal = retiroRepository.totalRetiros(null);
        BigDecimal gastosTotal = gastoRepository.totalGastos();
        BigDecimal cajaFisica = ingresosTotal.subtract(retirosTotal).subtract(gastosTotal);

        List<SaldoMedicoDto> saldos = medicoRepository.findAll().stream()
                .map(m -> {
                    BigDecimal ingresosMedico = ingresoRepository.totalIngresos(m.getId());
                    BigDecimal retirosMedico = retiroRepository.totalRetiros(m.getId());
                    return new SaldoMedicoDto(m.getId(), m.getNombreCompleto(), ingresosMedico, retirosMedico, ingresosMedico.subtract(retirosMedico));
                })
                .filter(s -> s.ingresosTotal().signum() != 0 || s.retirosTotal().signum() != 0)
                .toList();

        return new CajaResumenDto(ingresosTotal, retirosTotal, gastosTotal, cajaFisica, saldos);
    }

    private static final int TENDENCIA_DIAS_MAXIMO = 90;

    /** Ingresos/gastos/retiros por día de los últimos "dias" días (incluye hoy) — para el gráfico de tendencia del panel de contador. */
    @Transactional(readOnly = true)
    public List<TendenciaCajaDiaDto> tendencia(int dias) {
        int diasEfectivos = Math.min(Math.max(dias, 1), TENDENCIA_DIAS_MAXIMO);
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(diasEfectivos - 1L);

        Map<LocalDate, BigDecimal> ingresosPorDia = ingresoRepository.buscar(null, desde, hasta).stream()
                .filter(i -> !i.isAnulado())
                .collect(Collectors.groupingBy(Ingreso::getFecha, Collectors.reducing(BigDecimal.ZERO, Ingreso::getMonto, BigDecimal::add)));

        Map<LocalDate, BigDecimal> gastosPorDia = gastoRepository.buscar(null, desde, hasta).stream()
                .filter(g -> !g.isAnulado())
                .collect(Collectors.groupingBy(Gasto::getFecha, Collectors.reducing(BigDecimal.ZERO, Gasto::getMonto, BigDecimal::add)));

        Instant desdeInstant = desde.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant hastaInstant = hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Map<LocalDate, BigDecimal> retirosPorDia = retiroRepository.buscar(null, desdeInstant, hastaInstant).stream()
                .filter(r -> !r.isAnulado())
                .collect(Collectors.groupingBy(
                        r -> r.getFechaHora().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Retiro::getMonto, BigDecimal::add)));

        List<TendenciaCajaDiaDto> resultado = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            resultado.add(new TendenciaCajaDiaDto(fecha,
                    ingresosPorDia.getOrDefault(fecha, BigDecimal.ZERO),
                    gastosPorDia.getOrDefault(fecha, BigDecimal.ZERO),
                    retirosPorDia.getOrDefault(fecha, BigDecimal.ZERO)));
        }
        return resultado;
    }
}
