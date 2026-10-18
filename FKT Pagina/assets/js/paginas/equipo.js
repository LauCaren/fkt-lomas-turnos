// Script propio de equipo.html (antes estaba embebido en el HTML; se movió a un archivo
// aparte para que la Content-Security-Policy del sitio pueda prohibir scripts inline —
// ver _headers). Se carga después de config.js y site.js.

// ---------- FICHA DE PROFESIONAL ----------
// URL del backend: se configura una sola vez para todo el sitio en assets/js/config.js.
const API_URL = window.FKT_CONFIG.apiUrl;
const FOTO_PLACEHOLDER = 'data:image/svg+xml;utf8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 110 130"><rect width="110" height="130" fill="#F3F9F9"/>' +
  '<circle cx="55" cy="48" r="22" fill="#DFEAEA"/><path d="M18 118 A37 42 0 0 1 92 118" fill="#DFEAEA"/></svg>'
);

const overlay = document.getElementById('fichaOverlay');
const fichaTrap = window.fktCreateFocusTrap(overlay);
const elFoto = document.getElementById('fichaFoto');
const elNombre = document.getElementById('fichaNombre');
const elEspecialidad = document.getElementById('fichaEspecialidad');
const elExperiencia = document.getElementById('fichaExperiencia');
const elTituloBox = document.getElementById('fichaTituloBox');
const elTituloImg = document.getElementById('fichaTituloImg');
const elVerificado = document.getElementById('fichaVerificado');
const elHorariosLista = document.getElementById('fichaHorariosLista');
const elHorariosVacio = document.getElementById('fichaHorariosVacio');

const DIAS_SEMANA = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles', THURSDAY: 'Jueves',
  FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo',
};
const ORDEN_DIAS = Object.keys(DIAS_SEMANA);

function cargarHorarios(medicoId) {
  elHorariosLista.innerHTML = '';
  elHorariosVacio.style.display = 'none';
  fetch(`${API_URL}/api/medicos/${medicoId}/horarios`)
    .then((res) => res.json())
    .then((reglas) => {
      if (!reglas.length) {
        elHorariosVacio.textContent = 'Todavía no cargamos los horarios de este profesional acá — escribinos y te confirmamos disponibilidad.';
        elHorariosVacio.style.display = '';
        return;
      }
      reglas
        .slice()
        .sort((a, b) => ORDEN_DIAS.indexOf(a.diaSemana) - ORDEN_DIAS.indexOf(b.diaSemana) || a.horaInicio.localeCompare(b.horaInicio))
        .forEach((regla) => {
          const li = document.createElement('li');
          const dia = document.createElement('span');
          dia.textContent = DIAS_SEMANA[regla.diaSemana] || regla.diaSemana;
          const horario = document.createElement('span');
          horario.textContent = `${regla.horaInicio.slice(0, 5)} a ${regla.horaFin.slice(0, 5)}`;
          li.append(dia, horario);
          elHorariosLista.appendChild(li);
        });
    })
    .catch(() => {
      elHorariosVacio.textContent = 'No pudimos cargar los horarios en este momento — escribinos por WhatsApp y te confirmamos disponibilidad.';
      elHorariosVacio.style.display = '';
    });
}

function abrirFicha(medico) {
  elFoto.src = medico.tieneFoto ? `${API_URL}/api/medicos/${medico.id}/foto` : FOTO_PLACEHOLDER;
  elNombre.textContent = medico.nombreCompleto;
  elEspecialidad.textContent = medico.especialidadNombre;
  elExperiencia.textContent = medico.anosExperiencia
    ? `${medico.anosExperiencia} ${medico.anosExperiencia === 1 ? 'año' : 'años'} de experiencia`
    : '';
  if (medico.tieneTitulo) {
    elTituloImg.src = `${API_URL}/api/medicos/${medico.id}/titulo`;
    elTituloBox.style.display = '';
    elVerificado.style.display = '';
  } else {
    elTituloBox.style.display = 'none';
    elVerificado.style.display = 'none';
  }
  cargarHorarios(medico.id);
  fichaTrap.remember();
  overlay.classList.add('open');
  overlay.setAttribute('aria-hidden', 'false');
  document.getElementById('fichaCerrar').focus();
}

function cerrarFicha() {
  overlay.classList.remove('open');
  overlay.setAttribute('aria-hidden', 'true');
  fichaTrap.restore();
}

document.getElementById('fichaCerrar').addEventListener('click', cerrarFicha);
overlay.addEventListener('click', (e) => { if (e.target === overlay) cerrarFicha(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && overlay.classList.contains('open')) cerrarFicha(); });

// ---------- LIGHTBOX (ampliar foto / título) ----------
const lightbox = document.getElementById('lightbox');
const lightboxImg = document.getElementById('lightboxImg');
const lightboxTrap = window.fktCreateFocusTrap(lightbox);

function abrirLightbox(src, alt) {
  if (src.startsWith('data:image/svg+xml')) return;
  lightboxTrap.remember();
  lightboxImg.src = src;
  lightboxImg.alt = alt;
  lightbox.classList.add('open');
  lightbox.setAttribute('aria-hidden', 'false');
  document.getElementById('lightboxCerrar').focus();
}

function cerrarLightbox() {
  lightbox.classList.remove('open');
  lightbox.setAttribute('aria-hidden', 'true');
  lightboxTrap.restore();
}

elFoto.addEventListener('click', () => abrirLightbox(elFoto.src, elNombre.textContent));
elTituloImg.addEventListener('click', () => abrirLightbox(elTituloImg.src, `Título de ${elNombre.textContent}`));
document.getElementById('lightboxCerrar').addEventListener('click', cerrarLightbox);
lightbox.addEventListener('click', (e) => { if (e.target === lightbox) cerrarLightbox(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && lightbox.classList.contains('open')) cerrarLightbox(); });

fetch(`${API_URL}/api/medicos`)
  .then((res) => res.json())
  .then((medicos) => {
    const porNombre = new Map(medicos.map((m) => [m.nombreCompleto.trim(), m]));
    document.querySelectorAll('.team-card li').forEach((li) => {
      const nombrePlano = li.textContent.split('—')[0].trim();
      const medico = porNombre.get(nombrePlano);
      if (!medico) return;
      li.classList.add('clickable');
      li.setAttribute('role', 'button');
      li.setAttribute('tabindex', '0');
      li.addEventListener('click', () => abrirFicha(medico));
      li.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); abrirFicha(medico); } });
      // Avatar chico solo cuando hay foto real cargada — nada de placeholders genéricos
      // repetidos al lado de cada nombre, que se verían más a relleno que a dato real.
      if (medico.tieneFoto) {
        const avatar = document.createElement('img');
        avatar.className = 'li-avatar';
        avatar.src = `${API_URL}/api/medicos/${medico.id}/foto`;
        avatar.alt = '';
        avatar.loading = 'lazy';
        li.insertBefore(avatar, li.firstChild);
      }
    });
  })
  .catch(() => {
    // Sin conexión al backend: la página sigue funcionando, solo no quedan clickeables los
    // nombres — pero mejor avisar que quedar en silencio, para que no parezca que faltan a propósito.
    const notice = document.createElement('p');
    notice.className = 'team-fetch-notice';
    notice.innerHTML = 'No pudimos cargar las fotos y fichas del equipo en este momento. Los datos de cada profesional siguen siendo correctos — <a href="https://wa.me/5491123515402?text=Hola%2C%20quiero%20consultar%20por%20un%20profesional%20del%20equipo." target="_blank" rel="noopener">escribinos por WhatsApp</a> si necesitás algo puntual.';
    document.querySelector('.team-grid').insertAdjacentElement('beforebegin', notice);
  });
