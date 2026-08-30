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
