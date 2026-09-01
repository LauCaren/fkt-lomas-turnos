package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.RegistroAuditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    Optional<RegistroAuditoria> findTopByTipoEntidadAndEntidadIdAndDeshechoFalseOrderByCreatedAtDesc(String tipoEntidad, Long entidadId);

    @Query("""
            select r from RegistroAuditoria r
            left join fetch r.usuario
            where (:tipoEntidad is null or r.tipoEntidad = :tipoEntidad)
            and (:entidadId is null or r.entidadId = :entidadId)
            order by r.createdAt desc
            """)
    List<RegistroAuditoria> buscar(@Param("tipoEntidad") String tipoEntidad, @Param("entidadId") Long entidadId, Pageable pageable);
}
