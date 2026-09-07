#!/usr/bin/env bash
# Backup MANUAL/LOCAL de la base de Postgres de FKT Lomas — complementa (no reemplaza) a
# scripts/backup-db.sh + .github/workflows/backup.yml, que es el backup diario automático
# a S3/R2 en producción. Este script es para: (a) backups locales durante desarrollo, sin
# depender de un bucket, y (b) un backup manual "por las dudas" antes de una migración o
# operación riesgosa contra la base real (export PG* con los datos del host administrado).
# También sirve por tener restauración (restore-db-local.sh), que el backup a S3 no incluye.
#
# Sin argumentos apunta al Postgres local de docker-compose.yml (localhost:5433, fkt/fkt/fkt_turnos).
#
# Uso local:      ./scripts/backup-db-local.sh
# Uso en prod:     PGHOST=... PGPORT=... PGDATABASE=... PGUSER=... PGPASSWORD=... ./scripts/backup-db-local.sh
# Restaurar:       ./scripts/restore-db-local.sh backups/fkt_turnos_20260907_093000.sql.gz
set -euo pipefail

: "${PGHOST:=localhost}"
: "${PGPORT:=5433}"
: "${PGDATABASE:=fkt_turnos}"
: "${PGUSER:=fkt}"
: "${PGPASSWORD:=fkt}"
: "${BACKUP_DIR:="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/backups"}"
: "${RETENTION_DIAS:=14}"

export PGPASSWORD

mkdir -p "$BACKUP_DIR"
archivo="$BACKUP_DIR/fkt_turnos_$(date +%Y%m%d_%H%M%S).sql.gz"

echo "Respaldando $PGDATABASE@$PGHOST:$PGPORT -> $archivo"
pg_dump --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" --format=plain --no-owner --no-privileges --clean --if-exists \
  | gzip > "$archivo"

tamano=$(du -h "$archivo" | cut -f1)
echo "Listo ($tamano)."

# Retención: no dejar crecer backups/ para siempre en discos locales/CI.
find "$BACKUP_DIR" -name 'fkt_turnos_*.sql.gz' -mtime "+$RETENTION_DIAS" -print -delete
