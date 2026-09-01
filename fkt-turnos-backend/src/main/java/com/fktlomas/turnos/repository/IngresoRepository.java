package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Ingreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    @Query("""
            select i from Ingreso i
            join fetch i.medico m
            left join fetch i.turno
            join fetch i.registradoPor
            where (:medicoId is null or m.id = :medicoId)
            and (:desde is null or i.fecha >= :desde)
            and (:hasta is null or i.fecha <= :hasta)
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
            join fetch i.registradoPor
            where i.turno.paciente.id = :pacienteId
            order by i.fecha desc, i.createdAt desc
            """)
    List<Ingreso> findByTurnoPacienteId(@Param("pacienteId") Long pacienteId);
}
