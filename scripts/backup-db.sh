#!/usr/bin/env bash
# Dump de Postgres → gzip → cifrado (age) → sube al mismo bucket S3/R2 que ya usa el backend
# para archivos (uploads, ver fkt-turnos-backend/README.md), bajo el prefijo "backups/". Corre
# desacoplado del proceso de la app (ver .github/workflows/backup.yml) — no tiene sentido
# instalar postgresql-client en la imagen de runtime del backend solo para esto.
#
# El dump es la base completa en texto plano — nombres, DNI, teléfonos, historia clínica, todo
# lo que hay en la base salvo lo que ya está hasheado/cifrado a nivel de columna (contraseñas,
# secretos MFA). Sin cifrar el archivo en sí, cualquiera que consiga la credencial del bucket
# (clave filtrada, secret de CI comprometido, bucket mal configurado) se lleva la base entera
# con un solo `aws s3 cp`. age(1) es cifrado asimétrico de un solo archivo, sin passphrase que
# manejar acá: este script solo necesita la clave PÚBLICA (segura en un secret de CI, no da
# para descifrar nada); la clave privada para restaurar se guarda aparte, fuera de este repo.
#
# Requiere: pg_dump (paquete postgresql-client), aws-cli, gzip, age (`age-keygen` para generar
# el par de claves la primera vez — ver README.md de la raíz del repo).
#
# Variables de entorno requeridas:
#   DATABASE_URL           postgres://usuario:password@host:puerto/basededatos
#   STORAGE_S3_BUCKET      bucket destino (el mismo que STORAGE_S3_BUCKET del backend)
#   STORAGE_S3_ACCESS_KEY / STORAGE_S3_SECRET_KEY   credenciales del bucket
#   STORAGE_S3_ENDPOINT    URL del endpoint S3-compatible (vacío para AWS S3 real, obligatorio para R2)
#   STORAGE_S3_REGION      región (con R2 va "auto")
#   BACKUP_AGE_PUBLIC_KEY  clave pública age ("age1...") contra la que se cifra el dump
#
# El bucket debe tener configurada una regla de expiración/lifecycle para borrar backups
# viejos automáticamente (ej. 30-90 días) — no lo hace este script a propósito: reimplementar
# el borrado acá es un riesgo de bug que borre backups buenos, y toda plataforma S3-compatible
# (AWS S3, Cloudflare R2) ya trae esa función de forma nativa y confiable.

set -euo pipefail

: "${DATABASE_URL:?Falta la variable DATABASE_URL}"
: "${STORAGE_S3_BUCKET:?Falta la variable STORAGE_S3_BUCKET}"
: "${STORAGE_S3_ACCESS_KEY:?Falta la variable STORAGE_S3_ACCESS_KEY}"
: "${STORAGE_S3_SECRET_KEY:?Falta la variable STORAGE_S3_SECRET_KEY}"
: "${BACKUP_AGE_PUBLIC_KEY:?Falta la variable BACKUP_AGE_PUBLIC_KEY (clave pública age — ver README.md)}"

export AWS_ACCESS_KEY_ID="$STORAGE_S3_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$STORAGE_S3_SECRET_KEY"
export AWS_DEFAULT_REGION="${STORAGE_S3_REGION:-auto}"

ENDPOINT_ARGS=()
if [ -n "${STORAGE_S3_ENDPOINT:-}" ]; then
  ENDPOINT_ARGS=(--endpoint-url "$STORAGE_S3_ENDPOINT")
fi

TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"
ARCHIVO="fkt_turnos_${TIMESTAMP}.sql.gz.age"
DESTINO_LOCAL="/tmp/${ARCHIVO}"

echo "Generando dump cifrado..."
pg_dump "$DATABASE_URL" | gzip | age -r "$BACKUP_AGE_PUBLIC_KEY" > "$DESTINO_LOCAL"

TAMANIO=$(du -h "$DESTINO_LOCAL" | cut -f1)
echo "Dump generado (${TAMANIO}). Subiendo a s3://${STORAGE_S3_BUCKET}/backups/${ARCHIVO}..."

aws s3 cp "$DESTINO_LOCAL" "s3://${STORAGE_S3_BUCKET}/backups/${ARCHIVO}" "${ENDPOINT_ARGS[@]}"

rm -f "$DESTINO_LOCAL"
echo "Listo."
