# Seguridad del sistema

Cómo está protegido cada componente y qué hay que configurar al desplegar. Pensado para quien
mantenga el sistema: cada punto dice dónde está implementado y por qué.

Última revisión: octubre de 2026.

---

## Checklist de despliegue (obligatorio)

Sin estos valores el sistema arranca igual, pero en modo "inseguro de desarrollo". Todos son
variables de entorno del backend (ver `fkt-turnos-backend/.env.example`):

- [ ] `JWT_SECRET`: valor aleatorio propio de al menos 48 bytes (`openssl rand -base64 48`). Si
      falta, cada reinicio invalida todas las sesiones.
- [ ] `MFA_ENCRYPTION_KEY`: 32 bytes en Base64 (`openssl rand -base64 32`). **Guardarla en un lugar
      seguro aparte**: si se pierde, ningún usuario con 2FA puede volver a entrar.
- [ ] `ADMIN_BOOTSTRAP_PASSWORD`: contraseña fuerte para la primera cuenta admin. Cambiarla después
      del primer login y activar la verificación en dos pasos.
- [ ] `DEMO_DATA_ENABLED` **sin definir o en `false`** (si queda en `true`, carga cuentas de prueba
      con una contraseña conocida).
- [ ] `SWAGGER_ENABLED` sin definir o en `false`.
- [ ] `CORS_ALLOWED_ORIGINS` solo con los dominios reales de la app y del sitio (más
      `capacitor://localhost` y `https://localhost` si se publican las apps de celular).
- [ ] `FORWARDED_HEADERS_STRATEGY=native` si hay un proxy o CDN adelante (si no, el límite de
      intentos por IP ve a todos con la IP del proxy).
- [ ] `MERCADOPAGO_WEBHOOK_SECRET` y `TWILIO_WEBHOOK_BASE_URL` si se activan esas integraciones
      (sin ellos los webhooks se rechazan, que es lo seguro).
- [ ] Base de datos con usuario y contraseña propios (no `fkt`/`fkt`), sin acceso público desde
      internet, y backups automáticos (ver `.github/workflows/backup.yml`).
- [ ] Frontend: `VITE_API_URL` con la URL real del backend **antes** de compilar: la política de
      seguridad (CSP) de la app se genera a partir de esa variable.
- [ ] Sitio: correr `node scripts/configurar-urls.mjs --app … --api …` dentro de `FKT Pagina`
      (actualiza config.js, los links y la CSP de `_headers`/`vercel.json`).

## Autenticación y sesiones

| Medida | Dónde |
|---|---|
| Contraseñas con BCrypt; mínimo 8 caracteres con mayúscula, minúscula, número y símbolo | `SecurityConfig`, `Patrones.PASSWORD_FUERTE` |
| Rechazo de contraseñas filtradas (Have I Been Pwned, k-anonymity: solo se manda un prefijo del hash) | `PasswordFiltradaService` |
| 2FA (TOTP) obligatorio para admin, secretaría y contaduría; opcional para pacientes y médicos | `AppUserDetails`, `AuthService` |
| Cada código 2FA sirve una sola vez (no se puede reusar dentro de su ventana de 90 s) | `TotpService.pasoCoincidente`, columna `usuarios.mfa_ultimo_paso` |
| Secreto 2FA cifrado en la base (AES-GCM) | `MfaSecretConverter` |
| Sesión (JWT): 7 días para pacientes, 12 h para el personal | `JwtService.generarToken`, `app.jwt.*` |
| Cambio de contraseña o "cerrar todas las sesiones" invalida los tokens anteriores | `JwtAuthFilter` (`passwordChangedAt`, `sessionsRevokedAt`) |
| Bloqueo por cuenta tras 5 intentos fallidos (15 min) | `LoginRateLimiterService` |
| Límite por IP en login, registro, recuperación, formularios públicos y webhooks | `RateLimitingFilter` (compara contra la ruta ya decodificada: no se puede saltear codificando caracteres) |
| Token de recuperación de contraseña: 256 bits, 60 minutos, guardado hasheado (SHA-256) | `AuthService.olvidePassword` |
| "Iniciar sesión con Google" solo para pacientes; el personal no puede vincular Google | `AuthService.loginConGoogle` |
| Emails sin distinguir mayúsculas | `DatosPersonales.normalizarEmail` en los DTO de entrada |
| El registro no revela si un DNI o email ya es paciente | `AuthService.MENSAJE_CUENTA_EXISTENTE` |
| Aviso por mail cuando el personal entra fuera del horario laboral | `AuthService.alertarLoginFueraDeHorarioSiCorresponde` |

## Autorización

- Puerta gruesa por URL en `SecurityConfig` (`/api/admin/**`, `/api/medico/**`, `/api/contador/**`)
  y control fino con `@PreAuthorize` en cada controller (admin vs. secretaría).
- Cada recurso de un paciente se verifica contra el usuario logueado (turnos, pagos, planes,
  notificaciones, lista de espera): un paciente no puede ver ni tocar lo de otro cambiando un id.
- Los médicos solo ven su propia agenda y las evoluciones que ellos cargaron. Cada apertura de
  historia clínica queda en la auditoría.
- Las operaciones administrativas sensibles quedan en `registros_auditoria` con quién, cuándo y
  el antes/después.

## Datos y archivos

- **Imágenes** (fotos de perfil, fotos y títulos de médicos): solo JPG/PNG de hasta 5 MB, se
  rechazan dimensiones mayores a 6000 px (evita "bombas" de descompresión) y se **re-codifican**:
  se guarda solo la imagen, sin metadatos EXIF (ubicación GPS) ni contenido escondido —
  `ImagenSegura`.
- **PDFs** (CVs, comprobantes, órdenes médicas): se valida la firma real del archivo, no solo la
  extensión.
- Los archivos se guardan con nombre aleatorio (UUID), fuera de cualquier carpeta pública, y se
  sirven solo a través de endpoints con control de acceso.
- **Logs**: emails y teléfonos enmascarados (`DatosPersonales.enmascararEmail/Telefono`).
- **Errores**: la API nunca devuelve trazas ni mensajes internos de librerías; solo los mensajes
  escritos para el usuario (`GlobalExceptionHandler.mensajeSeguro`).
- **Retención**: borrado automático de lo que vence (`RetencionDatosService`).

## Integraciones externas

- **Mercado Pago**: el webhook valida la firma HMAC (`x-signature`), consulta el pago a la API de
  MP con nuestro token (no confía en lo que trae la notificación), verifica monto y moneda antes de
  registrar el ingreso, y es idempotente ante reintentos.
- **Twilio (WhatsApp)**: el webhook valida la firma `X-Twilio-Signature`.
- **Google**: el ID token se verifica criptográficamente (firma, audiencia, emisor, expiración y
  email verificado).

## Navegador (app y sitio)

- **Content-Security-Policy estricta**: solo scripts propios (más el hash del script del tema).
  - App: generada en cada build a partir de `VITE_API_URL` (`vite/politicaSeguridad.js`) e
    inyectada como `<meta>`, igual para web, celular y escritorio.
  - Sitio: en `FKT Pagina/_headers` y `vercel.json`. Los scripts de cada página están en
    `assets/js/paginas/`, nunca embebidos en el HTML.
  - **Si se modifica el script inline del tema** (en `index.html` del sitio o de la app), hay que
    recalcular su hash: en la app se hace solo en el build; en el sitio, reemplazar el
    `sha256-…` de `_headers` y `vercel.json` por el que muestre el error de CSP en la consola.
- Cabeceras HSTS, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` y
  `frame-ancestors 'none'` (anti-clickjacking) en `_headers`/`vercel.json` de ambos.
- CORS sin credenciales y con lista cerrada de orígenes y cabeceras.
- Ningún dato que venga de la API se inserta como HTML sin escapar (React escapa por defecto; en
  el sitio se usa `textContent` o `escapeHtml`).
- Link de confirmación de turnos: "No voy" pide un clic explícito y la API es POST, así un
  antivirus de correo que abre el link no puede cancelar el turno solo.

## App de escritorio (Electron)

- `contextIsolation`, `sandbox` y sin `nodeIntegration`: la ventana es una pestaña de navegador
  sin acceso al sistema.
- No navega fuera de su origen; los links externos se abren en el navegador de la PC (solo
  https, mailto y tel).
- Todos los pedidos de permisos se rechazan, salvo la cámara (sin micrófono) pedida por la propia app para sacar la foto de perfil.

## Infraestructura

- Contenedor del backend sin root (`Dockerfile`), con memoria dimensionada al contenedor.
- Postgres de desarrollo publicado solo en `127.0.0.1` (`docker-compose.yml`).
- CI (`.github/workflows/ci.yml`): búsqueda de secretos (gitleaks), análisis de vulnerabilidades
  de dependencias e imagen (Trivy), SpotBugs y tests en cada push. Dependabot mantiene las
  dependencias al día.

## Limitaciones conocidas (para cuando el sistema crezca)

- Los límites de intentos (`RateLimiterService`, `LoginRateLimiterService`) viven en memoria: con
  **una sola instancia** del backend funcionan perfecto; si algún día se corren varias detrás de un
  balanceador, hay que moverlos a un almacenamiento compartido (Redis).
- La sesión se guarda en `localStorage` del navegador. Es la opción estándar para una SPA con
  API en otro dominio; la CSP estricta es la que la protege. Pasar a cookies `HttpOnly` requeriría
  servir la API bajo el mismo dominio que la app.
