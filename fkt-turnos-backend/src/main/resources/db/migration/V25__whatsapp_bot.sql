-- Conversación por número de WhatsApp: reemplaza el JWT/sesión de la web para un canal
-- donde Twilio manda un POST sin estado por cada mensaje entrante (ver WhatsAppBotService).
-- El "usuario de sistema" que actúa como actor de auditoría al reservar/cancelar en nombre
-- de un paciente lo crea WhatsAppBotAccountInitializer al arrancar (mismo patrón que
-- AdminBootstrapRunner), no esta migración — así usa el PasswordEncoder real en vez de un
-- hash BCrypt pegado a mano en SQL.
CREATE TABLE conversaciones_whatsapp (
    id BIGSERIAL PRIMARY KEY,
    -- E.164 normalizado (ej "+5491123456789"), sin el prefijo "whatsapp:" de Twilio. Es la
    -- clave de la máquina de estados, no la identidad del paciente (ver TelefonoNormalizador
    -- y el comentario en ConversacionWhatsApp sobre por qué el teléfono no identifica).
    telefono VARCHAR(20) NOT NULL UNIQUE,
    -- Paso actual de la máquina de estados — ver EstadoConversacion.
    estado VARCHAR(40) NOT NULL,
    -- Null hasta que el DNI ingresado matchea un usuarios.id existente. Se limpia cada vez
    -- que la conversación vuelve a INICIO (reset por inactividad o "salir") para que un
    -- número reciclado o reutilizado no herede una identificación vieja.
    paciente_id BIGINT REFERENCES usuarios(id),
    -- Selecciones de reserva en curso (especialidad/médico/fecha/turno elegidos, datos de
    -- alta de un paciente nuevo) como JSON simple: cambia de forma según el paso y nunca se
    -- consulta fuera de esta misma conversación, así que no amerita columnas propias.
    contexto_json TEXT,
    -- DNIs incorrectos consecutivos en esta conversación puntual. Además del rate limit por
    -- teléfono en RateLimiterService (ventana deslizante de verdad), este contador es lo que
    -- dispara el paso a BLOQUEADO_TEMPORAL sin depender de esa ventana.
    intentos_dni INT NOT NULL DEFAULT 0,
    ultima_actividad TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversaciones_whatsapp_ultima_actividad ON conversaciones_whatsapp (ultima_actividad);
