package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoSolicitudBaja;
import com.fktlomas.turnos.domain.SolicitudBajaCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SolicitudBajaCuentaRepository extends JpaRepository<SolicitudBajaCuenta, Long> {

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoSolicitudBaja estado);

    long countByEstado(EstadoSolicitudBaja estado);

    @Query("""
            select s from SolicitudBajaCuenta s
            join fetch s.usuario
            where (:estado is null or s.estado = :estado)
            order by s.createdAt desc
            """)
    List<SolicitudBajaCuenta> buscar(EstadoSolicitudBaja estado);
}
