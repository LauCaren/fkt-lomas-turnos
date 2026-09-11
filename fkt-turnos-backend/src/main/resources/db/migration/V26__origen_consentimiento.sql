-- Registra de dónde salió (si salió) el consentimiento a la política de privacidad, separado
-- del booleano acepta_politica_privacidad — ver domain/OrigenConsentimiento.java. Nullable:
-- las cuentas ya existentes (staff, pacientes cargados antes de esta columna) no tienen forma
-- de saber retroactivamente cuál fue su origen real, así que quedan sin dato en vez de
-- inventarles uno.
ALTER TABLE usuarios ADD COLUMN origen_consentimiento VARCHAR(20);
