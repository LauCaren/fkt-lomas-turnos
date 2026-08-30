ALTER TABLE usuarios ADD COLUMN canal_notificacion_preferido VARCHAR(20) NOT NULL DEFAULT 'EMAIL';

CREATE TABLE recordatorios (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL UNIQUE REFERENCES turnos(id) ON DELETE CASCADE,
    canal VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INT NOT NULL DEFAULT 0,
    error_mensaje VARCHAR(500),
    fecha_envio TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recordatorios_estado ON recordatorios (estado);

-- Fila única de configuración editable desde el panel de admin (plantilla del
-- mensaje y con cuánta anticipación se manda). El intervalo de chequeo del
-- scheduler NO vive acá a propósito: queda en application.yml/variable de
-- entorno, no editable desde el panel (ver RecordatorioService).
CREATE TABLE configuracion_recordatorios (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    plantilla_mensaje TEXT NOT NULL,
    horas_anticipacion INT NOT NULL DEFAULT 24,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT configuracion_recordatorios_singleton CHECK (id = 1)
);

INSERT INTO configuracion_recordatorios (id, plantilla_mensaje, horas_anticipacion) VALUES (
    1,
    'Hola {paciente}, te recordamos tu turno con {profesional} ({especialidad}) el {fecha} a las {hora} hs en FKT Lomas, Larrea 496, Lomas de Zamora. Ante cualquier inconveniente para asistir, por favor avisanos. ¡Te esperamos!',
    24
);
