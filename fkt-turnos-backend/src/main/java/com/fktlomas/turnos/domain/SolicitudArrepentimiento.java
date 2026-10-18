package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Pedido hecho con el "Botón de arrepentimiento" (Ley 24.240 art. 34 y Resolución 424/2020 de
 * la Secretaría de Comercio Interior): quien contrató o pagó un servicio a distancia puede
 * revocar esa aceptación dentro de los 10 días corridos, sin costo y sin tener que registrarse
 * ni iniciar sesión. El centro tiene que darle un código de identificación del trámite dentro
 * de las 24 h por el mismo medio — acá se genera al instante, se muestra en pantalla y se manda
 * por mail (ver ArrepentimientoService).
 *
 * <p>Es una entidad propia (y no un MensajeContacto más) porque es un registro con valor legal:
 * hay que poder demostrar cuándo se pidió, qué código se entregó y cómo se resolvió.
 */
@Entity
@Table(name = "solicitudes_arrepentimiento")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudArrepentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código que se le entrega a la persona (ej. "ARR-7KQ2M9XD"). Único. */
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 10)
    private String dni;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 30)
    private String telefono;

    /** Qué quiere revocar, en palabras de la persona: turno, fecha, profesional, pago, etc. */
    @Column(name = "detalle_operacion", nullable = false, length = 1000)
    private String detalleOperacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoArrepentimiento estado = EstadoArrepentimiento.PENDIENTE;

    @Column(name = "nota_resolucion", length = 1000)
    private String notaResolucion;

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
