package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Ingreso;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Retiro;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.caja.*;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.GastoRepository;
import com.fktlomas.turnos.repository.IngresoRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.RetiroRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaService {

    private final IngresoRepository ingresoRepository;
    private final RetiroRepository retiroRepository;
    private final GastoRepository gastoRepository;
    private final MedicoRepository medicoRepository;
    private final TurnoRepository turnoRepository;

    @Transactional(readOnly = true)
    public List<IngresoDto> listarIngresos(Long medicoId, LocalDate desde, LocalDate hasta) {
        return ingresoRepository.buscar(medicoId, desde, hasta).stream().map(IngresoDto::de).toList();
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
    public IngresoDto anularIngreso(Long id, String motivo) {
        Ingreso ingreso = ingresoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Ingreso no encontrado"));
        if (ingreso.isAnulado()) {
            throw new ConflictoDeDatosException("Este ingreso ya está anulado");
        }
        ingreso.setAnulado(true);
        ingreso.setMotivoAnulacion(motivo);
        return IngresoDto.de(ingresoRepository.save(ingreso));
    }

    @Transactional(readOnly = true)
    public List<RetiroDto> listarRetiros(Long medicoId) {
        return retiroRepository.buscar(medicoId).stream().map(RetiroDto::de).toList();
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
    public RetiroDto anularRetiro(Long id, String motivo) {
        Retiro retiro = retiroRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Retiro no encontrado"));
        if (retiro.isAnulado()) {
            throw new ConflictoDeDatosException("Este retiro ya está anulado");
        }
        retiro.setAnulado(true);
        retiro.setMotivoAnulacion(motivo);
        return RetiroDto.de(retiroRepository.save(retiro));
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
}
