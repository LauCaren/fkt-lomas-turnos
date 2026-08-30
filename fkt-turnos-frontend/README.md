# fkt-turnos-frontend

App React + Redux Toolkit para el registro de pacientes, la reserva de turnos y el panel de administración de FKT Lomas.

## Correr en local

```bash
npm install
npm run dev
```

Por defecto apunta al backend en `http://localhost:8080` (ver `.env.development` y `.env.production`, variable `VITE_API_URL`) — actualizar `.env.production` con la URL real una vez que el backend esté desplegado.

## Rutas

- `/login`, `/registro` — públicas
- `/turnos` — asistente de reserva (especialidad → médico → fecha/horario → confirmación), requiere sesión de paciente
- `/turnos/mis-turnos` — turnos del paciente, con opción de cancelar
- `/admin` — panel de Fernando/secretaria (agenda con reserva telefónica, médicos y horarios, recordatorios de turno, mensajes de "alquiler de consultorios", postulaciones con descarga de CV, cambio de contraseña), requiere sesión de admin

El login es el mismo endpoint para pacientes y admin; la app redirige según el `role` que viene en el JWT.

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

`npm run desktop:build` corrido en una Mac genera el `.dmg`; para el instalador de Windows hay que correrlo en una PC con Windows (electron-builder, igual que jpackage, no puede cross-compilar el instalador nativo del otro SO). El backend con el que habla es el de `VITE_API_URL` (ver arriba) — si apunta a `localhost`, hay que agregar `http://localhost:43117` a `CORS_ALLOWED_ORIGINS` del backend para probarlo en desarrollo. Falta un ícono propio (`build.mac.icon`/`build.win.icon` en `package.json`) y firmar el instalador — sin firma, macOS/Windows van a mostrar la advertencia de "desarrollador no identificado" al abrirlo.
