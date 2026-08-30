ALTER TABLE usuarios ADD COLUMN reset_token VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN reset_token_expira TIMESTAMPTZ;
CREATE INDEX idx_usuarios_reset_token ON usuarios (reset_token);

ALTER TABLE usuarios ADD COLUMN mfa_secret VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN mfa_habilitado BOOLEAN NOT NULL DEFAULT false;
