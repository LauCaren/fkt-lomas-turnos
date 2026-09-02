# FKT Lomas — Sistema de turnos, contacto y postulaciones

Este repositorio contiene tres proyectos independientes:

```
FKT Pagina/              Sitio estático de marketing (ya existía). Se agregaron:
                          - alquiler-consultorios.html
                          - trabaja-con-nosotros.html
                          - link "Pedir turno" apuntando a la app de turnos
                          - columna "Otros" en el footer con los dos links nuevos
                          - CSS/JS compartidos entre las 10 páginas en assets/
                            (antes cada .html tenía su propia copia duplicada)

fkt-turnos-backend/      API REST en Spring Boot (Java 17): auth JWT, turnos,
                          kinesiología, caja/gastos, historia clínica, auditoría,
                          panel admin, contacto y postulaciones. Cuatro roles:
                          PACIENTE, ADMIN, MEDICO, CONTADOR (ver "Roles" abajo).

fkt-turnos-frontend/     App React + Redux Toolkit: registro/login de pacientes,
                          reserva de turnos, panel de administración, y los
                          portales de médico y contador. Empaquetable también
                          como app de escritorio (Electron) y apps móviles
                          (Capacitor).

docker-compose.yml       Postgres local para desarrollo.
```

## Cómo levantar todo en local

1. Base de datos:
   ```bash
   docker compose up -d
   ```
2. Backend (en `fkt-turnos-backend/`):
   ```bash
   ADMIN_BOOTSTRAP_EMAIL=fernando@fktlomas.com ADMIN_BOOTSTRAP_PASSWORD=elegí-una-clave ./mvnw spring-boot:run
   ```
   Ver `fkt-turnos-backend/README.md` para todas las variables de entorno disponibles.
3. Frontend (en `fkt-turnos-frontend/`):
   ```bash
   npm install
   npm run dev
   ```
   Queda en `http://localhost:5173`.
4. Sitio estático (en `FKT Pagina/`):
   ```bash
   python3 -m http.server 5500
   ```
   Servirlo en el puerto 5500 no es opcional para el desarrollo completo: el logo de la app de turnos linkea de vuelta a `http://localhost:5500`, y los formularios de contacto/CV necesitan que esté servido por http (no abierto como archivo local) para poder llamar a la API.

## App móvil (Capacitor)

`fkt-turnos-frontend/` está envuelto con [Capacitor](https://capacitorjs.com) para generar las apps nativas de iOS y Android a partir del mismo código React (carpetas `ios/` y `android/` dentro de `fkt-turnos-frontend/`, ya versionadas). Antes de compilar, siempre hay que buildear el web y sincronizar:

```bash
cd fkt-turnos-frontend
npm run build
npx cap sync
```

**iOS** (necesita Xcode instalado, no hace falta CocoaPods — usa Swift Package Manager):
```bash
cd ios/App
xcodebuild -project App.xcodeproj -scheme App -sdk iphonesimulator -configuration Debug -derivedDataPath build
```
O abrí `ios/App/App.xcodeproj` directo en Xcode y correlo desde ahí.

**Android** (necesita el Android SDK — `brew install --cask android-commandlinetools` alcanza, no hace falta Android Studio completo para compilar por consola):
```bash
cd android
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug
```
Importante: el build de Android necesita **JDK 21** (no el JDK 17 que usa el backend) y el SDK Platform 36 (`sdkmanager "platforms;android-36" "build-tools;36.0.0"`). El APK queda en `android/app/build/outputs/apk/debug/app-debug.apk`.

Íconos y splash screens salen de `fkt-turnos-frontend/resources/` (generados a partir del logo real) — para regenerarlos tras cambiar el logo: `npx capacitor-assets generate`.

## Levantarlo en Windows

Los pasos son los mismos, solo cambia la sintaxis de terminal. Instalá antes:

- **Git para Windows** ([git-scm.com](https://git-scm.com/download/win)) — incluye Git Bash y el Credential Manager que te va a pedir loguearte con GitHub al clonar (el repo es privado).
- **Java 17** — [Eclipse Temurin 17 (MSI)](https://adoptium.net/temurin/releases/?version=17), tildando la opción de setear `JAVA_HOME` durante la instalación. **No hace falta instalar Maven aparte**, el repo ya trae el wrapper (`mvnw.cmd`).
- **Node.js LTS** ([nodejs.org](https://nodejs.org)) — incluye `npm`.
- **Docker Desktop** ([docker.com](https://www.docker.com/products/docker-desktop)) — el instalador te va a guiar para habilitar WSL2 si no lo tenés. Sin esto no hay base de datos.

Clonar el repo (PowerShell o Git Bash):
```powershell
git clone https://github.com/LauCaren/fkt-lomas-turnos.git
cd fkt-lomas-turnos
```

Después, en PowerShell, desde la carpeta del repo:

```powershell
docker compose up -d
```
```powershell
cd fkt-turnos-backend
$env:ADMIN_BOOTSTRAP_EMAIL="fernando@fktlomas.com"
$env:ADMIN_BOOTSTRAP_PASSWORD="cambiar123"
.\mvnw.cmd spring-boot:run
```
En otra terminal:
```powershell
cd fkt-turnos-frontend
npm install
npm run dev
```
Y en una tercera, para el sitio estático:
```powershell
cd "FKT Pagina"
python -m http.server 5500
```
(si no tenés Python instalado, `npx serve -l 5500` hace lo mismo).

**Importante:** cada máquina tiene su propia base de datos local (el contenedor de Docker no se sincroniza entre tu Mac y tu PC). Vas a arrancar con las especialidades y los 9 médicos reales ya cargados (vienen de una migración), pero **no** con los pacientes/turnos de prueba que haya en la otra máquina — son entornos de desarrollo independientes, no una base compartida.

## Roles

El sistema tiene cuatro roles, cada uno con su propio login y su propia superficie de la API (`SecurityConfig` bloquea `/api/admin/**`, `/api/medico/**` y `/api/contador/**` por rol a nivel de servidor, no solo ocultando botones en el frontend):

- **PACIENTE**: reserva turnos, pide plan de kinesiología, ve/cancela sus propios turnos, gestiona su cuenta.
- **ADMIN** (la secretaria/Fernando): panel completo — turnos, médicos, caja, reportes, todo lo de abajo.
- **MEDICO**: portal propio (`/medico`) — agenda del día, cola de espera, historia clínica de sus pacientes, su saldo en caja. No ve nada de otros médicos ni del negocio en general.
- **CONTADOR**: portal de solo lectura (`/contador`) — únicamente movimientos de caja (ingresos, retiros, gastos). Sin acceso a turnos, pacientes ni historias clínicas, ni para leer ni para escribir.

## Estado actual

Implementado y probado de punta a punta:

**Turnos y pacientes**
- Registro de pacientes (DNI, obra social, nombre, email, teléfono, género) y login con JWT.
- Reserva de turnos por especialidad → médico → fecha → horario, con bloqueo real contra doble reserva (dos personas no pueden tomar el mismo horario).
- Copago de IOMA: el paciente ve el aviso de copago antes de confirmar si su obra social y la especialidad lo requieren.
- Cancelación de turnos propios (libera el horario para otros pacientes), lista de espera, y confirmación previa del turno (recordatorio con confirmar/cancelar) el día anterior.
- Si la fecha elegida no tiene horarios libres, el asistente sugiere las próximas fechas con lugar para ese médico (en vez de que el paciente tenga que ir probando fecha por fecha).
- Los pacientes pueden editar sus propios datos y cambiar su contraseña desde "Mi cuenta". "Olvidé mi contraseña" por email con link temporal.
- Feriados nacionales de Argentina 2026 precargados (Ley 27.399, verificados contra el calendario oficial) — se excluyen automáticamente al generar turnos, y se marcan visualmente en la agenda. El admin puede sumar cierres propios del centro vía `POST /api/admin/feriados`.

**Kinesiología**
- Plan de sesiones con orden médica obligatoria (subida de foto/PDF, validada por firma binaria del archivo, no por extensión ni por el Content-Type que manda el navegador) y validación manual del admin antes de generar los turnos.
- El paciente elige sus horarios fijos semanales de una lista de horarios reales del kinesiólogo (derivados de su `ReglaDisponibilidad`), no escribiendo día/hora a mano — o puede elegir "me da igual quién me atienda" para ver los horarios de todos los kinesiólogos juntos.
- Pausar/reanudar un plan libera y vuelve a generar las sesiones futuras sin perder el conteo de sesiones ya hechas.

**Caja, gastos y el rol CONTADOR**
- Caja física con ingresos (cobros a pacientes, por médico) y retiros, más gastos categorizados del consultorio — todo con saldo acumulado por médico.
- Cierre/resumen mensual con reenvío automático por email el último día del mes.
- Exportación a PDF y Excel (Apache PDFBox / POI, ambas Apache-2.0) de los movimientos, para que la secretaria le mande al dueño un resumen de un día o período puntual.
- El rol CONTADOR ve estos movimientos en su propio portal, de solo lectura.

**Historia clínica y el rol MEDICO**
- Cada médico tiene su propia cuenta (vinculada a su `Medico` desde el panel admin) y ve solo su agenda del día, su cola de espera de pacientes que llegaron, y puede marcar "atendido" y cargar una evolución clínica de la consulta.
- Historia clínica por paciente, visible solo para los médicos que lo atendieron — un médico no puede ver evoluciones cargadas por otro médico.
- Saldo de caja acumulado, visible en su propio portal.

**Auditoría**
- Undo/redo de operaciones sobre turnos (reservar, cancelar, reasignar) con historial completo — pensado para revertir un error de carga de la secretaria sin tener que arreglarlo a mano en la base.

**Administración**
- Alta de especialidades y médicos, carga de horarios semanales por médico, generación de turnos a partir de esos horarios, bandeja de mensajes de "alquiler de consultorios" y de postulaciones con descarga de CV.
- Agenda para la secretaria: vista de lista por día, y vista semanal tipo calendario (eligiendo un médico) con los turnos coloreados por estado, además de reserva de un turno en nombre de un paciente que llama por teléfono o WhatsApp (sin necesidad de que ese paciente tenga cuenta).
- Búsqueda global de pacientes.
- El resumen del panel (admin y contador) además del snapshot de hoy muestra un gráfico de tendencia de los últimos 14 días (turnos reservados e ingresos para admin; ingresos vs. gastos para contador).

**Seguridad de cuentas**
- Verificación en dos pasos (TOTP, tipo Google Authenticator), disponible para ADMIN, MEDICO y CONTADOR — no para pacientes, para no agregarles fricción a los que reservan por teléfono.

**Comunicaciones**
- Página de contacto para alquiler de consultorios y página "Trabajá con nosotros" con carga de CV en PDF, ambas en el sitio estático.
- Todos los emails al paciente sobre su turno o plan de kinesiología (confirmación, cancelación, recordatorio, plan aprobado/rechazado, turno liberado de la lista de espera) usan el diseño de marca de FKT Lomas (HTML, no texto plano) — ver `fkt-turnos-backend/src/main/resources/email-templates/`. Los emails internos a la clínica (aviso de nuevo mensaje/postulación, resumen mensual de caja, alerta de fallo de job) siguen en texto plano. Todo el envío está desactivado por defecto hasta que se configure un SMTP real (ver README del backend).
- Si falla el canal preferido de un paciente (email o WhatsApp) al notificarle cualquiera de esos eventos, el sistema reintenta automáticamente por el otro canal antes de darse por vencido — solo si hay un dato de contacto real para ese canal alternativo.
- Recordatorios automáticos de turno desde un job programado del backend, con reintentos, historial y reenvío manual, plantilla del mensaje (solo aplica al canal WhatsApp — el email usa el diseño fijo de arriba) y anticipación editables, y envío de un mensaje puntual a un paciente o masivo a todos los que tienen turno en un rango de fechas (ej. avisar un cierre por feriado) — todo desde la pestaña "Recordatorios" del panel de admin (ver README del backend para las variables de entorno de Twilio). WhatsApp desactivado hasta configurar Twilio; el canal por defecto de cada paciente es email.
- Resumen semanal automático (turnos reservados, ingresos, gastos, retiros, caja física) al mail interno de la clínica, todos los lunes a la mañana — mismo diseño de marca que el resto de los avisos.

**App de escritorio**
- El panel de administración (y los portales de médico y contador) también se pueden empaquetar como app de escritorio para Windows y Mac (Electron) — ver `fkt-turnos-frontend/electron/` y el script `npm run desktop:build`. La app de escritorio es exclusivamente para los paneles internos: no tiene ningún link de salida al sitio informativo público.

**API**
- La API está documentada con OpenAPI/Swagger (`springdoc-openapi`) — con el backend corriendo, la UI interactiva está en `http://localhost:8080/swagger-ui.html` y el JSON crudo en `/v3/api-docs`.

## Despliegue

Todavía no está desplegado — se está preparando la infraestructura de a poco, sin crear ninguna cuenta ni elegir proveedor final todavía:

- **Backend**: `fkt-turnos-backend/Dockerfile` (multi-stage, build verificado localmente con `docker build`) — sirve para desplegar en cualquier host que corra un Dockerfile (Railway, Render, Fly.io). Toda la config sensible ya sale de variables de entorno (ver `fkt-turnos-backend/README.md`), no hace falta tocar código para desplegar.
- **Uploads** (fotos de médicos, CVs): ya no dependen de disco local — hay un backend de storage S3-compatible (`STORAGE_TYPE=s3`, AWS S3 o Cloudflare R2) además del local de siempre. Ver `fkt-turnos-backend/README.md`.
- **Frontend** (`fkt-turnos-frontend/`): `vercel.json` y `public/_redirects` ya agregados — cubren el rewrite de SPA (todas las rutas caen en `index.html`) para Vercel, Netlify y Cloudflare Pages por igual.
- **Secretos de producción** (`JWT_SECRET`, `MFA_ENCRYPTION_KEY`, contraseña de admin): ya generados y guardados en `.secrets-local/produccion.env` (fuera de git, ver `.gitignore`) — listos para copiar al panel del host que se elija.
- **Todavía pendiente** (necesita elegir proveedor / crear cuentas — se deja para cuando esté todo listo del lado de negocio):
  - Elegir y crear cuenta en el host del backend + su Postgres administrado.
  - Elegir y crear cuenta en el host del frontend/sitio estático.
  - Crear el bucket S3/R2 real y cargar sus credenciales.
  - Resolver el acceso al DNS de `fktlomas.com` (lo armó un contratista externo).
  - SMTP real (Brevo sugerido) y cuenta de Twilio + aprobación de plantilla de WhatsApp en Meta.
  - Reemplazar las URLs de desarrollo hardcodeadas (`http://localhost:5173`, `http://localhost:5500`, `http://localhost:8080`) por las reales una vez que se conozcan los dominios definitivos — están en los `.html` del sitio estático, en `fkt-turnos-frontend/.env.production` (`VITE_SITE_URL`) y en la app de escritorio (hay que volver a correr `npm run desktop:build` después).
  - Certificado de firma de código para el instalador de Mac/Windows (hoy sale sin firmar).

## Pendiente / próximos pasos (fuera del alcance de esta versión)

**Bloqueado esperando respuestas de Fernando** (no conviene construir esto sin definiciones, para no tener que rehacerlo):
- Confirmar qué médicos trabajan hoy realmente y su horario real por especialidad — la mayoría de las especialidades cargadas (Traumatología, Reumatología, Flebología, Nutrición, Psicología, Psiquiatría, Psicopedagogía, Genética ADN, Estética, Pilates) todavía no tienen ningún médico asignado en el sistema.
- Tratamientos de capacidad simultánea (ej. magnetoterapia: un profesional atiende a varios pacientes a la vez conectados a una máquina) — el modelo actual asume un turno = un profesional ocupado con un paciente; necesita rediseño del lado de disponibilidad.
- Copagos por obra social más allá de IOMA (ya resuelto) y el precio de la sesión particular (sin obra social) — hoy el sistema avisa que tiene costo pero no puede mostrar el monto.
- Planes de sesiones recurrentes para especialidades que no sean kinesiología (ya resuelto ahí).
- Ventana de anticipación para reservar/cancelar, tope de turnos activos por paciente, y si el centro cierra días propios además de los feriados nacionales (ya cargados).

**Configuración pendiente** (el código ya está listo, falta crear cuentas/credenciales reales — ver sección "Despliegue" arriba):
- SMTP real para que los emails salgan de verdad (`MAIL_ENABLED=false` por defecto).
- Cuenta de Twilio + plantilla de WhatsApp Business aprobada por Meta, para que el canal WhatsApp de los recordatorios funcione.
- El deploy en sí: elegir host de backend/Postgres, host de frontend, bucket S3/R2, y resolver el acceso al DNS de `fktlomas.com`.
- Certificado de firma de código para el instalador de escritorio (Mac/Windows).

**Mejoras de producto sin bloqueo**, candidatas para una próxima vuelta:
- Reglas de disponibilidad particulares por médico (turnos rotativos, múltiples consultorios).
- Rollout de las primitivas de UI (`TabNav`, `StatCard`) al resto de los tabs de admin/contador que todavía no las usan.
- El destinatario del resumen semanal es siempre `ADMIN_NOTIFY_EMAIL` (no configurable desde el panel, a diferencia del reporte mensual de caja) — si en algún momento hace falta mandarlo a otra dirección, agregar esa opción a "Caja" o a una config propia.
