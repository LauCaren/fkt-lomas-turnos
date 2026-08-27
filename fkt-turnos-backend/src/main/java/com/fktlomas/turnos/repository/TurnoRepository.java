package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(Long medicoId, LocalDate fecha, EstadoTurno estado);

    List<Turno> findByPacienteIdOrderByFechaDescHoraInicioDesc(Long pacienteId);

    boolean existsByMedicoIdAndFechaAndHoraInicio(Long medicoId, LocalDate fecha, java.time.LocalTime horaInicio);

    /**
     * Update atómico: solo reserva si sigue DISPONIBLE. Si otra transacción ya lo tomó,
     * esta operación afecta 0 filas y el service la traduce en un 409 sin necesidad de locks explícitos.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Turno t set t.estado = com.fktlomas.turnos.domain.EstadoTurno.RESERVADO, " +
            "t.paciente = :paciente, t.motivoConsulta = :motivo " +
            "where t.id = :turnoId and t.estado = com.fktlomas.turnos.domain.EstadoTurno.DISPONIBLE")
    int reservarSiDisponible(@Param("turnoId") Long turnoId, @Param("paciente") Usuario paciente, @Param("motivo") String motivo);
}
