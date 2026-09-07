package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Retiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface RetiroRepository extends JpaRepository<Retiro, Long> {

    /** desde/hasta SIEMPRE se pasan no-nulos — ver comentario en IngresoRepository.buscar. */
    @Query("""
            select r from Retiro r
            join fetch r.medico
            join fetch r.entregadoPor
            where (:medicoId is null or r.medico.id = :medicoId)
            and r.fechaHora >= :desde and r.fechaHora < :hasta
            order by r.fechaHora desc
            """)
    List<Retiro> buscar(@Param("medicoId") Long medicoId, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select coalesce(sum(r.monto), 0) from Retiro r where r.anulado = false and (:medicoId is null or r.medico.id = :medicoId)")
    BigDecimal totalRetiros(@Param("medicoId") Long medicoId);

    /**
     * Detecta un doble-submit del mismo retiro (doble click, reintento de red): mismo médico,
     * monto y quién lo entregó, en los últimos segundos — ver ventana en CajaService. Sin un
     * turno que lo distinga (a diferencia de Ingreso), la ventana se mantiene corta a propósito
     * para no bloquear dos retiros reales seguidos por el mismo monto.
     */
    @Query("""
            select count(r) > 0 from Retiro r
            where r.medico.id = :medicoId
            and r.monto = :monto
            and r.entregadoPor.id = :entregadoPorId
            and r.anulado = false
            and r.fechaHora >= :desde
            """)
    boolean existeDuplicadoReciente(@Param("medicoId") Long medicoId, @Param("monto") BigDecimal monto,
            @Param("entregadoPorId") Long entregadoPorId, @Param("desde") Instant desde);
}
