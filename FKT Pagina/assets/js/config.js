// Configuración del sitio que cambia entre desarrollo y producción — el ÚNICO lugar donde se
// definen estas URLs para el JavaScript del sitio (site.js y assets/js/paginas/*.js las leen de
// acá). Los links de los botones "Pedir turno" del HTML también apuntan a appUrl: para
// cambiarlas todas juntas antes de publicar, usar el script del README:
//
//   node scripts/configurar-urls.mjs --app https://turnos.ejemplo.com --api https://api.ejemplo.com
//
// que reescribe este archivo y los links de los .html de una sola vez.
window.FKT_CONFIG = Object.freeze({
  // Backend (Spring Boot): formularios de contacto y postulaciones, equipo y reseñas.
  apiUrl: 'http://localhost:8080',
  // App de turnos (React): a dónde llevan los botones "Pedir turno".
  appUrl: 'http://localhost:5173',
});
