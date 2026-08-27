# fkt-turnos-backend

API REST en Spring Boot 3 (Java 17) para el sistema de turnos, contacto y postulaciones de FKT Lomas.

## Requisitos

- Java 17
- Maven (o usar el wrapper `./mvnw` si se agrega)
- Postgres (levantado con `docker compose up -d` desde la raíz del repo)

## Variables de entorno

| Variable | Default (dev) | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fkt_turnos` | Conexión a Postgres |
| `DB_USERNAME` / `DB_PASSWORD` | `fkt` / `fkt` | Credenciales de la base |
| `JWT_SECRET` | valor de desarrollo | **Cambiar en producción.** Secreto para firmar los JWT |
| `JWT_EXPIRATION_MINUTES` | `10080` (7 días) | Duración del token |
| `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` | ver `application.yml` | Credenciales del usuario admin (Fernando) que se crea automáticamente al arrancar si no existe. Cambiar la contraseña desde el panel una vez logueado. |
| `ADMIN_NOTIFY_EMAIL` | `fernando@fktlomas.com` | A dónde llegan los avisos de contacto/postulaciones |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos (separados por coma) |
| `MAIL_ENABLED` | `false` | Poner en `true` para enviar emails de verdad |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | vacío | Credenciales SMTP. Recomendado: [Brevo](https://www.brevo.com) (plan gratuito, 300 emails/día) u otro proveedor similar |
| `MAIL_FROM` | `no-reply@fktlomas.com` | Remitente de los emails |
| `UPLOADS_DIR` | `./uploads` | Carpeta donde se guardan los CVs recibidos |

Con `MAIL_ENABLED=false` (default) los emails no se envían, solo se loguean — así se puede probar todo el flujo sin tener un SMTP configurado.

## Correr en local

```bash
JAVA_HOME=<ruta a un JDK 17> \
ADMIN_BOOTSTRAP_EMAIL=fernando@fktlomas.com \
ADMIN_BOOTSTRAP_PASSWORD=elegí-una-clave \
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. Documentación interactiva (Swagger) en `http://localhost:8080/docs`.

## Tests

```bash
mvn test
```

Incluye un test que verifica que, ante dos intentos de reserva sobre el mismo turno, solo uno tiene éxito (la reserva usa un `UPDATE` atómico condicionado a `estado = DISPONIBLE`, sin necesidad de locks explícitos).

## Modelo de datos (resumen)

- **Usuario**: pacientes (`role=PACIENTE`) y el admin (`role=ADMIN`), en la misma tabla.
- **Especialidad** / **Medico**: catálogo administrado desde el panel.
- **ReglaDisponibilidad**: plantilla semanal (día + hora inicio/fin) por médico, usada para generar turnos concretos.
- **Turno**: la unidad real de reserva (`DISPONIBLE` / `RESERVADO` / `CANCELADO`), con `@Version` para optimistic locking además del `UPDATE` condicional.
- **MensajeContacto** / **Postulacion**: formularios públicos del sitio estático.

El schema se versiona con Flyway (`src/main/resources/db/migration`), no con `ddl-auto`.
