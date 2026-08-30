package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {
    List<Feriado> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
    boolean existsByFecha(LocalDate fecha);
}
