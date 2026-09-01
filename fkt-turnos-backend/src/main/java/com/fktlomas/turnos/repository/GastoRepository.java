package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    @Query("""
            select g from Gasto g
            join fetch g.categoria
            join fetch g.registradoPor
            where (:categoriaId is null or g.categoria.id = :categoriaId)
            and (:desde is null or g.fecha >= :desde)
            and (:hasta is null or g.fecha <= :hasta)
            order by g.fecha desc, g.createdAt desc
            """)
    List<Gasto> buscar(@Param("categoriaId") Long categoriaId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select coalesce(sum(g.monto), 0) from Gasto g where g.anulado = false")
    BigDecimal totalGastos();
}
