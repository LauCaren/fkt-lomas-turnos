#!/usr/bin/env bash
# Instala recordatorio-turnos como servicio en un servidor Linux/macOS.
#
# Uso:
#   ./install.sh [directorio_de_instalacion]
#
# Por defecto instala en /opt/recordatorio-turnos. Compila el proyecto con
# Maven, copia el jar y la configuración a ese directorio, y si el sistema
# tiene systemd genera y habilita un servicio para que corra en segundo
# plano y se reinicie solo ante fallas o reinicios del servidor.
set -euo pipefail

INSTALL_DIR="${1:-/opt/recordatorio-turnos}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> Compilando el proyecto con Maven..."
(cd "$SCRIPT_DIR" && mvn -q clean package)

JAR_ORIGEN="$SCRIPT_DIR/target/standalone/recordatorio-turnos.jar"
if [ ! -f "$JAR_ORIGEN" ]; then
    echo "No se encontró $JAR_ORIGEN. ¿Falló el build?" >&2
    exit 1
fi

SUDO=""
if [ ! -w "$(dirname "$INSTALL_DIR")" ]; then
    SUDO="sudo"
fi

echo "==> Instalando en $INSTALL_DIR ..."
$SUDO mkdir -p "$INSTALL_DIR/config" "$INSTALL_DIR/data"
$SUDO cp "$JAR_ORIGEN" "$INSTALL_DIR/recordatorio-turnos.jar"
$SUDO cp "$SCRIPT_DIR/sql/schema-origen.sql" "$INSTALL_DIR/schema-origen.sql"

if [ -f "$INSTALL_DIR/config/application.properties" ]; then
    echo "==> Ya existe $INSTALL_DIR/config/application.properties, no lo piso."
else
    $SUDO cp "$SCRIPT_DIR/config/application.properties.example" "$INSTALL_DIR/config/application.properties"
    echo "==> Se creó $INSTALL_DIR/config/application.properties -> completar antes de iniciar."
fi

if command -v systemctl >/dev/null 2>&1; then
    echo "==> Generando servicio systemd..."
    SERVICE_FILE=/etc/systemd/system/recordatorio-turnos.service
    JAVA_BIN="$(command -v java)"

    sudo tee "$SERVICE_FILE" > /dev/null <<EOF
[Unit]
Description=Recordatorio de turnos
After=network.target

[Service]
Type=simple
WorkingDirectory=$INSTALL_DIR
ExecStart=$JAVA_BIN -jar $INSTALL_DIR/recordatorio-turnos.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    echo
    echo "Instalación completa. Antes de arrancar:"
    echo "  1. Editá $INSTALL_DIR/config/application.properties con los datos reales."
    echo "  2. sudo systemctl enable --now recordatorio-turnos"
    echo "  3. Ver logs: journalctl -u recordatorio-turnos -f"
else
    echo
    echo "Instalación completa (no se detectó systemd)."
    echo "Antes de arrancar:"
    echo "  1. Editá $INSTALL_DIR/config/application.properties con los datos reales."
    echo "  2. cd $INSTALL_DIR && java -jar recordatorio-turnos.jar"
fi
