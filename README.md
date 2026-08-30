# FKT Lomas — Sistema de turnos, contacto y postulaciones

Este repositorio contiene tres proyectos independientes:

```
FKT Pagina/              Sitio estático de marketing (ya existía). Se agregaron:
                          - alquiler-consultorios.html
                          - trabaja-con-nosotros.html
                          - link "Pedir turno" apuntando a la app de turnos
                          - columna "Otros" en el footer con los dos links nuevos

fkt-turnos-backend/      API REST en Spring Boot (Java 17): auth JWT, turnos,
                          panel admin, contacto y postulaciones.

fkt-turnos-frontend/     App React + Redux Toolkit: registro/login de pacientes,
                          reserva de turnos y panel de administración de Fernando.

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

## Estado actual (v1)

Implementado y probado de punta a punta:

- Registro de pacientes (DNI, obra social, nombre, email, teléfono, género) y login con JWT.
- Reserva de turnos por especialidad → médico → fecha → horario, con bloqueo real contra doble reserva (dos personas no pueden tomar el mismo horario).
- Cancelación de turnos propios (libera el horario para otros pacientes).
- Panel de administración (Fernando): alta de especialidades y médicos, carga de horarios semanales por médico, generación de turnos a partir de esos horarios, bandeja de mensajes de "alquiler de consultorios" y de postulaciones con descarga de CV.
- Página de contacto para alquiler de consultorios y página "Trabajá con nosotros" con carga de CV en PDF, ambas en el sitio estático.
- Envío de emails (confirmación de turno, aviso de nuevo mensaje/postulación) — desactivado por defecto hasta que se configure un SMTP real (ver README del backend).

## Pendiente / próximos pasos (fuera del alcance de esta versión)

- Reglas de disponibilidad particulares por médico (turnos rotativos, feriados, múltiples consultorios) — a definir con las especificaciones que faltan.
- Deploy a un hosting real (elegir proveedor, dominio, base de datos y SMTP en producción). Hay URLs de desarrollo hardcodeadas que hay que reemplazar por las reales una vez desplegado: la URL de la app de turnos (`http://localhost:5173`) en los `.html` del sitio estático, y la URL del sitio estático (`VITE_SITE_URL`, hoy `http://localhost:5500`) en el frontend de turnos.
- Antes de ir a producción: cambiar `JWT_SECRET` y la contraseña del admin bootstrap por valores propios y seguros.
