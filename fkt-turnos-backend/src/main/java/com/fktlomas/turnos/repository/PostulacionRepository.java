package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    long countByRevisadoFalse();

    /** Para la retención automática (ver RetencionDatosService). */
    List<Postulacion> findByCreatedAtBefore(Instant limite);
}
