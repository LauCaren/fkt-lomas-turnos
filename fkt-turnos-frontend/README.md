# fkt-turnos-frontend

App React + Redux Toolkit para el registro de pacientes, la reserva de turnos y el panel de administración de FKT Lomas.

## Correr en local

```bash
npm install
npm run dev
```

Por defecto apunta al backend en `http://localhost:8080` (ver `.env.development`, variable `VITE_API_URL`). Para producción, crear un `.env.production` con `VITE_API_URL` apuntando al backend real.

## Rutas

- `/login`, `/registro` — públicas
- `/turnos` — asistente de reserva (especialidad → médico → fecha/horario → confirmación), requiere sesión de paciente
- `/turnos/mis-turnos` — turnos del paciente, con opción de cancelar
- `/admin` — panel de Fernando (médicos y horarios, mensajes de "alquiler de consultorios", postulaciones con descarga de CV, cambio de contraseña), requiere sesión de admin

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
