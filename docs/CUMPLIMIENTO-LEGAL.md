# Cumplimiento legal — qué está hecho y qué falta antes de publicar

Este documento resume cómo el sistema (sitio, app de turnos y backend) cubre la normativa argentina
que le aplica a un centro de salud que da turnos y cobra online, y qué tiene que completar el centro
antes de publicarlo. **No reemplaza la revisión de un abogado**: está pensado para que esa revisión
sea rápida, porque cada punto dice dónde está implementado.

Última revisión: octubre de 2026.

---

## 1. Lo que hay que completar antes de publicar (bloqueante)

Todos estos datos aparecen como `[COMPLETAR]` en el sitio. Buscarlos con:

```bash
grep -rn "COMPLETAR" "FKT Pagina" --include=*.html
```

| Dato | Dónde aparece | Por qué hace falta |
|---|---|---|
| Razón social, CUIT y domicilio legal del titular | Footer de todas las páginas, Política de Privacidad (punto 1), Términos (punto 1) | Ley 24.240 (identificación del proveedor) y Ley 25.326 art. 6 (identidad del responsable de la base) |
| Director/a médico/a y matrícula provincial | Footer, Términos | Publicidad sanitaria: los anuncios de servicios de salud deben identificar a la dirección médica (Ley 17.132 y normas provinciales) |
| N.º de habilitación sanitaria del establecimiento | Footer, Términos | Habilitación del Ministerio de Salud de la Provincia de Buenos Aires |
| N.º de inscripción en el Registro Nacional de Bases de Datos | Política de Privacidad (punto 1) | Ley 25.326 art. 21: toda base de datos con datos personales (y más con datos de salud) se inscribe en la AAIP |
| Email de privacidad | Política de Privacidad (puntos 1, 9 y 16) | Canal para ejercer derechos (acceso, rectificación, supresión) |
| Proveedores de hosting, almacenamiento y email (y su país) | Política de Privacidad (punto 6) | Ley 25.326 art. 6: informar destinatarios; art. 12: transferencias internacionales |

## 2. Trámites y documentos que tiene que hacer el centro (no son código)

1. **Inscribir la base de datos en la AAIP** (Registro Nacional de Bases de Datos). Es gratuito y
   online. Declarar que incluye datos de salud.
2. **Contratos con los proveedores que tratan datos** ("encargados del tratamiento", Ley 25.326
   art. 25): hosting, almacenamiento de archivos, email, Twilio (WhatsApp), Sentry si se activa.
   Para los que están fuera de Argentina (EE. UU.), usar las cláusulas contractuales modelo de la
   AAIP o verificar que los términos del proveedor las incluyan.
3. **Medidas de seguridad documentadas**: la AAIP publicó medidas de seguridad recomendadas para el
   tratamiento de datos (Resolución AAIP 47/2018). Este repo ya cubre la parte técnica (ver
   `docs/SEGURIDAD.md`); falta la parte organizativa: quién tiene acceso a qué, qué hacer si se
   pierde una PC, política de contraseñas del personal.
4. **Confidencialidad del personal**: que secretaría, profesionales y contaduría firmen un
   compromiso de confidencialidad (Ley 25.326 art. 10, secreto profesional).
5. **WhatsApp Business**: si se activa el canal de WhatsApp, Meta exige que el paciente haya
   aceptado recibir mensajes ("opt-in"). El sistema lo registra con el canal preferido que elige
   cada paciente en Mi cuenta.
6. **Revisión del texto legal por un abogado**, en particular: Política de Privacidad, Términos,
   el alcance del derecho de arrepentimiento para servicios de salud y los plazos de conservación.

## 3. Qué normativa cubre el sistema y dónde

### Ley 25.326 — Protección de Datos Personales

| Requisito | Implementación |
|---|---|
| Informar antes de recolectar (art. 6) | Política de Privacidad reescrita (`FKT Pagina/politica-de-privacidad.html`), link en el registro, en todos los formularios y en el pie de la app (`PieLegal.jsx`) |
| Consentimiento expreso, incluidos datos de salud (arts. 5 y 7) | Casilla obligatoria del registro (`ConsentimientoDatos.jsx`), mismo texto en el registro con Google. Se guarda `aceptaPoliticaPrivacidad` y `origenConsentimiento` |
| Datos sensibles: confidencialidad y secreto (arts. 7, 8 y 10) | La historia clínica la ve solo el profesional que la cargó (`MedicoPortalService`, `FichaPacienteDto` sin evoluciones) |
| Seguridad (art. 9) | Ver `docs/SEGURIDAD.md` |
| Derecho de acceso (art. 14) | "Exportar mis datos" en Mi cuenta (`/api/mi-cuenta/exportar-datos`) |
| Rectificación (art. 16) | Mi cuenta permite editar los datos; el DNI se corrige por el centro |
| Supresión (art. 16) | "Solicitar baja de mi cuenta" → cola de pedidos de baja → anonimización (`SolicitudBajaService`) |
| Leyenda obligatoria de la AAIP | Texto oficial en la Política de Privacidad, punto 9 |
| Destrucción cuando los datos dejan de ser necesarios (art. 4 inc. 7) | Borrado automático diario (`RetencionDatosService`, plazos en `app.retencion.*`) |
| Minimización en registros técnicos | Emails y teléfonos enmascarados en los logs (`DatosPersonales`) |
| Transferencias internacionales (art. 12) | Informadas en la Política de Privacidad, punto 7 |

### Ley 26.529 — Derechos del Paciente e Historia Clínica

| Requisito | Implementación |
|---|---|
| Conservación por 10 años (art. 18) | Los turnos, evoluciones, órdenes e ingresos no se borran nunca automáticamente; una baja de cuenta los anonimiza pero los conserva |
| Integridad / inviolabilidad (art. 18) | Las evoluciones solo se crean; no hay endpoint para editarlas ni borrarlas |
| Confidencialidad (art. 2) | Acceso por rol; el personal administrativo no ve contenido clínico |
| Trazabilidad del acceso | Cada vez que un médico abre la historia clínica de un paciente queda registrado en la auditoría (`VER_HISTORIA_CLINICA`) |
| Copia al paciente en 48 h (art. 14) | Comprometido en la Política de Privacidad; "Exportar mis datos" incluye las evoluciones |

### Ley 24.240 — Defensa del Consumidor

| Requisito | Implementación |
|---|---|
| Botón de arrepentimiento (art. 34 y Res. SCI 424/2020): link destacado desde la página de inicio, sin registro, código de trámite en 24 h | Botón con borde en el footer de todas las páginas del sitio y en el pie de la app. Página `arrepentimiento.html` y ruta `/arrepentimiento` de la app (pública aunque haya sesión). El backend genera el código al instante, lo muestra y lo manda por email (`ArrepentimientoService`). El centro gestiona los pedidos en la bandeja "Arrepentimiento" del panel |
| Link a la Ventanilla Única Federal de reclamos (Res. SCI 274/2021) | "Defensa de las y los Consumidores. Para reclamos ingrese aquí" en el footer del sitio, en el pie de la app y en los Términos |
| Identificación del proveedor | Footer y Términos (ver sección 1: completar) |
| Jurisdicción del consumidor | Términos, punto 12: el consumidor elige el tribunal de su domicilio |

### Buenas prácticas internacionales (RGPD europeo como referencia)

No es obligatorio para un centro en Argentina, pero se siguió porque suma confianza y es lo que
espera hoy un paciente:

- **Privacidad por diseño**: el mapa de Google no se carga hasta que la persona toca "Ver mapa"
  (`fktInitMapaDiferido` en `site.js`); las fuentes tipográficas se sirven desde el propio sitio y
  no desde Google Fonts. Resultado: abrir el sitio no le manda datos a ningún tercero.
- **Sin cookies de seguimiento ni analítica de terceros**: por eso no hace falta un cartel de
  cookies. Si en el futuro se agrega Google Analytics, Meta Pixel o similar, **hay que agregar un
  cartel de consentimiento** y actualizar la Política de Privacidad (punto 12).
- **Metadatos de fotos**: las fotos se re-codifican en el servidor y pierden el EXIF (ubicación
  GPS incluida) — `ImagenSegura`.
- **Moderación de fotos en el dispositivo**: los modelos de detección corren en el navegador; la
  imagen no se manda a ningún servicio externo para analizarla.

### Accesibilidad

La app y el sitio siguen las pautas WCAG 2.1 nivel AA (contraste, tamaños mínimos de 16 px y
controles de 48 px, foco visible, textos alternativos, sin depender solo del color para los
estados). La Ley 26.653 obliga formalmente a organismos públicos y a quienes les prestan
servicios, pero es la referencia correcta para un centro con muchos pacientes mayores.

## 4. Si algo cambia

- **Nuevo proveedor que toca datos personales** → agregarlo a la Política de Privacidad (punto 6),
  firmar el contrato de encargado y, si está fuera de Argentina, revisar el punto 7.
- **Cambio de un plazo de conservación** → cambiar `app.retencion.*` en `application.yml` **y** la
  Política de Privacidad (punto 8). Los dos tienen que decir lo mismo.
- **Nuevo dato que se le pide al paciente** → agregarlo a la Política (punto 2) y evaluar si es
  obligatorio (punto 5).
- **Analítica, píxeles de publicidad o newsletters** → requieren consentimiento aparte; no
  activarlos sin agregar el cartel de cookies y la casilla de consentimiento correspondiente.
- **Cambiar el texto de la Política o los Términos** → actualizar la fecha de "Última
  actualización" y, si el cambio es importante, avisar a los pacientes por email.
