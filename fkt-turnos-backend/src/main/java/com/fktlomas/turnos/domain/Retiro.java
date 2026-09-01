package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** El médico retira lo que tiene acumulado; queda quién se lo entregó, cuánto y cuándo, exacto. */
@Entity
@Table(name = "retiros")
@Getter
@Setter
@NoArgsConstructor
public class Retiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregado_por_id", nullable = false)
    private Usuario entregadoPor;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(nullable = false)
    private boolean anulado = false;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @PrePersist
    void prePersist() {
        if (fechaHora == null) {
            fechaHora = Instant.now();
        }
    }
}
