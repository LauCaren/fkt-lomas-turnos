package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Ingreso;
import com.fktlomas.turnos.domain.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    /**
     * desde/hasta SIEMPRE se pasan no-nulos (ver CajaService.listarIngresos) — Postgres no
     * puede inferir el tipo de un parámetro cuyo único uso en la consulta es "? is null" (a
     * diferencia de medicoId, que sí lo resuelve bien); pasar un rango con centinelas evita
     * el problema de raíz en vez de tener que castear el parámetro a mano en el JPQL.
     */
    @Query("""
            select i from Ingreso i
            join fetch i.medico m
            left join fetch i.turno
            left join fetch i.registradoPor
            where (:medicoId is null or m.id = :medicoId)
            and i.fecha between :desde and :hasta
            order by i.fecha desc, i.createdAt desc
            """)
    List<Ingreso> buscar(@Param("medicoId") Long medicoId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select coalesce(sum(i.monto), 0) from Ingreso i where i.anulado = false and (:medicoId is null or i.medico.id = :medicoId)")
    BigDecimal totalIngresos(@Param("medicoId") Long medicoId);

    /** Ingresos que se pueden atar a un paciente puntual — solo los que están ligados a uno de sus turnos. */
    @Query("""
            select i from Ingreso i
            join fetch i.medico
            left join fetch i.turno
            left join fetch i.registradoPor
            where i.turno.paciente.id = :pacienteId
            order by i.fecha desc, i.createdAt desc
            """)
    List<Ingreso> findByTurnoPacienteId(@Param("pacienteId") Long pacienteId);

    /** Para generar el comprobante de un ingreso puntual — trae todo lo que necesita el PDF en una sola vuelta. */
    @Query("""
            select i from Ingreso i
            join fetch i.medico m
            join fetch m.especialidad
            left join fetch i.turno t
            left join fetch t.paciente
            left join fetch i.registradoPor
            where i.id = :id
            """)
    Optional<Ingreso> buscarPorIdConDetalle(@Param("id") Long id);

    /**
     * Detecta un doble-submit del mismo cobro (doble click, reintento de red) para el mismo
     * turno: mismo turno, monto, método de pago y quién lo cargó, en los últimos segundos —
     * ver ventana en CajaService. Acotado a turnoId no nulo a propósito: dos cobros sin turno
     * asociado (ej. dos pacientes distintos pagando el mismo monto en efectivo, uno detrás del
     * otro) son un caso legítimo y no hay forma de distinguirlos de un duplicado real sin ese
     * dato.
     */
    @Query("""
            select count(i) > 0 from Ingreso i
            where i.turno.id = :turnoId
            and i.monto = :monto
            and i.metodoPago = :metodoPago
            and i.registradoPor.id = :registradoPorId
            and i.anulado = false
            and i.createdAt >= :desde
            """)
    boolean existeDuplicadoRecientePorTurno(@Param("turnoId") Long turnoId, @Param("monto") BigDecimal monto,
            @Param("metodoPago") MetodoPago metodoPago, @Param("registradoPorId") Long registradoPorId,
            @Param("desde") Instant desde);
}
