# Arquitectura del sistema

Guía para quien tenga que mantener o ampliar el sistema. Explica cómo está armado, por qué, y
cómo agregar algo nuevo sin romper lo que ya funciona. Para seguridad ver `SEGURIDAD.md`; para
normativa, `CUMPLIMIENTO-LEGAL.md`; para el sistema visual, `../DESIGN.md`.

Última revisión: octubre de 2026.

---

## 1. Las piezas

```
                    ┌────────────────────────┐
  Pacientes ───────▶│  Sitio (FKT Pagina)    │  HTML/CSS/JS estático, sin build
  (celular / PC)    │  fktlomas.com          │  Formularios, equipo, reseñas
                    └──────────┬─────────────┘
                               │ "Pedir turno"
                    ┌──────────▼─────────────┐
  Pacientes ───────▶│  App de turnos (React) │  Web + PWA + iOS/Android (Capacitor)
  Personal  ───────▶│  fkt-turnos-frontend   │  + escritorio (Electron, solo paneles)
                    └──────────┬─────────────┘
                               │ HTTPS + JWT (header Authorization)
                    ┌──────────▼─────────────┐      ┌─────────────────────────────┐
                    │  API (Spring Boot 4)   │─────▶│ PostgreSQL 16 (Flyway)      │
                    │  fkt-turnos-backend    │─────▶│ Archivos: disco local o S3/R2│
                    └──────────┬─────────────┘      └─────────────────────────────┘
                               │
            Email (SMTP) · WhatsApp (Twilio) · Mercado Pago · Google Sign-In
```

- **Un solo backend** para todo: el sitio y la app le hablan a la misma API.
- **Una sola base de datos**. Todo el esquema se versiona con migraciones de Flyway
  (`fkt-turnos-backend/src/main/resources/db/migration/V*.sql`); Hibernate solo valida
  (`ddl-auto: validate`), nunca modifica tablas.
- Las integraciones externas son **opcionales**: sin credenciales, cada una se apaga sola y el
  resto sigue funcionando (sin SMTP los emails se loguean; sin Twilio no hay WhatsApp; sin
  Mercado Pago se paga en el consultorio).
- `recordatorio-turnos/` es una app de escritorio JavaFX anterior, reemplazada por los
  recordatorios del backend. Se conserva solo como referencia.

## 2. Backend (`fkt-turnos-backend`)

### Capas

```
controller/   ← recibe el HTTP, valida el DTO (@Valid), no tiene lógica de negocio
   │             controller/admin/ = endpoints del panel interno (/api/admin/**)
service/      ← toda la lógica de negocio y las transacciones (@Transactional)
   │
repository/   ← acceso a datos (Spring Data JPA); consultas complejas con @Query
domain/       ← entidades JPA (tablas) y enums de estado
dto/          ← records de entrada (…Request) y salida (…Dto) de la API.
                 Las entidades NUNCA salen por la API: siempre se convierten con Dto.de(...)
security/     ← JWT, filtros, límites de intentos, verificación de firmas de webhooks
config/       ← SecurityConfig (quién puede entrar a qué), clientes HTTP, locks de tareas
storage/      ← dónde viven los archivos (local o S3) e ImagenSegura
exception/    ← excepciones de negocio + GlobalExceptionHandler (formato único de errores)
util/, validation/ ← helpers chicos (DatosPersonales, Patrones de validación)
```

### Convenciones

- **Nombres en castellano** (dominio del centro: turno, médico, obra social…). Las clases de
  Spring/Java conservan su sufijo en inglés (Service, Repository, Controller).
- **Errores**: un service tira `RecursoNoEncontradoException` (404), `ConflictoDeDatosException`
  (409), `IllegalArgumentException`/`IllegalStateException` (400) o `AccessDeniedException` (403)
  con un **mensaje pensado para mostrarle a la persona**. `GlobalExceptionHandler` arma siempre
  el mismo JSON: `{timestamp, status, mensaje, detalle?}`.
- **Autorización** en dos niveles: la puerta gruesa por URL en `SecurityConfig` y el control fino
  con `@PreAuthorize` en el controller. Todo lo que toca datos de un paciente verifica además, en
  el service, que el recurso sea de quien lo pide.
- **Auditoría**: toda operación administrativa sensible llama a `AuditoriaService.registrar(...)`
  con el antes/después.
- **Comentarios**: explican el *porqué* (la regla de negocio, la ley, el bug que evitan), no el
  *qué*. Si algo parece raro, casi siempre hay un comentario al lado que lo justifica.
- **Hora**: la aplicación corre en hora argentina (`TurnosApplication` fija la zona); los
  instantes se guardan como `Instant` y las fechas de agenda como `LocalDate`/`LocalTime`.

### Tareas programadas

| Tarea | Cuándo | Clase |
|---|---|---|
| Recordatorios de turnos | cada 15 min (`RECORDATORIOS_INTERVALO_MINUTOS`) | `RecordatorioService.procesarPendientes` |
| Resumen semanal al admin | lunes 8:00 | `ResumenSemanalService` |
| Reporte mensual de caja | último día del mes, 20:00 | `ReporteCajaProgramadoService` |
| Borrado de datos vencidos | todos los días, 3:30 | `RetencionDatosService` |

Todas llevan `@SchedulerLock` (ShedLock, tabla `shedlock`): si hay más de una instancia del
backend, cada tarea corre en una sola. Una tarea nueva tiene que llevarlo también.

## 3. App (`fkt-turnos-frontend`)

```
src/
  main.jsx, App.jsx    ← arranque y rutas (las pantallas pesadas se cargan con lazy())
  app/store.js         ← Redux store
  api/axiosClient.js   ← cliente HTTP: agrega el JWT, ante un 401 cierra la sesión
  config.js            ← URLs y constantes que vienen de las variables VITE_*
  features/<área>/     ← una carpeta por área: páginas, pestañas y su slice de Redux
                          (admin, auth, caja, contador, cuenta, kinesiologia, legal, medico,
                           notificaciones, perfil, secretaria, turnos)
  components/          ← piezas compartidas entre áreas (Layout, avatar, selector de obra social…)
  components/ui/       ← primitivas de interfaz (Modal, Goniometro, SidebarNav, Toast, StatCard…)
  hooks/               ← hooks compartidos (useTema, useAhora)
  utils/               ← funciones puras (formato de fechas y moneda, calendario, imágenes)
  constants/           ← listas fijas (obras sociales, avatares)
  index.css            ← TODO el estilo: tokens (variables CSS) + clases de componentes
vite/politicaSeguridad.js ← genera la CSP en cada build
electron/main.cjs     ← app de escritorio
```

### Convenciones

- **Estado del servidor en Redux Toolkit** (`createAsyncThunk` + slice por área); estado de
  formularios y de la pantalla, en `useState` local.
- **Nada de lógica repetida**: si un `<select>`, un formateo o un fetch se usa en dos pantallas,
  va a `components/`, `utils/` o `hooks/` (ejemplos: `SelectorObraSocial`, `formatFechaHora`,
  `useAhora`).
- **Estilos con clases y variables CSS** de `index.css`, no con colores sueltos en `style={{}}`.
  Los tokens están documentados en `DESIGN.md`.
- **Efectos de React**: solo para sincronizar con algo externo (API, temporizadores, el
  navegador). Lo que se puede calcular a partir del estado se calcula en el render (el linter
  `oxlint` lo controla y tiene que quedar en 0 avisos).
- **Accesibilidad**: textos de 16 px mínimo, controles de 48 px, `aria-*` en todo lo interactivo
  que no sea nativo; el goniómetro es decorativo y siempre va acompañado del dato en texto.

## 4. Sitio (`FKT Pagina`)

HTML estático por página + `assets/css/site.css` y `assets/js/site.js` compartidos. El JS propio
de cada página va en `assets/js/paginas/`, y las URLs de backend/app en `assets/js/config.js`.
Sin build: se publica la carpeta tal cual. Detalle en `FKT Pagina/README.md`.

## 5. Cómo agregar una funcionalidad (receta)

Ejemplo real: el "Botón de arrepentimiento" (octubre 2026) siguió exactamente estos pasos.

1. **Migración**: `V<siguiente>__descripcion.sql` con la tabla nueva. Nunca editar una migración
   ya aplicada: siempre una nueva.
2. **Entidad** en `domain/` (+ enum de estado si hace falta) y **repository**.
3. **DTOs** en `dto/<área>/`: `…Request` con Bean Validation y mensajes en castellano; `…Dto`
   con un `static de(entidad)`.
4. **Service** con la lógica y `@Transactional`; tira las excepciones de negocio de arriba.
5. **Controller**: público en `controller/`, del panel en `controller/admin/`. Si es público,
   agregarlo a `SecurityConfig` (y a `RateLimitingFilter` si recibe formularios).
6. **Tests**: al menos el caso feliz y los permisos (ver `AutorizacionRolesTest`).
7. **Frontend**: thunk en el slice del área, componente en `features/<área>/`, pestaña en el
   panel que corresponda (`GRUPOS_NAV` de `AdminDashboardPage`/`SecretariaDashboardPage`).
8. **Documentar**: README del componente tocado y, si cambia qué datos se guardan, la Política
   de Privacidad y `docs/CUMPLIMIENTO-LEGAL.md`.

Antes de commitear: `./mvnw test` en el backend; `npm run lint`, `npm test` y `npm run build` en
el frontend. La CI (`.github/workflows/ci.yml`) corre lo mismo más los escaneos de seguridad.

## 6. Escalar

El sistema está pensado para un centro (decenas de profesionales, miles de pacientes) y aguanta
crecer sin cambios de arquitectura:

- **Más tráfico** → el backend no guarda sesión en memoria (JWT), así que se pueden correr varias
  instancias detrás de un balanceador. Lo único que hay que mover a un almacenamiento compartido
  en ese caso son los **límites de intentos** (`RateLimiterService`, `LoginRateLimiterService`,
  hoy en memoria: pasarlos a Redis). Las tareas programadas ya están protegidas con ShedLock.
- **Archivos** → `STORAGE_TYPE=s3` (S3 o Cloudflare R2) para no depender del disco del servidor.
- **Base de datos** → las consultas pesadas están indexadas (agenda por médico y fecha, turnos por
  paciente, auditoría) y las bandejas que crecen sin límite (mensajes, auditoría, notificaciones)
  están paginadas. La retención automática evita que crezcan tablas que no hace falta guardar.
- **Más sedes** → hoy el sistema asume un único centro. Para varias sedes habría que agregar una
  entidad `Sede` y relacionarla con médicos, reglas de disponibilidad y turnos (una migración y
  un filtro más en la agenda), sin tocar el resto.
- **Frontend** → las pantallas de cada rol se cargan por separado (`lazy()`), y las librerías
  pesadas (moderación de fotos) solo cuando se usan.
