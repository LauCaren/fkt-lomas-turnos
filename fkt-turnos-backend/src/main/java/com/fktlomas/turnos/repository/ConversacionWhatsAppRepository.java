package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.ConversacionWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface ConversacionWhatsAppRepository extends JpaRepository<ConversacionWhatsApp, Long> {
    Optional<ConversacionWhatsApp> findByTelefono(String telefono);

    /** Para la retención automática (ver RetencionDatosService). */
    @Transactional
    long deleteByUltimaActividadBefore(Instant limite);
}
