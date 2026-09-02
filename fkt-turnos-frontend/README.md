# fkt-turnos-frontend

App React + Redux Toolkit para el registro de pacientes, la reserva de turnos y el panel de administración de FKT Lomas.

## Correr en local

```bash
npm install
npm run dev
```

Por defecto apunta al backend en `http://localhost:8080` (ver `.env.development` y `.env.production`, variable `VITE_API_URL`) — actualizar `.env.production` con la URL real una vez que el backend esté desplegado.

## Rutas

Públicas:
- `/login`, `/registro` — login y alta de pacientes
- `/olvide-password`, `/restablecer-password` — recuperación de contraseña por email
- `/confirmar-turno` — confirmación/rechazo de asistencia sin login, desde el link del recordatorio (token por turno)

Paciente (`RutaPaciente`, requiere sesión con `role=PACIENTE`):
- `/turnos` — asistente de reserva (especialidad → médico → fecha/horario → confirmación), con sugerencia de próximas fechas con lugar si la elegida no tiene horarios
- `/turnos/kinesiologia` — pedido de plan de sesiones de kinesiología (orden médica, kinesiólogo, horarios fijos)
- `/turnos/mis-turnos` — turnos del paciente, con opción de cancelar/reprogramar
- `/turnos/mi-cuenta` — datos propios, cambio de contraseña, canal de notificación preferido

Paneles internos (uno por rol, cada uno con su propio guard — `RutaAdmin`/`RutaMedico`/`RutaContador`):
- `/admin` — panel de Fernando/secretaria: agenda (lista y semanal) con reserva telefónica, médicos y horarios, kinesiología, caja, reportes, recordatorios (config, mensaje puntual, mensaje masivo por rango de fechas), mensajes de "alquiler de consultorios", postulaciones, cuenta/2FA
- `/medico` — agenda del día del médico, cola de espera, su propio saldo de caja, historia clínica de sus pacientes
- `/contador` — solo lectura de caja (ingresos, retiros, gastos, tendencia), 2FA obligatorio

El login es el mismo endpoint para los cuatro roles; la app redirige según el `role` que viene en el JWT.

## Variables de entorno

- `VITE_API_URL` — URL base del backend.
- `VITE_SITE_URL` — URL del sitio de marketing estático (a dónde apunta el logo del header). En desarrollo apunta a `http://localhost:5500` (ver más abajo); en producción hay que setearla a la URL real del sitio nuevo una vez desplegado — **no** a `fktlomas.com` (esa es la web vieja).

## Ver también el logo apuntando al sitio nuevo

El logo del header linkea al sitio estático (`FKT Pagina/`), no al backend ni a esta app. Para que ese link funcione en desarrollo, serví esa carpeta en el puerto 5500 en una terminal aparte:

```bash
cd "../FKT Pagina"
python3 -m http.server 5500
```

(o `npx serve -l 5500`, lo que tengas a mano). Si no lo levantás, la app sigue funcionando igual — el logo simplemente no va a resolver hasta que ese servidor esté corriendo.

## App de escritorio (Electron)

El mismo panel de admin se puede empaquetar como programa instalable para Windows y Mac, así la secretaria tiene un solo lugar para agenda + recordatorios en vez de una pestaña del navegador. Vive en `electron/` (`main.cjs`): arma el build de Vite y lo sirve con un servidor Express local propio en `http://localhost:43117` (no con `file://`, que rompe el service worker de la PWA — por eso además el build de escritorio se hace sin el plugin de PWA, no tiene sentido cachear "offline" en una app nativa que ya tiene los archivos localmente).

```bash
npm run electron        # build + abre la app sin empaquetar, para probar
npm run desktop:build   # build + genera el instalador (.dmg en Mac, con electron-builder) en release/
```

`npm run desktop:build` corrido en una Mac genera el `.dmg`; para el instalador de Windows hay que correrlo en una PC con Windows (electron-builder, igual que jpackage, no puede cross-compilar el instalador nativo del otro SO). El backend con el que habla es el de `VITE_API_URL` (ver arriba) — si apunta a `localhost`, hay que agregar `http://localhost:43117` a `CORS_ALLOWED_ORIGINS` del backend para probarlo en desarrollo. Falta firmar el instalador — sin firma, macOS/Windows van a mostrar la advertencia de "desarrollador no identificado" al abrirlo (ver más abajo).

### Actualizaciones automáticas

La app usa `electron-updater`: al arrancar (solo en un build empaquetado, no con `npm run electron`), busca una versión nueva en la URL de `build.publish` (`package.json`) y, si encuentra una, la descarga sola y le pregunta a la secretaria si quiere instalarla ahora o al cerrar el programa. Así no hace falta reinstalar el `.exe`/`.dmg` a mano en cada máquina.

**Hoy `build.publish.url` es un placeholder** (`https://updates.fktlomas.com/`) que no existe todavía — la app intenta buscar ahí, no encuentra nada, loguea el error y sigue funcionando normal (no rompe nada, solo no hay auto-update real todavía). Para activarlo de verdad hace falta:

1. Un lugar público donde alojar los instaladores + el archivo `latest.yml` (Windows) / `latest-mac.yml` (Mac) que genera `electron-builder` en `release/` junto al instalador — puede ser el mismo bucket S3/R2 que ya se va a usar para uploads (ver README del backend), un repo **público** separado solo para releases, o cualquier hosting estático.
2. Actualizar `build.publish.url` en `package.json` con esa dirección real.
3. Subir ahí el contenido de `release/` cada vez que se genere una versión nueva con `npm run desktop:build` (subir el `.exe`/`.dmg`, su `.blockmap`, y el `.yml` correspondiente — `electron-builder` los genera listos para publicar tal cual).
4. Subir también la versión (`npm version patch/minor`, o editar `version` en `package.json` a mano) antes de cada build — `electron-updater` compara versiones semánticas para saber si hay algo más nuevo.

**Importante:** si en algún momento se prefiere usar GitHub Releases como feed (`provider: "github"`) en vez de un hosting propio, el repo del código **tiene que ser público** o el token para leerlo quedaría embebido en la app (cualquiera podría extraerlo y usarlo para leer el código fuente completo, no solo los releases) — no lo configuré así por eso. Si se quiere ese camino, lo más seguro es un repo nuevo y separado, público, solo con los instaladores compilados (nunca con el código).
