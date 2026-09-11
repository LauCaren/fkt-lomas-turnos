package com.fktlomas.turnos.domain;

/**
 * De dónde salió (si salió) el consentimiento a la política de privacidad de este paciente —
 * separado de {@link Usuario#isAceptaPoliticaPrivacidad()} porque ese booleano por sí solo no
 * distingue "aceptó un checkbox explícito" de "se lo dimos por aceptado sin preguntarle nada".
 * Antes de esto, todo alta por teléfono o WhatsApp guardaba {@code true} sin que la persona
 * hubiera aceptado nada — ver el comentario en TurnoService.crearPacienteTelefonico.
 */
public enum OrigenConsentimiento {
    /** Registro web: checkbox obligatorio, validado también en el backend (ver RegistroRequest). */
    WEB,
    /** Alta por el bot de WhatsApp: solo recibió el aviso pasivo del primer mensaje del bot, nunca un opt-in explícito. */
    WHATSAPP,
    /** Alta telefónica por secretaria/admin: no hay ningún aviso de política de privacidad dado por el sistema. */
    TELEFONICA
}
