package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.ConfirmacionPrevia;
import com.fktlomas.turnos.domain.EstadoAsistencia;
import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.domain.Turno;
import com.fktlomas.turnos.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByMedicoIdAndFechaAndEstadoOrderByHoraInicioAsc(Long medicoId, LocalDate fecha, EstadoTurno estado);

    /** Para el panel "requiere atención" del resumen admin — turnos de hoy que todavía no
     * respondieron al recordatorio de confirmación previa. */
    long countByFechaAndEstadoAndConfirmacionPrevia(LocalDate fecha, EstadoTurno estado, ConfirmacionPrevia confirmacionPrevia);

    /** Turnos reservados de hoy de un paciente — para el check-in por QR de la sala de espera. */
    @Query("""
            select t from Turno t
            join fetch t.medico m
            where t.paciente.id = :pacienteId
            and t.fecha = :fecha
            and t.estado = com.fktlomas.turnos.domain.EstadoTurno.RESERVADO
            order by t.horaInicio asc
            """)
    List<Turno> turnosDeHoyDePaciente(@Param("pacienteId") Long pacienteId, @Param("fecha") LocalDate fecha);

    /**
     * Próximas fechas (a partir de, sin incluir, :desde) en las que ese médico tiene al menos
     * un turno DISPONIBLE — para sugerirle al paciente el próximo día con lugar cuando la fecha
     * que eligió no tiene horarios. Pageable acota cuántas fechas devolver (ver TurnoService).
     */
    @Query("""
            select t.fecha from Turno t
            where t.medico.id = :medicoId
            and t.estado = com.fktlomas.turnos.domain.EstadoTurno.DISPONIBLE
            and t.fecha > :desde
            group by t.fecha
            order by t.fecha asc
            """)
    List<LocalDate> proximasFechasConDisponibilidad(@Param("medicoId") Long medicoId, @Param("desde") LocalDate desde, Pageable pageable);

    List<Turno> findByPacienteIdOrderByFechaDescHoraInicioDesc(Long pacienteId);

    /** Cuántas veces este paciente no se presentó a un turno — señal informativa para el staff, ver AdminPacienteService.ficha. */
    long countByPacienteIdAndEstadoAsistencia(Long pacienteId, EstadoAsistencia estadoAsistencia);

    boolean existsByMedicoIdAndFechaAndHoraInicio(Long medicoId, LocalDate fecha, java.time.LocalTime horaInicio);

    @Query("""
            select t from Turno t
            join fetch t.medico m
            join fetch m.especialidad
            left join fetch t.paciente
            where t.fecha between :desde and :hasta
            and (:medicoId is null or m.id = :medicoId)
            and (:especialidadId is null or m.especialidad.id = :especialidadId)
            order by t.fecha asc, t.horaInicio asc
            """)
    List<Turno> findAgenda(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                           @Param("medicoId") Long medicoId, @Param("especialidadId") Long especialidadId);

    @Query("""
            select t from Turno t
            join fetch t.medico m
            join fetch m.especialidad
            left join fetch t.paciente
            where t.fecha between :desde and :hasta
            and (:medicoId is null or m.id = :medicoId)
            and (:especialidadId is null or m.especialidad.id = :especialidadId)
            and (:estado is null or t.estado = :estado)
            order by t.fecha asc, t.horaInicio asc
            """)
    Page<Turno> buscarTurnos(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                             @Param("medicoId") Long medicoId, @Param("especialidadId") Long especialidadId,
                             @Param("estado") EstadoTurno estado, Pageable pageable);

    /**
     * Update atómico: solo reserva si sigue DISPONIBLE. Si otra transacción ya lo tomó,
     * esta operación afecta 0 filas y el service la traduce en un 409 sin necesidad de locks explícitos.
     */
    /**
     * Candidatos a recordatorio: RESERVADO dentro de un rango de fechas (día completo,
     * a favor de mano ancha) — RecordatorioService filtra en memoria por hora exacta
     * contra la ventana real de anticipación, ya con el paciente y el médico cargados.
     */
    @Query("""
            select t from Turno t
            join fetch t.medico m
            join fetch m.especialidad
            join fetch t.paciente
            where t.estado = com.fktlomas.turnos.domain.EstadoTurno.RESERVADO
            and t.fecha between :desde and :hasta
            order by t.fecha asc, t.horaInicio asc
            """)
    List<Turno> findReservadosEntreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /**
     * Al reservar se limpian confirmacionPrevia/tokenConfirmacion: son del paciente que tenía
     * este turno antes de liberarse (si lo rechazó, ese aviso queda visible en la agenda
     * mientras el turno sigue libre — ver TurnoService.cancelarInterno), no tienen sentido
     * para quien lo está reservando ahora.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Turno t set t.estado = com.fktlomas.turnos.domain.EstadoTurno.RESERVADO, " +
            "t.paciente = :paciente, t.motivoConsulta = :motivo, " +
            "t.confirmacionPrevia = com.fktlomas.turnos.domain.ConfirmacionPrevia.SIN_RESPUESTA, t.tokenConfirmacion = null " +
            "where t.id = :turnoId and t.estado = com.fktlomas.turnos.domain.EstadoTurno.DISPONIBLE")
    int reservarSiDisponible(@Param("turnoId") Long turnoId, @Param("paciente") Usuario paciente, @Param("motivo") String motivo);

    /** Cola de espera del día: pacientes que llegaron (check-in) y todavía no fueron atendidos, en el orden de su turno. */
    @Query("""
            select t from Turno t
            join fetch t.medico m
            join fetch m.especialidad
            join fetch t.paciente
            where t.fecha = :fecha
            and t.estado = com.fktlomas.turnos.domain.EstadoTurno.RESERVADO
            and t.estadoAsistencia = com.fktlomas.turnos.domain.EstadoAsistencia.LLEGO
            and (:medicoId is null or m.id = :medicoId)
            order by t.horaInicio asc
            """)
    List<Turno> colaEspera(@Param("fecha") LocalDate fecha, @Param("medicoId") Long medicoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Turno t set t.estadoAsistencia = :estado where t.id = :turnoId")
    int actualizarAsistencia(@Param("turnoId") Long turnoId, @Param("estado") EstadoAsistencia estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Turno t set t.estadoAsistencia = com.fktlomas.turnos.domain.EstadoAsistencia.ATENDIDO " +
            "where t.id = :turnoId and t.medico.id = :medicoId and t.estadoAsistencia = com.fktlomas.turnos.domain.EstadoAsistencia.LLEGO")
    int marcarAtendidoSiLlego(@Param("turnoId") Long turnoId, @Param("medicoId") Long medicoId);

    @Query("""
            select t from Turno t
            join fetch t.medico m
            join fetch m.especialidad
            left join fetch t.paciente
            where t.planKinesiologia.id = :planId
            order by t.fecha asc, t.horaInicio asc
            """)
    List<Turno> findByPlanKinesiologiaId(@Param("planId") Long planId);

    /** Libera (vuelve DISPONIBLE) las sesiones futuras aún reservadas de un plan que se cancela o pausa. */
    List<Turno> findByPlanKinesiologiaIdAndEstadoAndFechaGreaterThanEqual(Long planId, EstadoTurno estado, LocalDate fecha);

    @Query("""
            select t from Turno t
            join fetch t.medico m
            join fetch m.especialidad
            join fetch t.paciente
            where t.tokenConfirmacion = :token
            """)
    java.util.Optional<Turno> findByTokenConfirmacion(@Param("token") String token);
}
