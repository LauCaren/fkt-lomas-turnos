package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoResena;
import com.fktlomas.turnos.domain.Resena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    boolean existsByTurnoId(Long turnoId);

    /** Para marcar en "mis turnos" cuáles ya tienen reseña cargada, sin una consulta por turno — ver TurnoService.misTurnos. */
    @Query("select r.turno.id from Resena r where r.turno.id in :turnoIds")
    List<Long> turnoIdsConResena(@Param("turnoIds") Set<Long> turnoIds);

    @Query("""
            select r from Resena r
            join fetch r.paciente
            join fetch r.medico
            where (:estado is null or r.estado = :estado)
            order by r.createdAt desc
            """)
    List<Resena> buscar(@Param("estado") EstadoResena estado);

    @Query("""
            select r from Resena r
            join fetch r.paciente
            join fetch r.medico m
            join fetch m.especialidad
            where m.id = :medicoId and r.estado = com.fktlomas.turnos.domain.EstadoResena.APROBADA
            order by r.createdAt desc
            """)
    List<Resena> aprobadasDeMedico(@Param("medicoId") Long medicoId);

    /** Para el sitio de marketing (FKT Pagina/opiniones.html): últimas reseñas aprobadas de cualquier médico del centro. */
    @Query("""
            select r from Resena r
            join fetch r.paciente
            join fetch r.medico m
            join fetch m.especialidad
            where r.estado = com.fktlomas.turnos.domain.EstadoResena.APROBADA
            order by r.createdAt desc
            """)
    List<Resena> ultimasAprobadas(Pageable pageable);

    @Query("select coalesce(avg(r.puntaje), 0) from Resena r where r.medico.id = :medicoId and r.estado = com.fktlomas.turnos.domain.EstadoResena.APROBADA")
    double promedioDeMedico(@Param("medicoId") Long medicoId);
}
