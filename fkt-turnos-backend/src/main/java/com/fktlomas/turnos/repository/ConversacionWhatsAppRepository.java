package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.ConversacionWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversacionWhatsAppRepository extends JpaRepository<ConversacionWhatsApp, Long> {
    Optional<ConversacionWhatsApp> findByTelefono(String telefono);
}
