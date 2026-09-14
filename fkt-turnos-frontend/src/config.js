// URL del sitio de marketing estático (FKT Pagina/). En producción hay que
// configurar VITE_SITE_URL con la URL real una vez desplegado.
export const SITE_URL = import.meta.env.VITE_SITE_URL || 'http://localhost:5500';

// electron/main.cjs no pisa el user agent por defecto de Electron, así que
// esto alcanza para distinguir la app de escritorio (solo paneles internos)
// del navegador normal, sin necesidad de un preload/contextBridge.
export const isElectron = typeof navigator !== 'undefined' && /Electron/i.test(navigator.userAgent);

// URL pública de ESTA app (la de turnos, no el sitio de marketing) — la usa el cartel QR
// de la sala de espera (ver SalaEsperaQr.jsx), que un paciente escanea desde su propio
// celular. En el navegador normal (web/PWA) alcanza con window.location.origin porque
// coincide con la URL pública. En la app de escritorio (Electron) NO alcanza: ahí
// window.location.origin es "http://localhost:43117" (el servidor Express local de
// electron/main.cjs), que solo existe en la computadora de la secretaria — un QR con esa
// URL no lo puede abrir nadie desde su teléfono. Por eso en Electron hace falta configurar
// VITE_APP_URL a mano en el build (con la URL real, ej. https://turnos.fktlomas.com) antes
// de generar el cartel; si no está configurada, queda en null a propósito para que
// SalaEsperaQr.jsx avise en vez de imprimir un código roto.
export const APP_URL = import.meta.env.VITE_APP_URL
  || (!isElectron && typeof window !== 'undefined' ? window.location.origin : null);

// A dónde va cada rol después de loguearse — una sola fuente de verdad para
// App.jsx (InicioRedirect) y LoginPage, que antes tenían cada uno su propia
// copia del mapeo y podían desincronizarse si se agregaba un rol nuevo.
export const RUTA_POR_ROL = { ADMIN: '/admin', SECRETARIA: '/secretaria', MEDICO: '/medico', CONTADOR: '/contador' };

// Roles para los que el backend exige 2FA y no permite desactivarla (ver AuthService.ROLES_MFA_OBLIGATORIO
// y AppUserDetails en el backend) — usado para bloquear el panel hasta que la activen y para
// ocultar la opción de desactivarla una vez activa.
export const ROLES_MFA_OBLIGATORIO = ['ADMIN', 'SECRETARIA', 'CONTADOR'];

// ID de cliente OAuth de Google (mismo valor que GOOGLE_CLIENT_ID del backend). Vacío = "Iniciar
// sesión con Google" simplemente no se muestra, en vez de mostrar un botón roto — mismo criterio
// que MercadoPago/Twilio del backend (ver GoogleAuthButton).
export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
