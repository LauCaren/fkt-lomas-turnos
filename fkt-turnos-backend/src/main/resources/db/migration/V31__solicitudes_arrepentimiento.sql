-- "Botón de arrepentimiento" (Ley 24.240 art. 34, Resolución SCI 424/2020): pedidos de
-- revocación de un servicio contratado o pagado a distancia. Ver SolicitudArrepentimiento.
CREATE TABLE solicitudes_arrepentimiento (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre_completo VARCHAR(150) NOT NULL,
    dni VARCHAR(10),
    email VARCHAR(180) NOT NULL,
    telefono VARCHAR(30),
    detalle_operacion VARCHAR(1000) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    nota_resolucion VARCHAR(1000),
    resuelta_por_id BIGINT REFERENCES usuarios(id),
    resuelta_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_solicitudes_arrepentimiento_estado ON solicitudes_arrepentimiento(estado, created_at);
