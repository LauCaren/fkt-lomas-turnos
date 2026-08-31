-- Esquema REAL de la base del sistema de turnos de FKT Lomas
-- (repo fkt-turnos-backend, PostgreSQL — ver db/migration/V1__init.sql ahí).
-- Esta app nunca crea ni modifica estas tablas: solo hace SELECT sobre ellas
-- desde JdbcOrigenRepository, para sincronizar hacia su propia base local
-- (ver schema-local.sql). Se deja acá solo como referencia — si el esquema
-- real cambia, hay que actualizar el SQL en JdbcOrigenRepository.
--
-- Notas:
--   * Pacientes y personal comparten "usuarios", distinguidos por "role"
--     ('PACIENTE' / 'ADMIN') — solo se sincronizan los PACIENTE.
--   * "turnos" también contiene los horarios libres (estado DISPONIBLE);
--     solo interesan los reservados (estado RESERVADO, con paciente_id).
--   * No hay concepto de "consultorio": FKT Lomas tiene una sola sede.

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    dni VARCHAR(10) UNIQUE,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    telefono VARCHAR(30),
    genero VARCHAR(30),
    obra_social VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'PACIENTE' | 'ADMIN'
    acepta_politica_privacidad BOOLEAN NOT NULL DEFAULT false,
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

CREATE TABLE turnos (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES medicos(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE', -- DISPONIBLE | RESERVADO | CANCELADO
    paciente_id BIGINT REFERENCES usuarios(id),
    motivo_consulta VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
