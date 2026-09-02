package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /**
     * "OrderByCreatedAtDescIdDesc": createdAt solo tiene precisión de milisegundo, así que dos
     * operaciones seguidas sobre el mismo turno (ej. reservar y cancelar, ambas parte del mismo
     * click o de una carga en lote) pueden empatar en el timestamp — sin el desempate por id
     * (BIGSERIAL, monótonamente creciente sin importar la precisión del reloj), el orden entre
     * empatados queda a criterio de la base y "deshacer" puede volver al estado equivocado.
     */
    Optional<RegistroAuditoria> findTopByTipoEntidadAndEntidadIdAndDeshechoFalseOrderByCreatedAtDescIdDesc(String tipoEntidad, Long entidadId);

    @Query("""
            select r from RegistroAuditoria r
            left join fetch r.usuario
            where (:tipoEntidad is null or r.tipoEntidad = :tipoEntidad)
            and (:entidadId is null or r.entidadId = :entidadId)
            order by r.createdAt desc, r.id desc
            """)
    List<RegistroAuditoria> buscar(@Param("tipoEntidad") String tipoEntidad, @Param("entidadId") Long entidadId, Pageable pageable);

    /** Listado general para el panel de auditoría del admin — todos los filtros son opcionales. */
    @Query("""
            select r from RegistroAuditoria r
            left join fetch r.usuario
            where (:tipoEntidad is null or r.tipoEntidad = :tipoEntidad)
            and (:usuarioId is null or r.usuario.id = :usuarioId)
            and (:desde is null or r.createdAt >= :desde)
            and (:hasta is null or r.createdAt < :hasta)
            order by r.createdAt desc, r.id desc
            """)
    Page<RegistroAuditoria> buscarGeneral(@Param("tipoEntidad") String tipoEntidad, @Param("usuarioId") Long usuarioId,
                                           @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable pageable);
}
