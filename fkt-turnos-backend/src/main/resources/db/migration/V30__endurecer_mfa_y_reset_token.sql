-- Endurecimiento de autenticación (revisión de seguridad, octubre 2026).

-- 1) Protección contra reuso de códigos 2FA: se guarda el último "paso" TOTP (ventana de
--    30 segundos) aceptado en un login, y un código de ese paso o anterior se rechaza aunque
--    siga dentro de su ventana de validez. Ver TotpService.pasoCoincidente.
ALTER TABLE usuarios ADD COLUMN mfa_ultimo_paso BIGINT;

-- 2) El token de "olvidé mi contraseña" pasa a guardarse hasheado (SHA-256), no en texto
--    plano. Los tokens que ya estaban guardados en texto plano dejan de servir con el código
--    nuevo (se buscan por hash), así que se limpian: quien tuviera un link pendiente solo
--    tiene que pedir uno nuevo. Duran 60 minutos, así que el impacto es mínimo.
UPDATE usuarios SET reset_token = NULL, reset_token_expira = NULL WHERE reset_token IS NOT NULL;
