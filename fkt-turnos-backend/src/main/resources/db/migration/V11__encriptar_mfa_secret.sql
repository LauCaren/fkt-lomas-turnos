-- mfa_secret pasa a guardarse encriptado (ver MfaSecretConverter) — el texto
-- cifrado en Base64 ocupa más que el Base32 plano original, así que se
-- ensancha la columna. Los secretos existentes quedan en texto plano y ya no
-- son compatibles con el nuevo formato: se limpian acá. En este momento
-- ninguna cuenta tiene mfa_habilitado=true con un secreto cargado, así que
-- no hay ninguna sesión con MFA activo que se vea afectada — si alguien tenía
-- un QR a mitad de escanear sin confirmar, simplemente tiene que generarlo de nuevo.
ALTER TABLE usuarios ALTER COLUMN mfa_secret TYPE VARCHAR(255);
UPDATE usuarios SET mfa_secret = NULL WHERE mfa_secret IS NOT NULL;
