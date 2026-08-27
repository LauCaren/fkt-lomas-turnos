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
- `VITE_SITE_URL` — URL del sitio de marketing estático (a dónde apunta el logo del header). Por defecto `https://fktlomas.com`.
