CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    dni VARCHAR(10) UNIQUE,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    telefono VARCHAR(30),
    genero VARCHAR(30),
    obra_social VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE especialidades (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE medicos (
    id BIGSERIAL PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    especialidad_id BIGINT NOT NULL REFERENCES especialidades(id),
    matricula VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT true,
    duracion_turno_minutos INTEGER NOT NULL DEFAULT 30
);

CREATE TABLE reglas_disponibilidad (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES medicos(id) ON DELETE CASCADE,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL
);

CREATE TABLE turnos (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES medicos(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    paciente_id BIGINT REFERENCES usuarios(id),
    motivo_consulta VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_turno_medico_fecha_hora UNIQUE (medico_id, fecha, hora_inicio)
);

CREATE INDEX idx_turnos_medico_fecha ON turnos (medico_id, fecha);
CREATE INDEX idx_turnos_paciente ON turnos (paciente_id);

CREATE TABLE mensajes_contacto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL,
    telefono VARCHAR(30),
    mensaje VARCHAR(2000) NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE postulaciones (
    id BIGSERIAL PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL,
    telefono VARCHAR(30),
    mensaje VARCHAR(2000),
    cv_path VARCHAR(255) NOT NULL,
    cv_nombre_original VARCHAR(255) NOT NULL,
    cv_content_type VARCHAR(100) NOT NULL,
    cv_size BIGINT NOT NULL,
    revisado BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
