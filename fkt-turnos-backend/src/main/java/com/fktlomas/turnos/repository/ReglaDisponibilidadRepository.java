package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.ReglaDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaDisponibilidadRepository extends JpaRepository<ReglaDisponibilidad, Long> {
    List<ReglaDisponibilidad> findByMedicoId(Long medicoId);
}
