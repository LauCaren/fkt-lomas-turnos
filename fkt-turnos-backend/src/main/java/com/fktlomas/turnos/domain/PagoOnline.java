package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * El intento de pago online de un turno vía Mercado Pago (Checkout Pro), desde que se crea
 * la preferencia hasta que el webhook confirma (o rechaza) el pago. Vive aparte de
 * {@link Ingreso} a propósito: un PENDIENTE o RECHAZADO nunca debe tocar caja, solo un
 * APROBADO genera el Ingreso correspondiente (ver MercadoPagoService).
 */
@Entity
@Table(name = "pagos_online")
@Getter
@Setter
@NoArgsConstructor
public class PagoOnline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPagoOnline estado = EstadoPagoOnline.PENDIENTE;

    @Column(name = "preference_id", nullable = false, length = 100)
    private String preferenceId;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingreso_id")
    private Ingreso ingreso;

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
