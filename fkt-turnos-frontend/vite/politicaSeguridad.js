// Content-Security-Policy de la app, generada en cada build a partir de las mismas variables de
// entorno que usa el código (VITE_API_URL, VITE_SENTRY_DSN, VITE_GOOGLE_CLIENT_ID) — así no hay
// una lista de dominios escrita a mano que se desincronice cuando cambia la URL del backend.
//
// Se inyecta como <meta http-equiv="Content-Security-Policy"> en el index.html del build, que es
// lo único que funciona igual en los cuatro lugares donde corre la app: el hosting web (Vercel /
// Netlify / Cloudflare Pages), la PWA instalada, los WebView de Capacitor (iOS/Android) y la app
// de escritorio (Electron). Las dos directivas que un <meta> no admite (frame-ancestors, contra
// clickjacking, y las cabeceras HSTS/nosniff/etc.) van como cabeceras HTTP: en public/_headers
// y vercel.json para la web, y en electron/main.cjs para el escritorio.
//
// Por qué importa: la sesión (JWT) vive en localStorage. Si alguna vez se colara un XSS (un
// dato mostrado sin escapar, una dependencia comprometida), esta política es la que impide que
// el script inyectado corra o mande el token a otro dominio.
//
// Solo aplica al build (`apply: 'build'`): el servidor de desarrollo de Vite necesita scripts
// inline para el hot reload.

import { createHash } from 'node:crypto';

function origenDe(url) {
  try {
    return url ? new URL(url).origin : null;
  } catch {
    return null;
  }
}

/** Hash CSP de cada <script> inline sin src del HTML (hoy, solo el del tema claro/oscuro). */
function hashesDeScriptsInline(html) {
  const hashes = [];
  for (const [, cuerpo] of html.matchAll(/<script>([\s\S]*?)<\/script>/g)) {
    hashes.push(`'sha256-${createHash('sha256').update(cuerpo).digest('base64')}'`);
  }
  return hashes;
}

export function construirCsp({ apiUrl, sentryDsn, googleClientId, hashesScripts = [] }) {
  const api = origenDe(apiUrl);
  const sentry = origenDe(sentryDsn);
  const google = Boolean(googleClientId);

  const directivas = {
    'default-src': ["'self'"],
    // 'wasm-unsafe-eval' no hace falta: TensorFlow.js usa el backend WebGL, no WebAssembly.
    'script-src': ["'self'", ...hashesScripts, ...(google ? ['https://accounts.google.com/gsi/client'] : [])],
    // 'unsafe-inline' en estilos: React aplica style={{...}} como atributos inline y el botón de
    // Google inyecta los suyos. Inyectar CSS es mucho menos grave que inyectar scripts.
    'style-src': ["'self'", "'unsafe-inline'", ...(google ? ['https://accounts.google.com/gsi/style'] : [])],
    // blob: = fotos de perfil y comprobantes que se bajan con el token y se muestran como blob.
    'img-src': ["'self'", 'data:', 'blob:', ...(api ? [api] : [])],
    'font-src': ["'self'"],
    'connect-src': ["'self'", ...(api ? [api] : []), ...(sentry ? [sentry] : []), ...(google ? ['https://accounts.google.com/gsi/'] : [])],
    'frame-src': google ? ['https://accounts.google.com/gsi/'] : ["'none'"],
    'worker-src': ["'self'", 'blob:'],
    'manifest-src': ["'self'"],
    'media-src': ["'self'", 'blob:'],
    'object-src': ["'none'"],
    'base-uri': ["'none'"],
    'form-action': ["'self'"],
  };
  return Object.entries(directivas).map(([nombre, valores]) => `${nombre} ${valores.join(' ')}`).join('; ');
}

export default function politicaSeguridad(env) {
  return {
    name: 'fkt-politica-seguridad',
    apply: 'build',
    transformIndexHtml: {
      order: 'post',
      handler(html) {
        const csp = construirCsp({
          apiUrl: env.VITE_API_URL,
          sentryDsn: env.VITE_SENTRY_DSN,
          googleClientId: env.VITE_GOOGLE_CLIENT_ID,
          hashesScripts: hashesDeScriptsInline(html),
        });
        return {
          html,
          tags: [{ tag: 'meta', attrs: { 'http-equiv': 'Content-Security-Policy', content: csp }, injectTo: 'head-prepend' }],
        };
      },
    },
  };
}
