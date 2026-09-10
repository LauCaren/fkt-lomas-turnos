import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// El service worker de la PWA no tiene sentido dentro del wrapper de
// escritorio (Electron): ya sirve los archivos localmente, no hay nada que
// cachear para "offline". Se omite el plugin por completo en ese build
// (ver package.json: "desktop:build" setea esta variable).
const paraElectron = process.env.ELECTRON_BUILD === 'true'

// https://vite.dev/config/
export default defineConfig({
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
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    globals: true,
  },
  plugins: [
    react(),
    !paraElectron && VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['pwa/apple-touch-icon.png'],
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
})
