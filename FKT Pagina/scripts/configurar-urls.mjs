#!/usr/bin/env node
// Cambia de una sola vez las URLs del backend y de la app de turnos en todo el sitio, para
// pasar de desarrollo (localhost) a producción sin buscar a mano en cada .html.
//
// Uso (desde la carpeta "FKT Pagina"):
//   node scripts/configurar-urls.mjs --app https://turnos.ejemplo.com --api https://api.ejemplo.com
//
// Qué toca:
//   - assets/js/config.js: apiUrl y appUrl (lo que usa el JavaScript del sitio).
//   - Todos los .html: los href que apuntan a la app de turnos ("Pedir turno", etc.).
//   - _headers y vercel.json: agrega el origen del backend a la Content-Security-Policy
//     (connect-src e img-src), para que el navegador deje al sitio hablar con la API.
// Se puede correr de nuevo con otras URLs: siempre reemplaza la que esté configurada ahora.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function argumento(nombre) {
  const i = process.argv.indexOf(`--${nombre}`);
  return i > 0 ? process.argv[i + 1] : undefined;
}

function origenValido(valor, nombre) {
  if (!valor) return undefined;
  let url;
  try {
    url = new URL(valor);
  } catch {
    throw new Error(`--${nombre} no es una URL válida: ${valor}`);
  }
  if (url.protocol !== 'https:' && url.hostname !== 'localhost') {
    throw new Error(`--${nombre} tiene que ser https (salvo localhost): ${valor}`);
  }
  return url.origin;
}

const nuevaApp = origenValido(argumento('app'), 'app');
const nuevaApi = origenValido(argumento('api'), 'api');
if (!nuevaApp && !nuevaApi) {
  console.error('Uso: node scripts/configurar-urls.mjs --app <url de la app> --api <url del backend>');
  process.exit(1);
}

const rutaConfig = path.join(raiz, 'assets', 'js', 'config.js');
let config = fs.readFileSync(rutaConfig, 'utf8');
const appActual = config.match(/appUrl: '([^']+)'/)[1];
const apiActual = config.match(/apiUrl: '([^']+)'/)[1];

function reemplazarEn(ruta, de, a) {
  const texto = fs.readFileSync(ruta, 'utf8');
  const nuevo = texto.split(de).join(a);
  if (nuevo !== texto) fs.writeFileSync(ruta, nuevo);
  return nuevo !== texto;
}

if (nuevaApp) {
  config = config.replace(`appUrl: '${appActual}'`, `appUrl: '${nuevaApp}'`);
  const tocados = fs.readdirSync(raiz)
    .filter((f) => f.endsWith('.html'))
    .filter((f) => reemplazarEn(path.join(raiz, f), `href="${appActual}"`, `href="${nuevaApp}"`));
  reemplazarEn(path.join(raiz, 'assets', 'js', 'site.js'), `'${appActual}'`, `'${nuevaApp}'`);
  console.log(`App de turnos: ${appActual} → ${nuevaApp} (${tocados.length} páginas)`);
}
if (nuevaApi) {
  config = config.replace(`apiUrl: '${apiActual}'`, `apiUrl: '${nuevaApi}'`);
  for (const archivo of ['_headers', 'vercel.json']) {
    const ruta = path.join(raiz, archivo);
    if (fs.existsSync(ruta)) reemplazarEn(ruta, apiActual, nuevaApi);
  }
  console.log(`Backend: ${apiActual} → ${nuevaApi}`);
}
fs.writeFileSync(rutaConfig, config);
console.log('Listo. Revisá el diff antes de publicar.');
