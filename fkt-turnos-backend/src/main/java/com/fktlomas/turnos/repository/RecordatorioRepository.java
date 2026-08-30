package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    /**
     * Con join fetch a propósito: RecordatorioService.procesarPendientes() usa esto
     * fuera de una transacción explícita (para no tener una conexión abierta durante
     * los envíos), así que el turno/médico/especialidad/paciente tienen que quedar
     * completamente cargados acá — si no, acceder a esos campos más tarde revienta
     * con LazyInitializationException ("no Session").
     */
    @Query("""
            select r from Recordatorio r
            join fetch r.turno t
            join fetch t.medico m
            join fetch m.especialidad
            join fetch t.paciente
            where t.id = :turnoId
            """)
    Optional<Recordatorio> findByTurnoId(@Param("turnoId") Long turnoId);

    @Query("""
            select r from Recordatorio r
            join fetch r.turno t
            join fetch t.medico m
            join fetch m.especialidad
            join fetch t.paciente
            order by t.fecha desc, t.horaInicio desc
            """)
    List<Recordatorio> listarConDetalle();
}
