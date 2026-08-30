const { app, BrowserWindow } = require('electron');
const path = require('node:path');
const express = require('express');

// Puerto fijo local: no usamos file:// porque vite-plugin-pwa registra un
// service worker que se comporta mal bajo ese protocolo (no hay un origen
// real). Serví el build de Vite (dist/) con un servidor estático propio y
// cargamos esa URL, igual que en un navegador normal.
const PUERTO = 43117;

function carpetaDist() {
  // Empaquetada (electron-builder): dist/ queda dentro de resources/ (ver
  // "files" en package.json). En desarrollo (electron .): relativa a este
  // archivo, un nivel arriba.
  return app.isPackaged
    ? path.join(process.resourcesPath, 'dist')
    : path.join(__dirname, '..', 'dist');
}

function iniciarServidorEstatico() {
  return new Promise((resolve) => {
    const servidor = express();
    servidor.use(express.static(carpetaDist(), {
      // index.html referencia los archivos con hash (assets/index-XXXX.js) por
      // nombre — si el propio index.html quedara cacheado, una actualización
      // de la app podría seguir mostrando la versión vieja indefinidamente
      // (nos pasó: reconstruimos la app pero la ventana ya abierta seguía con
      // el bundle anterior). Los archivos con hash sí pueden cachearse fuerte,
      // porque cambian de nombre solos cuando cambia su contenido.
      setHeaders: (res, rutaArchivo) => {
        if (path.basename(rutaArchivo) === 'index.html') {
          res.setHeader('Cache-Control', 'no-store');
        }
      },
    }));
    // Fallback de SPA: cualquier ruta que no sea un archivo estático (las de
    // react-router-dom) devuelve index.html, para que el router del lado
    // cliente la resuelva.
    servidor.get('*', (_req, res) => {
      res.setHeader('Cache-Control', 'no-store');
      res.sendFile(path.join(carpetaDist(), 'index.html'));
    });
    servidor.listen(PUERTO, '127.0.0.1', resolve);
  });
}

async function crearVentana() {
  await iniciarServidorEstatico();

  const ventana = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  // Por las dudas quede algo cacheado de una versión anterior de la app
  // (instalada encima de la vieja, mismo puerto): arrancar siempre en limpio.
  await ventana.webContents.session.clearCache();
  ventana.loadURL(`http://localhost:${PUERTO}`);
}

app.whenReady().then(() => {
  crearVentana();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) crearVentana();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
