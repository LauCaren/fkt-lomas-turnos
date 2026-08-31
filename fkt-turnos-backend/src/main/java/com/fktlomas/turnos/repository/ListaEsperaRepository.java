package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoListaEspera;
import com.fktlomas.turnos.domain.ListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    List<ListaEspera> findByMedicoIdAndFechaAndEstado(Long medicoId, LocalDate fecha, EstadoListaEspera estado);

    Optional<ListaEspera> findByPacienteIdAndMedicoIdAndFechaAndEstado(
            Long pacienteId, Long medicoId, LocalDate fecha, EstadoListaEspera estado);
}
