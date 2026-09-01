package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoPlanKinesiologia;
import com.fktlomas.turnos.domain.PlanKinesiologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanKinesiologiaRepository extends JpaRepository<PlanKinesiologia, Long> {

    @Query("""
            select p from PlanKinesiologia p
            join fetch p.paciente
            join fetch p.medico m
            join fetch m.especialidad
            where p.paciente.id = :pacienteId
            order by p.createdAt desc
            """)
    List<PlanKinesiologia> findByPacienteIdOrderByCreatedAtDesc(@Param("pacienteId") Long pacienteId);

    @Query("""
            select p from PlanKinesiologia p
            join fetch p.paciente
            join fetch p.medico m
            join fetch m.especialidad
            where (:estado is null or p.estado = :estado)
            order by p.createdAt desc
            """)
    List<PlanKinesiologia> buscar(@Param("estado") EstadoPlanKinesiologia estado);
}
