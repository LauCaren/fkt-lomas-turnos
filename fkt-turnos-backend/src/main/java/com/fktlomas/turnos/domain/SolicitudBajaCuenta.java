package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Pedido de baja de cuenta (derecho de supresión, Ley 25.326) — a diferencia de un borrado
 * directo, queda en una cola para que un admin/secretaria lo procese. Al aprobarse, se
 * anonimiza el PII del Usuario en vez de borrar la fila: los turnos, evoluciones e ingresos ya
 * vinculados a esa cuenta tienen que conservarse por la obligación legal de retención de
 * historia clínica (Ley 26.529) y de registros contables — ver SolicitudBajaService.aprobar.
 */
@Entity
@Table(name = "solicitudes_baja_cuenta")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudBajaCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudBaja estado = EstadoSolicitudBaja.PENDIENTE;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
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
