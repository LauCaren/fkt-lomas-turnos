package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {
}
