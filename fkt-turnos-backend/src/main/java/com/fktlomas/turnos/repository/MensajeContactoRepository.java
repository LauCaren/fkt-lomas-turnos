package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.MensajeContacto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Long> {

    long countByLeidoFalse();
}
