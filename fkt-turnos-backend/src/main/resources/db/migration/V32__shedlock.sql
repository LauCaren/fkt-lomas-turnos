-- Locks de las tareas programadas (ShedLock): con más de una instancia del backend, cada tarea
-- (recordatorios, resumen semanal, reporte mensual, retención de datos) corre en una sola.
-- Estructura estándar de ShedLock para JdbcTemplateLockProvider. Ver SchedulerLockConfig.
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
