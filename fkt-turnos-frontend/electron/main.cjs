const { app, BrowserWindow } = require('electron');
const path = require('node:path');
const express = require('express');

// Puerto fijo local: no usamos file:// porque vite-plugin-pwa registra un
// service worker que se comporta mal bajo ese protocolo (no hay un origen
// real). Serví el build de Vite (dist/) con un servidor estático propio y
// cargamos esa URL, igual que en un navegador normal.
const PUERTO = 43117;

// Si hay dos copias del .app instaladas (ej. una en /Applications y otra
// que quedó suelta en la carpeta del proyecto) y las dos se abren, compiten
// por el mismo puerto fijo — la segunda en arrancar carga silenciosamente
// el servidor de la primera, que puede ser una build vieja, y no hay forma
// de saber cuál se está viendo. Esto lo evita de raíz: si ya hay una
// instancia corriendo, esta simplemente no arranca y le pasa el foco a la
// que ya estaba.
if (!app.requestSingleInstanceLock()) {
  app.quit();
}

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

  // clearCache() solo limpia la caché HTTP normal — no alcanza. Antes de
  // sacarle el plugin de PWA al build de escritorio, una prueba temprana
  // registró un service worker acá dentro (session propia y persistente de
  // Electron, en ~/Library/Application Support/fkt-turnos-frontend): una vez
  // instalado, intercepta cualquier request a este origen y sirve SU PROPIA
  // caché (Cache Storage), sin importar qué tan seguido se reconstruya la
  // app — clearCache() no toca esa caché porque es otro almacenamiento.
  // Desregistrar cualquier service worker + vaciar su caché deja esto en
  // limpio de una vez; no debería volver a registrarse ninguno ya que el
  // build de escritorio no incluye el plugin de PWA.
  await ventana.webContents.session.clearCache();
  await ventana.webContents.session.clearStorageData({ storages: ['serviceworkers', 'cachestorage'] });
  ventana.loadURL(`http://localhost:${PUERTO}`);
}

app.on('second-instance', () => {
  const ventana = BrowserWindow.getAllWindows()[0];
  if (ventana) {
    if (ventana.isMinimized()) ventana.restore();
    ventana.focus();
  }
});

app.whenReady().then(() => {
  crearVentana();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) crearVentana();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
