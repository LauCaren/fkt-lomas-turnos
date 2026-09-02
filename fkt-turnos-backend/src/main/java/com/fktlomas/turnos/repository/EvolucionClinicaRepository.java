package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EvolucionClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface EvolucionClinicaRepository extends JpaRepository<EvolucionClinica, Long> {
    List<EvolucionClinica> findByMedicoIdAndPacienteIdOrderByCreatedAtDesc(Long medicoId, Long pacienteId);
    boolean existsByTurnoId(Long turnoId);
    List<EvolucionClinica> findByTurnoIdIn(Set<Long> turnoIds);

    /** Derecho de acceso del paciente a su propia historia clínica (Ley 26.529 art. 14) — a diferencia de la vista del médico, acá se listan las evoluciones de TODOS los médicos que lo atendieron. */
    List<EvolucionClinica> findByPacienteIdOrderByCreatedAtDesc(Long pacienteId);
}
