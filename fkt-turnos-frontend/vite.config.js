import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'
import politicaSeguridad from './vite/politicaSeguridad.js'

// El service worker de la PWA no tiene sentido dentro del wrapper de
// escritorio (Electron): ya sirve los archivos localmente, no hay nada que
// cachear para "offline". Se omite el plugin por completo en ese build
// (ver package.json: "desktop:build" setea esta variable).
const paraElectron = process.env.ELECTRON_BUILD === 'true'

// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  // "release/" es la salida de "electron-builder" (npm run desktop:build) — vigilarla
  // choca con sus archivos temporales/bloqueados mientras arma el instalador y puede
  // tirar abajo el dev server entero (EBUSY en Windows).
  server: {
    // Sin esto, Vite a veces solo abre el socket en el loopback IPv6 (::1) y no en el IPv4
    // (127.0.0.1) — según cómo resuelva "localhost" cada navegador, el otro te tira
    // ERR_CONNECTION_REFUSED aunque el servidor esté corriendo. host:true fuerza a escuchar
    // en todas las interfaces IPv4 (0.0.0.0), así que "localhost" resuelva como resuelva.
    host: true,
    watch: {
      ignored: ['**/release/**'],
    },
    // Túnel temporal de Cloudflare para compartir un link público de prueba (ver charla con
    // el equipo) — no es necesario para desarrollo local normal.
    allowedHosts: ['.trycloudflare.com'],
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          // Las librerías de moderación de fotos (TensorFlow.js + face-api.js + nsfwjs, ~1,8 MB)
          // solo se usan al subir una foto de perfil y ya se importan con import() dinámico en
          // src/utils/moderacionFoto.js. Agruparlas en un chunk con nombre fijo permite
          // excluirlas del precache del service worker (ver workbox.globIgnores más abajo):
          // sin esto, cada paciente se bajaba ese 1,8 MB en su primera visita aunque nunca
          // subiera una foto.
          groups: [
            {
              name: 'moderacion-ml',
              test: /node_modules[\\/](@tensorflow|face-api\.js|nsfwjs|seedrandom)[\\/]/,
            },
          ],
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    globals: true,
  },
  plugins: [
    react(),
    // CSP generada a partir de VITE_API_URL y compañía (ver vite/politicaSeguridad.js).
    politicaSeguridad(loadEnv(mode, process.cwd(), 'VITE_')),
    !paraElectron && VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['pwa/apple-touch-icon.png'],
      workbox: {
        // El chunk de moderación de fotos queda fuera del precache (ver build.rolldownOptions);
        // cuando alguien lo usa se guarda en caché recién ahí, junto con los pesos de los
        // modelos de /models (que tampoco se precachean: son .json/.bin, fuera del patrón).
        globIgnores: ['**/moderacion-ml-*.js'],
        // Las fuentes (Atkinson/Caveat, servidas desde la app) se precachean para que la PWA se
        // vea igual sin conexión; .woff es el respaldo para navegadores viejos y no hace falta.
        globPatterns: ['**/*.{js,css,html,woff2}'],
        runtimeCaching: [
          {
            urlPattern: ({ url }) =>
              url.pathname.startsWith('/models/') || /\/assets\/moderacion-ml-.*\.js$/.test(url.pathname),
            handler: 'CacheFirst',
            options: {
              cacheName: 'moderacion-fotos',
              expiration: { maxEntries: 20, maxAgeSeconds: 60 * 60 * 24 * 90 },
            },
          },
        ],
      },
      manifest: {
        name: 'FKT Lomas — Turnos',
        short_name: 'FKT Turnos',
        description: 'Sacá y gestioná tus turnos en FKT Lomas, fisiokinesioterapia en Lomas de Zamora.',
        lang: 'es-AR',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        background_color: '#FFFFFF',
        theme_color: '#239A96',
        icons: [
          { src: '/pwa/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/pwa/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/pwa/maskable-icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
    }),
  ],
}))
