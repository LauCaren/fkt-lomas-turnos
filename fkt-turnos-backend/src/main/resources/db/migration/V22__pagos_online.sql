-- Un pago online creado nunca tiene a nadie de staff "registrándolo" (lo confirma el
-- webhook de Mercado Pago, no una persona) — antes esta columna era obligatoria en todos
-- los casos porque solo existía la carga manual desde caja.
ALTER TABLE ingresos ALTER COLUMN registrado_por_id DROP NOT NULL;

CREATE TABLE pagos_online (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL REFERENCES turnos(id),
    paciente_id BIGINT NOT NULL REFERENCES usuarios(id),
    monto NUMERIC(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    preference_id VARCHAR(100) NOT NULL,
    payment_id VARCHAR(100),
    ingreso_id BIGINT REFERENCES ingresos(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_pagos_online_turno ON pagos_online(turno_id);
CREATE INDEX idx_pagos_online_preference ON pagos_online(preference_id);
-- Único solo entre los que ya tienen payment_id (varios PENDIENTE de un turno nunca llegan
-- a tener el mismo payment_id de Mercado Pago) — evita duplicar el Ingreso si el webhook
-- reintenta la misma notificación de pago aprobado.
CREATE UNIQUE INDEX uq_pagos_online_payment_id ON pagos_online(payment_id) WHERE payment_id IS NOT NULL;
