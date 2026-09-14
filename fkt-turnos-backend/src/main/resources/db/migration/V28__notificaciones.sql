-- Notificaciones dentro de la app — un lugar donde cualquier usuario ve qué pasó con sus
-- turnos sin depender de revisar el email o WhatsApp (que además pueden fallar, ver
-- CanalFallbackService). turno_id es nullable e intencionalmente SIN foreign key: un turno
-- puede reprogramarse o el registro purgarse con el tiempo, y la notificación tiene que seguir
-- legible (con turno_id null si el turno ya no existe) en vez de desaparecer o romper el insert.
CREATE TABLE notificaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    tipo VARCHAR(40) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    turno_id BIGINT,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- La consulta de siempre es "mis notificaciones, las más nuevas primero" y "cuántas no leídas
-- tengo" — ambas filtran por usuario_id y a menudo por leida, así que un índice compuesto
-- cubre las dos sin necesitar dos índices separados.
CREATE INDEX idx_notificaciones_usuario ON notificaciones(usuario_id, leida, created_at DESC);
