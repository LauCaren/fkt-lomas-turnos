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
4. Sitio estático: abrí los `.html` de `FKT Pagina/` directamente en el navegador, o serví la carpeta con cualquier servidor estático (por ejemplo `python3 -m http.server 5500` desde esa carpeta) si querés probar los formularios de contacto/CV, que llaman a la API del backend.

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
- Deploy a un hosting real (elegir proveedor, dominio, base de datos y SMTP en producción). Las URLs de la app de turnos (`http://localhost:5173`) están hardcodeadas en los `.html` del sitio estático — hay que reemplazarlas por la URL real una vez desplegada.
- Antes de ir a producción: cambiar `JWT_SECRET` y la contraseña del admin bootstrap por valores propios y seguros.
