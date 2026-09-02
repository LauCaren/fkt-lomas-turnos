CREATE TABLE solicitudes_baja_cuenta (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    motivo TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo TEXT,
    resuelta_por_id BIGINT REFERENCES usuarios(id),
    resuelta_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_solicitudes_baja_usuario ON solicitudes_baja_cuenta(usuario_id);
CREATE INDEX idx_solicitudes_baja_estado ON solicitudes_baja_cuenta(estado);
