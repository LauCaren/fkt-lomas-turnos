# FKT Lomas — sitio informativo

Sitio estático (HTML + CSS + JS vanilla, sin build step) para FKT Lomas, centro de
kinesiología y especialidades médicas en Lomas de Zamora. 11 páginas de contenido + una
página 404, todas compartiendo `assets/css/site.css` y `assets/js/site.js`.

> **Este archivo se mantiene actualizado en cada sesión de trabajo** — es la referencia de
> qué tiene el sitio hoy y qué falta para darlo por terminado. Antes de asumir que algo no
> existe, buscarlo acá primero.

## Estructura

```
FKT Pagina/
├── index.html, servicios.html, especialidades.html, equipo.html,
│   el-centro.html, opiniones.html, contacto.html,
│   alquiler-consultorios.html, trabaja-con-nosotros.html,
│   politica-de-privacidad.html, terminos-y-condiciones.html, 404.html
├── assets/
│   ├── css/site.css      ← estilos compartidos por las 12 páginas
│   ├── js/site.js        ← todo el JS compartido (ver más abajo)
│   ├── img/               ← fotos reales del centro, logos de obras sociales, etc.
│   └── video/
├── sitemap.xml, robots.txt
```

Cada página conserva, en su propio `<style>`, solo las reglas que le son exclusivas (grilla
de especialidades, hero del home, etc.) — todo lo reutilizable vive en `site.css`.

`equipo.html`, `el-centro.html`, `alquiler-consultorios.html` y `trabaja-con-nosotros.html`
tienen además un `<script>` propio al final del body que habla con el backend
(`fkt-turnos-backend`, Spring Boot en `http://localhost:8080`) para la ficha de
profesionales y el envío de formularios. **`politica-de-privacidad.html` y
`terminos-y-condiciones.html` no cargan `site.js`** — son páginas legales con header mínimo
a propósito (sin buscador, sin menú hamburguesa), decisión ya tomada, no es un olvido.

## Qué tiene el sitio hoy

### Contenido y estructura
- Home orientado a "¿qué te pasa?" en vez de estructura médica: hero, mapa corporal
  interactivo, "por qué FKT", servicios, especialidades, obras sociales, cita destacada de
  reseñas, FAQ, ubicación, alquiler/trabajá con nosotros.
- Servicios y especialidades con fichas clickeables (beneficios + reseñas reales o
  calificación agregada — nunca casos inventados).
- Equipo con ficha de profesional (foto, especialidad, años de experiencia, título
  habilitante si está cargado, días y horarios de atención) conectada al backend real.
- El Centro, Opiniones (reseñas reales + link a Google), Contacto (obras sociales,
  FAQ completa, mapa, datos), Alquiler de consultorios y Trabajá con nosotros (formularios
  reales contra el backend).

### Búsqueda del sitio
Buscador (Ctrl/Cmd+K o ícono en el header) con índice de ~65 entradas: páginas, servicios,
especialidades, los 26 profesionales del equipo, FAQ, obras sociales — con deep-link directo
a la sección exacta. Definido en `site.js` (`FKT_SEARCH_INDEX`).

### Mapa corporal interactivo
En el hero del home: silueta clickeable (hombro, codo, columna, cadera, rodilla, pie) que
muestra los tratamientos/especialidades reales indicados para esa zona y abre su ficha.
Una sola instancia en toda la página (`FKT_BODY_MAP` en `site.js`).

### Motion / interacción
Todo gateado por `prefers-reduced-motion` y por `pointer:fine` (nada de esto corre en
celular, que ya tiene su propio scroll con inercia nativo):
- Cursor a medida + cursor magnético en botones primarios.
- Tilt 3D en tarjetas (servicios, especialidades, promos, reseñas).
- Títulos que se revelan con clip-path en vez de fade.
- Íconos de servicios/especialidades que se "dibujan" (stroke-dashoffset) al entrar en pantalla.
- Spotlight que sigue al cursor en las bandas oscuras de CTA.
- Scroll con inercia (suaviza la rueda del mouse sin romper el `window.scrollY` real).
- Intro de carga con el isotipo — una vez por sesión de navegador (`sessionStorage`).
- Contadores animados (4.4★, +60 reseñas) con leve desenfoque mientras cuentan.
- Ripple en botones primarios al soltar el click/tap.
- Marquee infinito de logos de obras sociales (con fondo blanco fijo para que se lean bien
  también en modo oscuro).

### Modo oscuro
Toggle real (sol/luna) en el header, persistido en `localStorage`, respeta
`prefers-color-scheme` por defecto. Sin flash al cargar (script bloqueante mínimo en el
`<head>` de las 12 páginas). Paleta completa redefinida vía variables CSS en `site.css`.

### Accesibilidad
- Skip-link ("Saltar al contenido") en las 12 páginas.
- Focus trap real en los 5 modales del sitio (búsqueda, ficha de tratamiento, ficha de
  profesional, y los 2 lightbox) vía `window.fktCreateFocusTrap` (`site.js`).
- `:focus-visible` consistente, `aria-label`/`role` en elementos interactivos no nativos.

### SEO
- `MedicalClinic` (JSON-LD) en el home, `FAQPage` en contacto.html, `Service` en
  servicios.html y especialidades.html.
- Open Graph + Twitter Card completos en las 12 páginas, `theme-color`, favicon.
- `sitemap.xml` / `robots.txt` ya existían antes de esta sesión — **no incluyen 404.html a
  propósito** (una página de error no debe indexarse).

### Rendimiento
`loading="lazy" decoding="async"` en las fotos debajo del pliegue (galerías, filas 2+ de
grillas); las imágenes hero/banner quedan eager a propósito (son el LCP de cada página).

### Otros detalles
- Página 404 a medida con el buscador como protagonista.
- Glosario en línea (RPD, bio-ortopedia, tendinopatías, fascitis plantar) con tooltip,
  dentro de las fichas de tratamiento.
- Scrollbar a medida (fina, en teal).
- Numeración editorial automática en los `eyebrow` de cada `.section-head` (01, 02...),
  vía CSS counters — reinicia por página.
- Hoja de impresión (`@media print`) que oculta chrome/flotantes y fuerza colores claros.
- Avatar chico junto al nombre de cada profesional en equipo.html **solo cuando el backend
  tiene una foto real cargada** (nada de placeholders repetidos).
- Sello "Título verificado" en la ficha de profesional cuando el backend confirma
  `tieneTitulo`.
- Días y horarios de atención de cada profesional en su ficha (equipo.html) — se piden a
  `/api/medicos/{id}/horarios` recién al abrir la ficha (no de entrada para las 26 personas
  del equipo), la misma `ReglaDisponibilidad` que ya usa el turnero para calcular
  disponibilidad. Si un profesional todavía no tiene horarios cargados en el sistema, la
  ficha lo dice explícitamente en vez de mostrar la sección vacía o inventar un horario.
- Aviso visible (no silencioso) si falla la conexión al backend en equipo.html.
- Orb de fluido animado (WebGL, shader GLSL de ruido/fbm propio, sin librerías de por medio)
  en la página 404 — portado desde [rare-ui](https://github.com/swamimalode07/rare-ui) (MIT)
  a JS vanilla en vez de instalar su stack completo (Tailwind + shadcn). Ver `fktInitFluidOrb`
  en `assets/js/site.js`.
- Nota de "listado actualizado en [mes/año]" en la grilla de obras sociales de contacto.html
  — hoy dice "septiembre de 2026"; **hay que actualizarla a mano** la próxima vez que se
  toque esa lista (no es un dato dinámico, es honesto: se actualiza cuando alguien la edita).

## Qué falta — necesita información real del cliente

Nada de esto se inventó porque hacerlo sería publicar información falsa sobre una clínica
real. Implementar apenas haya el dato:

- [ ] **Carta del equipo**: un párrafo en primera persona (de Fernando u otro socio) sobre
      por qué armaron el centro, para el home — humaniza mucho más que cualquier animación.
- [ ] **Tiempo de respuesta de WhatsApp** real ("normalmente respondemos en X hs").
- [ ] **Años de trayectoria / cantidad de pacientes atendidos**, si quieren mostrarlo.
- [ ] **Feed de Instagram embebido** (@fktlomas) — necesita posts reales concretos.
- [ ] **Cómo llegar en detalle**: líneas de colectivo, estacionamiento cercano verificados.
- [ ] **Sello de matrícula / colegio profesional**, si tienen uno para mostrar.
- [ ] **"Próximo turno disponible" en vivo**: el botón "Pedir turno" hoy apunta a
      `http://localhost:5173` — el turnero (`fkt-turnos-frontend`) todavía no está en
      producción. Conectar un endpoint público en vivo es trabajo de infraestructura aparte
      (backend público + CORS), no de esta página.
- [ ] Contenido tipo blog ("consejos para tu recuperación") — requiere que alguien lo
      escriba, no es un build de una vez.

## Decisiones deliberadas (no son bugs ni pendientes)

- Las bandas de CTA oscuras repiten texto en varias páginas (index/servicios/opiniones) a
  propósito — es un patrón de cierre de página, no contenido confuso.
- opiniones.html es la única fuente de las 3 reseñas reales que existen — el home solo
  muestra una cita destacada y linkea a opiniones.html, no las repite todas.
- Las páginas legales (política de privacidad, términos) no cargan `site.js` ni tienen
  buscador/dark-mode-toggle propio, aunque sí heredan el tema si el usuario lo cambió en
  otra página (vía `localStorage` + `data-theme`).

## Desarrollo local

Servidor estático vía `.claude/launch.json` → configuración `fkt-pagina` (puerto 5500,
`npx serve`). El botón "Pedir turno" y el envío de formularios asumen el backend
(`fkt-turnos-backend`, puerto 8080) corriendo en paralelo — sin eso, esos flujos fallan de
forma visible y controlada (nunca en silencio), como se documenta arriba.
