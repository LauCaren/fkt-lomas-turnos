package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.CategoriaGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaGastoRepository extends JpaRepository<CategoriaGasto, Long> {
    List<CategoriaGasto> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
}
