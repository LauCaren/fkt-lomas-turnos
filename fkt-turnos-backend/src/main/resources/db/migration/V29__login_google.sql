-- "Iniciar sesión con Google" para pacientes (ver AuthService.loginConGoogle). Nullable y
-- único: la mayoría de las cuentas no lo tienen (login con email/contraseña de siempre),
-- pero cuando está presente identifica de forma única al usuario de Google (claim "sub" del
-- ID token) — más estable que el email, que en teoría podría cambiar del lado de Google.
ALTER TABLE usuarios ADD COLUMN google_id VARCHAR(255) UNIQUE;
