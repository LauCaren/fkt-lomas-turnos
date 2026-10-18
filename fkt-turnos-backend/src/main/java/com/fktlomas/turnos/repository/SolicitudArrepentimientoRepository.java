package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoArrepentimiento;
import com.fktlomas.turnos.domain.SolicitudArrepentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface SolicitudArrepentimientoRepository extends JpaRepository<SolicitudArrepentimiento, Long> {

    boolean existsByCodigo(String codigo);

    long countByEstado(EstadoArrepentimiento estado);

    /** Para la retención automática (ver RetencionDatosService): solo pedidos ya resueltos. */
    @Transactional
    long deleteByEstadoAndResueltaAtBefore(EstadoArrepentimiento estado, Instant limite);

    @Query("""
            select s from SolicitudArrepentimiento s
            left join fetch s.resueltaPor
            where (:estado is null or s.estado = :estado)
            order by s.createdAt desc
            """)
    List<SolicitudArrepentimiento> buscar(EstadoArrepentimiento estado);
}
