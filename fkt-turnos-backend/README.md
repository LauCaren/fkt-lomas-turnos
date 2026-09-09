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
| `UPLOADS_DIR` | `./uploads` | Carpeta donde se guardan fotos de médicos y CVs cuando `STORAGE_TYPE=local` |
| `STORAGE_TYPE` | `local` | `local` (disco, dev) o `s3` (bucket S3-compatible — AWS S3 o Cloudflare R2 — **obligatorio en cualquier host con disco efímero**, ver `S3FileStorageService`) |
| `STORAGE_S3_BUCKET` | vacío | Nombre del bucket. Requerido si `STORAGE_TYPE=s3` |
| `STORAGE_S3_REGION` | `auto` | Región AWS (ej. `sa-east-1`). Con R2 dejar `auto`, no aplica |
| `STORAGE_S3_ENDPOINT` | vacío | Vacío para AWS S3 real. Para Cloudflare R2: `https://<account_id>.r2.cloudflarestorage.com` |
| `STORAGE_S3_ACCESS_KEY` / `STORAGE_S3_SECRET_KEY` | vacío | Credenciales del bucket. El bucket debe quedar **privado** — los archivos siempre se sirven a través del backend (que aplica su propio control de acceso), nunca con una URL pública de S3/R2 |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | vacío | Credenciales de [Twilio](https://www.twilio.com), para el canal WhatsApp de los recordatorios de turno y para el bot de reservas (ver más abajo) |
| `TWILIO_NUMERO_ORIGEN_WHATSAPP` | vacío | Número de Twilio para WhatsApp, **sin** el prefijo `whatsapp:` (el código lo agrega solo) |
| `TWILIO_WEBHOOK_BASE_URL` | vacío | URL pública **https** de este backend (sin barra final), tiene que coincidir exactamente con la que se configura en la consola de Twilio como "when a message comes in" para el bot de WhatsApp (`/api/whatsapp/webhook`, ver `TwilioSignatureVerifier`). Sin esto, **el bot rechaza todo mensaje entrante** — mismo criterio que `MERCADOPAGO_WEBHOOK_SECRET` |
| `RECORDATORIOS_INTERVALO_MINUTOS` | `15` | Cada cuánto corre el job que revisa turnos próximos y manda recordatorios. La anticipación (cuántas horas antes) y el texto del mensaje se configuran desde el panel de admin, no acá |
| `BACKEND_URL` | vacío | URL pública **https** de este backend, para que Mercado Pago le pegue al webhook de confirmación de pago (`/api/pagos/mercadopago/webhook`, ver `MercadoPagoService`). Sin esto, la preferencia de pago se crea igual pero sin `notification_url` — la confirmación nunca llega |
| `MERCADOPAGO_ACCESS_TOKEN` / `MERCADOPAGO_PUBLIC_KEY` | vacío | Credenciales de [Mercado Pago](https://www.mercadopago.com.ar/developers/panel) ("Tus integraciones" > Credenciales). Sin `MERCADOPAGO_ACCESS_TOKEN`, el pago online al reservar queda deshabilitado y solo se puede pagar en el consultorio, como antes |
| `MERCADOPAGO_WEBHOOK_SECRET` | vacío | Firma secreta del webhook ("Tus integraciones" > Webhooks > Configurar notificaciones). A diferencia de otras integraciones de este proyecto, sin esto **el webhook rechaza toda notificación** — ver `MercadoPagoService.validarFirma` |
| `CHEQUEO_PASSWORD_FILTRADA` | `true` | Chequea cada contraseña nueva contra la API pública de HaveIBeenPwned (sin API key, k-anonymity) además de la regla de fuerza — ver `PasswordFiltradaService`. Poner en `false` solo si el host no tiene salida a internet; de todos modos, sin conexión el chequeo falla "abierto" (no bloquea) |

Con `MAIL_ENABLED=false` (default) los emails no se envían, solo se loguean — así se puede probar todo el flujo sin tener un SMTP configurado. Mismo criterio para WhatsApp: sin credenciales de Twilio, ese canal simplemente no se registra (los recordatorios de pacientes con ese canal preferido quedan en FALLIDO con un mensaje de error claro, no rompen el resto). El canal SMS no existe en el sistema — solo email y WhatsApp.

### Bot de reservas por WhatsApp

Un paciente puede escribirle al número de WhatsApp del centro para identificarse por DNI y reservar, ver o cancelar un turno, o anotarse en lista de espera — sin usar la web ni una contraseña (ver `WhatsAppBotService`, `WhatsAppWebhookController`). El teléfono que escribe nunca identifica por sí solo a un paciente (esa columna no tiene normalización ni es única): la identidad siempre es el DNI, igual que cuando una secretaria carga un turno por teléfono (`TurnoService.reservarParaPaciente`) — este bot automatiza ese mismo nivel de confianza, no lo baja. Deliberadamente fuera de alcance: kinesiología (se filtra del menú y redirige a un humano), pago desde el bot, y comprensión de lenguaje libre — todo el flujo es por menú numerado en texto plano.

Para que funcione de verdad hace falta, además de `TWILIO_ACCOUNT_SID`/`TWILIO_AUTH_TOKEN`/`TWILIO_NUMERO_ORIGEN_WHATSAPP`/`TWILIO_WEBHOOK_BASE_URL`: (1) que este backend sea alcanzable por HTTPS desde internet — en local, un túnel tipo `cloudflared`; y (2) configurar esa URL + `/api/whatsapp/webhook` como el webhook de "when a message comes in" del número de WhatsApp en la consola de Twilio. Sin la firma `X-Twilio-Signature` válida, el bot rechaza el mensaje (`TwilioSignatureVerifier`) — no hay forma de probarlo end-to-end sin al menos el WhatsApp Sandbox de Twilio.

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

Cubren, entre otras cosas: que ante dos intentos de reserva sobre el mismo turno solo uno tiene éxito (la reserva usa un `UPDATE` atómico condicionado a `estado = DISPONIBLE`, sin locks explícitos), los límites de autorización por rol, la lógica de caja/kinesiología/undo, el rate limiting de login, el fallback de canal de las notificaciones al paciente — turno reservado/cancelado, recordatorio, plan de kinesiología, lista de espera (`CanalFallbackService`: si falla el canal preferido, reintenta por el otro solo si hay un dato de contacto real para ese canal) — y la máquina de estados del bot de WhatsApp (kinesiología excluida del menú, bloqueo tras varios DNI fallidos, y la firma `X-Twilio-Signature` con el mismo algoritmo real que usa `com.twilio.security.RequestValidator`).

## Modelo de datos (resumen)

- **Usuario**: pacientes (`role=PACIENTE`) y el admin (`role=ADMIN`), en la misma tabla.
- **Especialidad** / **Medico**: catálogo administrado desde el panel.
- **ReglaDisponibilidad**: plantilla semanal (día + hora inicio/fin) por médico, usada para generar turnos concretos.
- **Turno**: la unidad real de reserva (`DISPONIBLE` / `RESERVADO` / `CANCELADO`), con `@Version` para optimistic locking además del `UPDATE` condicional.
- **MensajeContacto** / **Postulacion**: formularios públicos del sitio estático.

El schema se versiona con Flyway (`src/main/resources/db/migration`), no con `ddl-auto`.
