package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.MensajeContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Long> {

    long countByLeidoFalse();

    /** Para la retención automática (ver RetencionDatosService). */
    @Transactional
    long deleteByCreatedAtBefore(Instant limite);
}
