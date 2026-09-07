#!/usr/bin/env bash
# Restaura un backup generado por backup-db-local.sh (o descargado a mano del bucket S3/R2
# del backup diario, ver .github/workflows/backup.yml). Sin argumentos apunta al Postgres
# local de docker-compose.yml — para restaurar en el host real, exportá las PG* antes.
#
# Uso: ./scripts/restore-db-local.sh backups/fkt_turnos_20260907_093000.sql.gz
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Uso: $0 <archivo-backup.sql.gz>" >&2
  exit 1
fi
archivo="$1"
if [ ! -f "$archivo" ]; then
  echo "No existe el archivo: $archivo" >&2
  exit 1
fi

: "${PGHOST:=localhost}"
: "${PGPORT:=5433}"
: "${PGDATABASE:=fkt_turnos}"
: "${PGUSER:=fkt}"
: "${PGPASSWORD:=fkt}"
export PGPASSWORD

echo "Esto SOBRESCRIBE los datos actuales de $PGDATABASE@$PGHOST:$PGPORT con el contenido de $archivo."
read -r -p "Confirmá escribiendo el nombre de la base (\"$PGDATABASE\") para continuar: " confirmacion
if [ "$confirmacion" != "$PGDATABASE" ]; then
  echo "Cancelado." >&2
  exit 1
fi

gunzip -c "$archivo" | psql --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" --set ON_ERROR_STOP=on
echo "Restauración completa."
