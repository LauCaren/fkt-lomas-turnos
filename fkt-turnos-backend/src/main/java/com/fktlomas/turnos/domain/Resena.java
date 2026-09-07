package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Calificación de un paciente sobre un turno puntual que ya tuvo — a diferencia de las reseñas
 * fijas del sitio de marketing, esta queda atada a un {@link Turno} real en estado
 * {@link EstadoAsistencia#ATENDIDO} (una por turno, ver el índice único en la migración), así
 * que no se puede fabricar sin haber sido atendido de verdad. Pasa por moderación de un
 * admin/secretaria (mismo patrón que {@link SolicitudBajaCuenta}) antes de mostrarse en
 * público — ver ResenaService.
 */
@Entity
@Table(name = "resenas")
@Getter
@Setter
@NoArgsConstructor
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    /** Denormalizado desde turno.medico (igual que Ingreso.medico) para no tener que atravesar el turno en cada consulta pública por médico. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false)
    private int puntaje;

    @Column(length = 500)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoResena estado = EstadoResena.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por_id")
    private Usuario resueltaPor;

    @Column(name = "resuelta_at")
    private Instant resueltaAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
