-- Solo para los tests (H2 sin Flyway): la tabla de locks de las tareas programadas, igual que
-- la migración V32__shedlock.sql. Hibernate no la crea porque no es una entidad.
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
