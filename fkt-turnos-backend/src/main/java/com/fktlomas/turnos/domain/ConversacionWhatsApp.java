package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Estado de una conversación de WhatsApp por número de teléfono. El teléfono es solo la
 * clave de esta sesión, no la identidad del paciente — Usuario.telefono no tiene
 * normalización ni constraint de unicidad, y un número puede reasignarse con el tiempo, así
 * que quien identifica al paciente es el DNI (pacienteId queda null hasta que se valida uno
 * contra Usuario). Reemplaza al JWT para un canal donde Twilio manda un POST sin estado por
 * cada mensaje entrante — ver WhatsAppBotService.
 */
@Entity
@Table(name = "conversaciones_whatsapp")
@Getter
@Setter
@NoArgsConstructor
public class ConversacionWhatsApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoConversacion estado = EstadoConversacion.INICIO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Usuario paciente;

    @Column(name = "contexto_json")
    private String contextoJson;

    @Column(name = "intentos_dni", nullable = false)
    private int intentosDni = 0;

    @Column(name = "ultima_actividad", nullable = false)
    private Instant ultimaActividad;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant ahora = Instant.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        if (ultimaActividad == null) {
            ultimaActividad = ahora;
        }
    }

    /** Vuelve al inicio sin arrastrar nada de la sesión anterior — ver reset por inactividad y "salir" en WhatsAppBotService. */
    public void reiniciar() {
        estado = EstadoConversacion.INICIO;
        paciente = null;
        contextoJson = null;
        intentosDni = 0;
    }
}
