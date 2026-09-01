package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicos")
@Getter
@Setter
@NoArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(length = 50)
    private String matricula;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "duracion_turno_minutos", nullable = false)
    private int duracionTurnoMinutos = 30;

    @Column(name = "anos_experiencia")
    private Integer anosExperiencia;

    @Column(name = "foto_path")
    private String fotoPath;

    @Column(name = "foto_content_type", length = 100)
    private String fotoContentType;

    @Column(name = "titulo_imagen_path")
    private String tituloImagenPath;

    @Column(name = "titulo_imagen_content_type", length = 100)
    private String tituloImagenContentType;

    @Column(name = "cobra_copago_ioma", nullable = false)
    private boolean cobraCopagoIoma = false;

    /** Cuenta de login del médico (rol MEDICO) — null hasta que el admin le crea credenciales. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    /** Tarifa fija de consulta, autocompletada al cobrar en caja — editable por admin. */
    @Column(name = "tarifa_consulta", precision = 12, scale = 2)
    private java.math.BigDecimal tarifaConsulta;
}
