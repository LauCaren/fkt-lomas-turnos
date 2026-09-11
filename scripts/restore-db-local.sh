#!/usr/bin/env bash
# Restaura un backup generado por backup-db-local.sh (.sql.gz, sin cifrar) o descargado a mano
# del bucket S3/R2 del backup diario (.sql.gz.age, cifrado con age — ver
# .github/workflows/backup.yml y scripts/backup-db.sh). Sin argumentos apunta al Postgres local
# de docker-compose.yml — para restaurar en el host real, exportá las PG* antes.
#
# Para un archivo .sql.gz.age hace falta la clave PRIVADA age que generó
# scripts/backup-db.sh (age-keygen) — nunca vive en este repo ni en CI, se guarda aparte
# (ver README.md de la raíz). Pasala con AGE_IDENTITY_FILE apuntando al archivo de la clave,
# o con AGE_IDENTITY con la clave en texto (por ejemplo desde un gestor de secretos).
#
# Uso: ./scripts/restore-db-local.sh backups/fkt_turnos_20260907_093000.sql.gz
#      AGE_IDENTITY_FILE=~/.config/age/fkt-backups.key ./scripts/restore-db-local.sh fkt_turnos_20260910-060000.sql.gz.age
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Uso: $0 <archivo-backup.sql.gz|.sql.gz.age>" >&2
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

if [[ "$archivo" == *.age ]]; then
  if [ -n "${AGE_IDENTITY_FILE:-}" ]; then
    gunzip -c <(age -d -i "$AGE_IDENTITY_FILE" "$archivo") | psql --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" --set ON_ERROR_STOP=on
  elif [ -n "${AGE_IDENTITY:-}" ]; then
    gunzip -c <(age -d -i <(printf '%s\n' "$AGE_IDENTITY") "$archivo") | psql --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" --set ON_ERROR_STOP=on
  else
    echo "Archivo cifrado (.age): falta AGE_IDENTITY_FILE o AGE_IDENTITY con la clave privada." >&2
    exit 1
  fi
else
  gunzip -c "$archivo" | psql --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE" --set ON_ERROR_STOP=on
fi
echo "Restauración completa."
