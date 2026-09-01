package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Retiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RetiroRepository extends JpaRepository<Retiro, Long> {

    @Query("""
            select r from Retiro r
            join fetch r.medico
            join fetch r.entregadoPor
            where (:medicoId is null or r.medico.id = :medicoId)
            order by r.fechaHora desc
            """)
    List<Retiro> buscar(@Param("medicoId") Long medicoId);

    @Query("select coalesce(sum(r.monto), 0) from Retiro r where r.anulado = false and (:medicoId is null or r.medico.id = :medicoId)")
    BigDecimal totalRetiros(@Param("medicoId") Long medicoId);
}
