package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.CanalNotificacion;
import com.fktlomas.turnos.domain.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Intenta notificar a un paciente por su canal preferido y, si falla, reintenta una vez por el
 * otro canal — solo si el paciente tiene un dato de contacto real para ese canal alternativo
 * (ver {@link Usuario#tieneEmailReal()}/{@link Usuario#tieneTelefono()}). Compartido por
 * RecordatorioService (recordatorios de turno) y NotificacionTurnoService (confirmación,
 * cancelación, avisos de kinesiología/lista de espera) — antes cada uno tenía su propia copia
 * de esta misma lógica de reintento.
 */
@Slf4j
@Service
public class CanalFallbackService {

    @FunctionalInterface
    public interface EnvioPorCanal {
        void enviar(CanalNotificacion canal) throws Exception;
    }

    /**
     * @return el canal que efectivamente se usó (el preferido, o el alternativo si hizo falta el reintento).
     * @throws Exception si tanto el canal preferido como el alternativo (cuando existía) fallaron —
     *         el llamador decide qué hacer con eso (loguear y seguir, marcar el envío como fallido, etc).
     */
    public CanalNotificacion enviarConFallback(Usuario paciente, EnvioPorCanal envio) throws Exception {
        CanalNotificacion preferido = paciente.getCanalNotificacionPreferido();
        try {
            envio.enviar(preferido);
            return preferido;
        } catch (Exception fallaOriginal) {
            CanalNotificacion alternativo = preferido == CanalNotificacion.EMAIL ? CanalNotificacion.WHATSAPP : CanalNotificacion.EMAIL;
            boolean tieneContactoAlternativo = alternativo == CanalNotificacion.EMAIL ? paciente.tieneEmailReal() : paciente.tieneTelefono();
            if (!tieneContactoAlternativo) {
                throw fallaOriginal;
            }
            log.warn("Fallo la notificación a {} por {} ({}), reintentando por {}",
                    paciente.getEmail(), preferido, fallaOriginal.getMessage(), alternativo);
            envio.enviar(alternativo);
            return alternativo;
        }
    }
}
