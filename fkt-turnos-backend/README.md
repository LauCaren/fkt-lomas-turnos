# fkt-turnos-backend

API REST en Spring Boot 3 (Java 17) para el sistema de turnos, contacto y postulaciones de FKT Lomas.

## Requisitos

- Java 17
- Postgres (levantado con `docker compose up -d` desde la raíz del repo)
- No hace falta instalar Maven: el repo trae el wrapper (`./mvnw` en Mac/Linux, `mvnw.cmd` en Windows).

## Variables de entorno

| Variable | Default (dev) | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/fkt_turnos` | Conexión a Postgres. Se usa el puerto 5433 (no el 5432 estándar) para no pisarse con un Postgres nativo que puedas tener instalado en la máquina |
| `DB_USERNAME` / `DB_PASSWORD` | `fkt` / `fkt` | Credenciales de la base |
| `JWT_SECRET` | valor de desarrollo | **Cambiar en producción.** Secreto para firmar los JWT |
| `JWT_EXPIRATION_MINUTES` | `10080` (7 días) | Duración del token |
| `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` | ver `application.yml` | Credenciales del usuario admin (Fernando) que se crea automáticamente al arrancar si no existe. Cambiar la contraseña desde el panel una vez logueado. |
| `ADMIN_NOTIFY_EMAIL` | `fernando@fktlomas.com` | A dónde llegan los avisos de contacto/postulaciones |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos (separados por coma) |
| `FRONTEND_URL` | `http://localhost:5173` | Base para armar el link de "olvidé mi contraseña" que se manda por email |
| `MAIL_ENABLED` | `false` | Poner en `true` para enviar emails de verdad |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | vacío | Credenciales SMTP. Recomendado: [Brevo](https://www.brevo.com) (plan gratuito, 300 emails/día) u otro proveedor similar |
| `MAIL_FROM` | `no-reply@fktlomas.com` | Remitente de los emails |
| `UPLOADS_DIR` | `./uploads` | Carpeta donde se guardan los CVs recibidos |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | vacío | Credenciales de [Twilio](https://www.twilio.com), para los canales SMS y WhatsApp de los recordatorios de turno |
| `TWILIO_NUMERO_ORIGEN` | vacío | Número de Twilio para SMS (formato `+1415XXXXXXX`) |
| `TWILIO_NUMERO_ORIGEN_WHATSAPP` | vacío | Número de Twilio para WhatsApp, **sin** el prefijo `whatsapp:` (el código lo agrega solo) |
| `RECORDATORIOS_INTERVALO_MINUTOS` | `15` | Cada cuánto corre el job que revisa turnos próximos y manda recordatorios. La anticipación (cuántas horas antes) y el texto del mensaje se configuran desde el panel de admin, no acá |

Con `MAIL_ENABLED=false` (default) los emails no se envían, solo se loguean — así se puede probar todo el flujo sin tener un SMTP configurado. Mismo criterio para SMS/WhatsApp: sin credenciales de Twilio, esos canales simplemente no se registran (los recordatorios de pacientes con ese canal preferido quedan en FALLIDO con un mensaje de error claro, no rompen el resto).

## Correr en local

Mac/Linux:
```bash
ADMIN_BOOTSTRAP_EMAIL=fernando@fktlomas.com \
ADMIN_BOOTSTRAP_PASSWORD=elegí-una-clave \
./mvnw spring-boot:run
```

Windows (PowerShell):
```powershell
$env:ADMIN_BOOTSTRAP_EMAIL="fernando@fktlomas.com"
$env:ADMIN_BOOTSTRAP_PASSWORD="elegí-una-clave"
.\mvnw.cmd spring-boot:run
```

Si tenés más de un JDK instalado y Maven elige uno que no es la versión 17, seteá `JAVA_HOME` apuntando al JDK 17 antes de correr el comando.

La API queda en `http://localhost:8080`. Documentación interactiva (Swagger) en `http://localhost:8080/docs`.

## Tests

```bash
./mvnw test
```

Incluye un test que verifica que, ante dos intentos de reserva sobre el mismo turno, solo uno tiene éxito (la reserva usa un `UPDATE` atómico condicionado a `estado = DISPONIBLE`, sin necesidad de locks explícitos).

## Modelo de datos (resumen)

- **Usuario**: pacientes (`role=PACIENTE`) y el admin (`role=ADMIN`), en la misma tabla.
- **Especialidad** / **Medico**: catálogo administrado desde el panel.
- **ReglaDisponibilidad**: plantilla semanal (día + hora inicio/fin) por médico, usada para generar turnos concretos.
- **Turno**: la unidad real de reserva (`DISPONIBLE` / `RESERVADO` / `CANCELADO`), con `@Version` para optimistic locking además del `UPDATE` condicional.
- **MensajeContacto** / **Postulacion**: formularios públicos del sitio estático.

El schema se versiona con Flyway (`src/main/resources/db/migration`), no con `ddl-auto`.
