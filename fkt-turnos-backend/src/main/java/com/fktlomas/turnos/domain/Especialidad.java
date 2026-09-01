package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "especialidades")
@Getter
@Setter
@NoArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /** Kinesiología tiene un flujo de reserva propio (orden médica + plan de sesiones recurrentes). */
    @Column(name = "es_kinesiologia", nullable = false)
    private boolean esKinesiologia = false;
}
