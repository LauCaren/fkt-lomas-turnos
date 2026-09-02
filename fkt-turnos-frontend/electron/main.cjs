const { app, BrowserWindow, dialog } = require('electron');
const path = require('node:path');
const express = require('express');
const { autoUpdater } = require('electron-updater');

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
    // Defensa en profundidad: si alguna vez hay un XSS en el bundle de React, esto
    // impide que ejecute scripts/estilos inline o cargue nada de un origen ajeno —
    // el backend ya manda su propio CSP en sus respuestas, pero este es un servidor
    // Express aparte (el bundle estático de dist/), que no la hereda.
    servidor.use((_req, res, next) => {
      // connect-src apunta al mismo backend que VITE_API_URL (.env.production) — hoy
      // todavía localhost:8080 porque no se desplegó (ver README, sección Despliegue).
      // Actualizar acá junto con VITE_API_URL cuando haya un dominio real, y volver a
      // correr "npm run desktop:build".
      res.setHeader('Content-Security-Policy',
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
        + "img-src 'self' data:; connect-src 'self' http://localhost:8080; "
        + "frame-ancestors 'none'; object-src 'none'; base-uri 'none'");
      next();
    });
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
    icon: path.join(__dirname, 'build', 'icon.png'),
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

  const origenPermitido = `http://localhost:${PUERTO}`;
  // Sin esto, cualquier link/redirect que termine ejecutándose adentro de la SPA (un XSS
  // futuro, un dato malicioso pegado en un campo) podría navegar toda la ventana a una
  // página externa que se hace pasar por la app — mismo ícono, mismo título, sin barra de
  // direcciones que lo delate. React Router usa la History API (pushState), que no dispara
  // "will-navigate", así que esto no interfiere con la navegación normal de la app.
  ventana.webContents.on('will-navigate', (evento, url) => {
    if (!url.startsWith(origenPermitido)) {
      evento.preventDefault();
    }
  });

  ventana.loadURL(origenPermitido);
}

// Solo tiene sentido contra un build empaquetado y publicado — en desarrollo
// (electron .) no hay feed de actualizaciones ni instalador que reemplazar.
function configurarAutoUpdater() {
  if (!app.isPackaged) return;

  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;

  autoUpdater.on('update-downloaded', async (info) => {
    const { response } = await dialog.showMessageBox({
      type: 'info',
      title: 'Actualización disponible',
      message: `Hay una nueva versión de FKT Lomas Admin (${info.version}) lista para instalar.`,
      detail: 'Se va a instalar la próxima vez que cierres el programa, o ahora mismo si preferís.',
      buttons: ['Instalar ahora', 'Más tarde'],
      defaultId: 0,
      cancelId: 1,
    });
    if (response === 0) {
      autoUpdater.quitAndInstall();
    }
  });

  // Fallas de red o feed sin configurar todavía (ver electron-builder.yml / build.publish
  // en package.json) no deben interrumpir a la secretaria — solo se loguean.
  autoUpdater.on('error', (error) => {
    console.error('Error buscando actualizaciones:', error);
  });

  autoUpdater.checkForUpdates().catch((error) => {
    console.error('No se pudo verificar actualizaciones:', error);
  });
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
  configurarAutoUpdater();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) crearVentana();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
