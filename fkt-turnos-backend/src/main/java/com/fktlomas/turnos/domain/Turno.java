package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "turnos")
@Getter
@Setter
@NoArgsConstructor
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurno estado = EstadoTurno.DISPONIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Usuario paciente;

    @Column(name = "motivo_consulta", length = 500)
    private String motivoConsulta;

    /**
     * Check-in de sala de espera: PENDIENTE hasta que la secretaria marca que el paciente
     * llegó; el médico marca ATENDIDO desde su propia pantalla al terminar la consulta.
     * Independiente de {@link #estado} (disponible/reservado/cancelado).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_asistencia", nullable = false, length = 20)
    private EstadoAsistencia estadoAsistencia = EstadoAsistencia.PENDIENTE;

    /** Si este turno es una sesión generada por un plan de kinesiología, a cuál pertenece. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_kinesiologia_id")
    private PlanKinesiologia planKinesiologia;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
