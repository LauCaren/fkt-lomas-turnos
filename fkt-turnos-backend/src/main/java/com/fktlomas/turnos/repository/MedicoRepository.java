package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {
    List<Medico> findByEspecialidadIdAndActivoTrue(Long especialidadId);
    List<Medico> findByActivoTrue();
    Optional<Medico> findByUsuarioId(Long usuarioId);
}
