package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan de sesiones de kinesiología generado a partir de una orden médica: una cantidad
 * total de sesiones que se van reservando semana a semana en uno o más horarios fijos,
 * hasta completarlas. Requiere validación manual del admin antes de generar los turnos.
 */
@Entity
@Table(name = "planes_kinesiologia")
@Getter
@Setter
@NoArgsConstructor
public class PlanKinesiologia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(name = "cantidad_sesiones", nullable = false)
    private int cantidadSesiones;

    @Column(name = "sesiones_generadas", nullable = false)
    private int sesionesGeneradas = 0;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoPlanKinesiologia estado = EstadoPlanKinesiologia.PENDIENTE_VALIDACION;

    @Column(name = "orden_medica_path", nullable = false)
    private String ordenMedicaPath;

    @Column(name = "orden_medica_content_type", length = 100)
    private String ordenMedicaContentType;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por_id")
    private Usuario validadoPor;

    @Column(name = "validado_at")
    private Instant validadoAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanKinesiologiaHorario> horarios = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
