// Script propio de opiniones.html (antes estaba embebido en el HTML; se movió a un archivo
// aparte para que la Content-Security-Policy del sitio pueda prohibir scripts inline —
// ver _headers). Se carga después de config.js y site.js.

const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// Rating count-up + star fill
const RATING = 4.4;
const numEl = document.getElementById('ratingNum');
const starsEl = document.getElementById('ratingStars');
const STAR_PATH = 'M10 1l2.6 5.6 6.1.6-4.6 4.1 1.3 6-5.4-3.2L4.6 17.3l1.3-6L1.3 7.2l6.1-.6z';
for (let i=0;i<5;i++){
  const pct = Math.max(0, Math.min(1, RATING - i)) * 100;
  const wrap = document.createElement('span');
  wrap.style.position='relative'; wrap.style.display='inline-block'; wrap.style.width='26px'; wrap.style.height='26px';
  wrap.innerHTML = '<svg viewBox="0 0 20 20" style="position:absolute;inset:0;width:100%;height:100%;fill:rgba(255,255,255,0.18)"><path d="'+STAR_PATH+'"/></svg>' +
    '<svg class="star-fill" data-pct="'+pct+'" viewBox="0 0 20 20" style="position:absolute;inset:0;width:100%;height:100%;fill:currentColor"><path d="'+STAR_PATH+'"/></svg>';
  starsEl.appendChild(wrap);
}

function animateRating(){
  const stars = starsEl.querySelectorAll('.star-fill');
  stars.forEach(s => { s.style.clipPath = 'inset(0 ' + (100 - parseFloat(s.dataset.pct)) + '% 0 0)'; });
  if (reduceMotion) { numEl.textContent = RATING.toFixed(1); return; }
  const DURATION = 1200, start = performance.now();
  function tick(now){
    const t = Math.min((now-start)/DURATION, 1);
    numEl.textContent = (RATING * t).toFixed(1);
    if (t < 1) requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
}
if (reduceMotion) { animateRating(); }
else {
  const ratingIO = new IntersectionObserver((entries)=>{ entries.forEach(e=>{ if(e.isIntersecting){ animateRating(); ratingIO.disconnect(); } }); }, {threshold:0.4});
  ratingIO.observe(document.getElementById('ratingNum'));
}

// Reseñas verificadas: reemplazan las 3 tarjetas fijas de arriba por reseñas reales de
// pacientes que de verdad tuvieron un turno atendido (ver ResenaService en el backend),
// moderadas por el centro antes de aparecer acá. Si el backend no responde o todavía no hay
// ninguna aprobada, se dejan las tarjetas de Google de siempre — mismo criterio que equipo.html.
// URL del backend: se configura una sola vez para todo el sitio en assets/js/config.js.
const API_URL = window.FKT_CONFIG.apiUrl;
const STAR_PATH_MINI = 'M10 1l2.6 5.6 6.1.6-4.6 4.1 1.3 6-5.4-3.2L4.6 17.3l1.3-6L1.3 7.2l6.1-.6z';

function estrellasSvg(puntaje) {
  let html = '';
  for (let i = 0; i < 5; i++) {
    const relleno = i < puntaje ? 'currentColor' : 'var(--line)';
    html += '<svg viewBox="0 0 20 20" fill="' + relleno + '"><path d="' + STAR_PATH_MINI + '"/></svg>';
  }
  return html;
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function tarjetaResena(r) {
  const texto = r.comentario || 'Calificó su atención en FKT Lomas con ' + r.puntaje + ' de 5 estrellas.';
  return '<div class="review-card">' +
    '<div class="stars-mini" aria-hidden="true">' + estrellasSvg(r.puntaje) + '</div>' +
    '<p class="txt">' + escapeHtml(texto) + '</p>' +
    '<div class="who"><span>' + escapeHtml(r.pacienteNombreCorto) + ' — ' + escapeHtml(r.especialidadNombre) + '</span><span class="src">Verificada</span></div>' +
    '</div>';
}

fetch(API_URL + '/api/resenas?limite=6')
  .then((res) => res.ok ? res.json() : [])
  .then((resenas) => {
    if (!Array.isArray(resenas) || resenas.length === 0) return;
    const grid = document.querySelector('.reviews-grid');
    const nota = document.querySelector('.reviews-note');
    grid.innerHTML = resenas.map(tarjetaResena).join('');
    if (nota) nota.textContent = 'Reseñas verificadas: solo pacientes que tuvieron un turno atendido en FKT Lomas pueden dejarlas, y el centro las revisa antes de publicarlas.';
  })
  .catch(() => {});
