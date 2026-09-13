// FKT Lomas — comportamiento compartido por las 10 páginas del sitio
// (antes este bloque estaba repetido, casi idéntico, en cada .html).

function imgFallback(img) {
  img.onerror = null;
  img.style.objectFit = 'cover';
  img.src = 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#F3F9F9"/><path d="M150 190 A50 50 0 0 1 250 190" fill="none" stroke="#DFEAEA" stroke-width="4"/><circle cx="200" cy="190" r="4" fill="#239A96"/></svg>');
}

// ---------- ÍNDICE DE BÚSQUEDA ----------
// Contenido real de las 11 páginas (títulos, secciones, profesionales, obras sociales,
// preguntas frecuentes) para que la búsqueda del header lleve directo al lugar exacto en
// vez de obligar a recorrer cada página. "url" con # navega y hace scroll en el sitio;
// las absolutas (http) se abren en pestaña nueva (turnero, WhatsApp).
const FKT_SEARCH_INDEX = [
  { cat: 'Página', title: 'Inicio', snippet: 'Kinesiología y especialidades médicas en Lomas de Zamora.', url: 'index.html' },
  { cat: 'Página', title: 'Servicios', snippet: 'Kinesiología, RPD, ondas de choque radiales y acupuntura kinésica.', url: 'servicios.html' },
  { cat: 'Página', title: 'Especialidades médicas', snippet: 'Traumatología, reumatología, flebología y más, en un mismo centro.', url: 'especialidades.html' },
  { cat: 'Página', title: 'Equipo', snippet: 'Los profesionales de kinesiología y especialidades médicas de FKT Lomas.', url: 'equipo.html' },
  { cat: 'Página', title: 'El Centro', snippet: 'Consultorios privados, jardín con equipamiento y sala de espera.', url: 'el-centro.html' },
  { cat: 'Página', title: 'Opiniones', snippet: 'Reseñas de pacientes en Google.', url: 'opiniones.html' },
  { cat: 'Página', title: 'Contacto', snippet: 'Dirección, teléfono, WhatsApp, horarios y obras sociales.', url: 'contacto.html' },
  { cat: 'Página', title: 'Alquiler de consultorios', snippet: 'Consultorios para profesionales de la salud en Lomas de Zamora.', url: 'alquiler-consultorios.html' },
  { cat: 'Página', title: 'Trabajá con nosotros', snippet: 'Sumate al equipo enviando tu CV en PDF.', url: 'trabaja-con-nosotros.html' },
  { cat: 'Página', title: 'Política de privacidad', snippet: 'Cómo usamos y protegemos tus datos personales.', url: 'politica-de-privacidad.html' },
  { cat: 'Página', title: 'Términos y condiciones', snippet: 'Condiciones de uso del sistema de turnos y del sitio.', url: 'terminos-y-condiciones.html' },

  { cat: 'Servicio', title: 'Kinesiología', snippet: 'Recuperá movilidad, fuerza y funcionalidad tras una lesión, cirugía o dolor crónico.', url: 'servicios.html#kinesiologia' },
  { cat: 'Servicio', title: 'RPD Postural (Reeducación Postural Dinámica)', snippet: 'Corrección de la postura y alivio de contracturas.', url: 'servicios.html#rpd' },
  { cat: 'Servicio', title: 'Ondas de choque radiales', snippet: 'Tecnología no invasiva para dolor crónico y tendinopatías persistentes.', url: 'servicios.html#ondas-de-choque' },
  { cat: 'Servicio', title: 'Acupuntura kinésica', snippet: 'Estimulación de puntos específicos integrada al plan de tratamiento.', url: 'servicios.html#acupuntura' },

  { cat: 'Especialidad', title: 'Traumatología', snippet: 'Lesiones óseas, articulares y musculares.', url: 'especialidades.html#traumatologia' },
  { cat: 'Especialidad', title: 'Reumatología', snippet: 'Enfermedades del sistema músculo-esquelético.', url: 'especialidades.html#reumatologia' },
  { cat: 'Especialidad', title: 'Flebología', snippet: 'Salud venosa y circulatoria.', url: 'especialidades.html#flebologia' },
  { cat: 'Especialidad', title: 'Dermopatología', snippet: 'Diagnóstico y cuidado de la piel.', url: 'especialidades.html#dermopatologia' },
  { cat: 'Especialidad', title: 'Nutrición', snippet: 'Planes alimentarios personalizados.', url: 'especialidades.html#nutricion' },
  { cat: 'Especialidad', title: 'Psicología', snippet: 'Acompañamiento y salud mental.', url: 'especialidades.html#psicologia' },
  { cat: 'Especialidad', title: 'Psiquiatría', snippet: 'Diagnóstico y tratamiento psiquiátrico.', url: 'especialidades.html#psiquiatria' },
  { cat: 'Especialidad', title: 'Psicopedagogía', snippet: 'Aprendizaje y desarrollo.', url: 'especialidades.html#psicopedagogia' },
  { cat: 'Especialidad', title: 'Genética ADN', snippet: 'Estudios genéticos y de filiación.', url: 'especialidades.html#genetica-adn' },
  { cat: 'Especialidad', title: 'Estética', snippet: 'Tratamientos faciales y corporales.', url: 'especialidades.html#estetica' },
  { cat: 'Especialidad', title: 'Pilates', snippet: 'Fortalecimiento y control corporal.', url: 'especialidades.html#pilates' },

  { cat: 'Profesional', title: 'Lic. Fernando Carenzio', snippet: 'Kinesiología · Ondas de choque radiales', url: 'equipo.html#kinesiologia' },
  { cat: 'Profesional', title: 'Lic. Diego Palacios', snippet: 'Kinesiología', url: 'equipo.html#kinesiologia' },
  { cat: 'Profesional', title: 'Lic. Paola Luna', snippet: 'Kinesiología', url: 'equipo.html#kinesiologia' },
  { cat: 'Profesional', title: 'Lic. Agustina Smit', snippet: 'Kinesiología', url: 'equipo.html#kinesiologia' },
  { cat: 'Profesional', title: 'Lic. Belén Borgonovo', snippet: 'RPG · Reeducación Postural Global', url: 'equipo.html#rpg' },
  { cat: 'Profesional', title: 'Lic. Florencia Ramírez', snippet: 'RPG · Reeducación Postural Global', url: 'equipo.html#rpg' },
  { cat: 'Profesional', title: 'Lic. Camila Gómez', snippet: 'RPD · Reeducación Postural Dinámica', url: 'equipo.html#rpd' },
  { cat: 'Profesional', title: 'Lic. Fátima Zánchez', snippet: 'RPD · Reeducación Postural Dinámica', url: 'equipo.html#rpd' },
  { cat: 'Profesional', title: 'Lic. Pablo Sorin', snippet: 'Osteopatía · Terapia manual', url: 'equipo.html#osteopatia' },
  { cat: 'Profesional', title: 'Dra. Graciela Sanchez', snippet: 'Dermopatología', url: 'equipo.html#dermopatologia' },
  { cat: 'Profesional', title: 'Dr. Cristian Indrieri', snippet: 'Traumatología — pierna, tobillo y pie', url: 'equipo.html#traumatologia' },
  { cat: 'Profesional', title: 'Dr. Marcelo Guerra', snippet: 'Traumatología — rodilla', url: 'equipo.html#traumatologia' },
  { cat: 'Profesional', title: 'Dr. Maximiliano Garavaglia', snippet: 'Traumatología — bio-ortopedia', url: 'equipo.html#traumatologia' },
  { cat: 'Profesional', title: 'Dra. Tamara Sisi', snippet: 'Traumatología — columna', url: 'equipo.html#traumatologia' },
  { cat: 'Profesional', title: 'Dr. Leonel Shteimberg', snippet: 'Traumatología — columna', url: 'equipo.html#traumatologia' },
  { cat: 'Profesional', title: 'Dr. Gonzalo Rodriguez', snippet: 'Reumatología', url: 'equipo.html#reumatologia' },
  { cat: 'Profesional', title: 'Dr. Gaston Farjat', snippet: 'Flebología', url: 'equipo.html#flebologia' },
  { cat: 'Profesional', title: 'Dr. Daniel Oyarzun', snippet: 'Flebología', url: 'equipo.html#flebologia' },
  { cat: 'Profesional', title: 'Dr. Sergio Lozano', snippet: 'Genética ADN — investigaciones biomoleculares', url: 'equipo.html#genetica-adn' },
  { cat: 'Profesional', title: 'Dra. Elfi Molina', snippet: 'Nutrición', url: 'equipo.html#nutricion' },
  { cat: 'Profesional', title: 'Lic. Gabriela Morganti', snippet: 'Nutrición', url: 'equipo.html#nutricion' },
  { cat: 'Profesional', title: 'Lic. Lorena Morganti', snippet: 'Psicología', url: 'equipo.html#psicologia' },
  { cat: 'Profesional', title: 'Dr. Aristides Damiano', snippet: 'Psiquiatría', url: 'equipo.html#psiquiatria' },
  { cat: 'Profesional', title: 'Lic. Vanina Kilb', snippet: 'Psicopedagogía', url: 'equipo.html#psicopedagogia' },
  { cat: 'Profesional', title: 'Adriana Fernández', snippet: 'Estética', url: 'equipo.html#estetica' },
  { cat: 'Profesional', title: 'Emilia Aedo', snippet: 'Pilates', url: 'equipo.html#pilates' },

  { cat: 'Cobertura', title: 'Obras sociales y prepagas', snippet: 'Swiss Medical, IOMA, Osmecon, Hospital Alemán, OSPJN, CASA, OSPEDYC, OSPEPBA, OSPIA, OSPIP, OSPTV, OSSEG, Premedic, Sancor Salud, William Hope, ACA Salud, AMEBPBA, Amsterdam Salud, APSOT, Asociación Eclesiástica San Pedro, Centro Médico Pueyrredón, Clínica Espora, Fundación COMEI, DASMI, DASUTEN, Docthos, Colegio de Escribanos, Federada Salud, Futbolistas Agremiados, IOSFA, Jerárquicos Salud, La Pequeña Familia, Luz y Fuerza, Medicar Work, Medifé, Motociclistas, OPDEA, OSAPM, OSFATUN.', url: 'contacto.html#obras-sociales' },

  { cat: 'Pregunta frecuente', title: '¿Qué obras sociales atienden?', snippet: 'Trabajamos con numerosas obras sociales y prepagas — mirá el listado completo. Si la tuya no aparece, escribinos por WhatsApp.', url: 'contacto.html#preguntas-frecuentes' },
  { cat: 'Pregunta frecuente', title: '¿Necesito orden médica para sacar un turno?', snippet: 'Depende de tu obra social: algunas la piden y otras no. Consultanos por WhatsApp antes de tu turno.', url: 'contacto.html#preguntas-frecuentes' },
  { cat: 'Pregunta frecuente', title: '¿Cómo cancelo un turno?', snippet: 'Desde "Mis turnos" en la web, o escribiéndonos por WhatsApp con la mayor anticipación posible.', url: 'contacto.html#preguntas-frecuentes' },
  { cat: 'Pregunta frecuente', title: '¿Puedo atenderme si no tengo obra social?', snippet: 'Sí, atendemos pacientes particulares. El costo se confirma directamente con el centro.', url: 'contacto.html#preguntas-frecuentes' },
  { cat: 'Pregunta frecuente', title: '¿Cuál es el horario de atención?', snippet: 'Lunes a viernes de 8 a 20 hs. No hay turnos los fines de semana.', url: 'contacto.html#preguntas-frecuentes' },
  { cat: 'Pregunta frecuente', title: '¿Puedo sacar un turno para un familiar?', snippet: 'Sí, coordinalo por WhatsApp o llamando al centro con los datos de esa persona.', url: 'contacto.html#preguntas-frecuentes' },
  { cat: 'Pregunta frecuente', title: 'No encuentro turnos disponibles, ¿qué hago?', snippet: 'Escribinos por WhatsApp — a veces se liberan horarios o lo coordinamos con el profesional.', url: 'contacto.html#preguntas-frecuentes' },

  { cat: 'El Centro', title: 'Secretaría dedicada', snippet: 'Un equipo administrativo que coordina tu turno y tu obra social.', url: 'el-centro.html' },
  { cat: 'El Centro', title: 'Espacios privados', snippet: 'Consultorios privados para cada consulta y tratamiento.', url: 'el-centro.html' },
  { cat: 'El Centro', title: 'Equipamiento completo', snippet: 'Jardín con equipamiento de entrenamiento.', url: 'el-centro.html' },
  { cat: 'El Centro', title: 'Sala de espera', snippet: 'Un espacio cómodo para esperar tu turno.', url: 'el-centro.html' },

  { cat: 'Contacto', title: 'Dirección', snippet: 'Juan Larrea 496, Lomas de Zamora, Buenos Aires.', url: 'contacto.html' },
  { cat: 'Contacto', title: 'Teléfono y WhatsApp', snippet: '11 5950-5888 · WhatsApp 11 2351-5402.', url: 'contacto.html' },
  { cat: 'Contacto', title: 'Horario de atención', snippet: 'Lunes a viernes de 8 a 20 hs.', url: 'contacto.html' },
  { cat: 'Acción', title: 'Pedir turno online', snippet: 'Elegí el día y el horario que mejor te quede.', url: 'http://localhost:5173' },
  { cat: 'Acción', title: 'Escribinos por WhatsApp', snippet: 'Consultanos por un turno o por tu cobertura.', url: 'https://wa.me/5491123515402?text=Hola%20FKT%20Lomas%2C%20quiero%20consultar%20por%20un%20turno.' },
];

function fktInitSearch() {
  const navCta = document.querySelector('.nav-cta');
  const mobileCta = document.querySelector('#mobilePanel .nav-cta');
  if (!navCta) return;

  const trigger = document.createElement('button');
  trigger.type = 'button';
  trigger.className = 'search-trigger';
  trigger.setAttribute('aria-label', 'Buscar en el sitio');
  trigger.innerHTML = '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/><line x1="16.2" y1="16.2" x2="21" y2="21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg><span>Buscar</span><kbd>Ctrl K</kbd>';
  navCta.insertBefore(trigger, navCta.firstChild);

  if (mobileCta) {
    const mobileTrigger = trigger.cloneNode(true);
    mobileCta.insertBefore(mobileTrigger, mobileCta.firstChild);
    mobileTrigger.addEventListener('click', openSearch);
  }

  const overlay = document.createElement('div');
  overlay.className = 'search-overlay';
  overlay.setAttribute('role', 'dialog');
  overlay.setAttribute('aria-modal', 'true');
  overlay.innerHTML = `
    <div class="search-panel">
      <div class="search-input-row">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/><line x1="16.2" y1="16.2" x2="21" y2="21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        <input type="text" placeholder="Buscá un servicio, una especialidad, un profesional..." aria-label="Buscar en el sitio" autocomplete="off">
        <span class="search-esc">Esc</span>
      </div>
      <div class="search-results"></div>
      <div class="search-hint">
        <span><kbd>&uarr;</kbd><kbd>&darr;</kbd> navegar</span>
        <span><kbd>&crarr;</kbd> ir</span>
      </div>
    </div>`;
  document.body.appendChild(overlay);

  const input = overlay.querySelector('input');
  const resultsEl = overlay.querySelector('.search-results');
  let activeIndex = -1;
  let currentResults = [];

  function escRe(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
  function highlight(text, q) {
    if (!q) return text;
    return text.replace(new RegExp('(' + escRe(q) + ')', 'ig'), '<mark>$1</mark>');
  }

  function search(query) {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    const words = q.split(/\s+/).filter(Boolean);
    return FKT_SEARCH_INDEX
      .map((item) => {
        const hay = (item.title + ' ' + item.snippet + ' ' + item.cat).toLowerCase();
        let score = 0;
        for (const w of words) {
          if (item.title.toLowerCase().startsWith(w)) score += 6;
          else if (item.title.toLowerCase().includes(w)) score += 4;
          else if (hay.includes(w)) score += 1;
          else return null;
        }
        return score > 0 ? { item, score } : null;
      })
      .filter(Boolean)
      .sort((a, b) => b.score - a.score)
      .slice(0, 8)
      .map((r) => r.item);
  }

  function render(query) {
    currentResults = search(query);
    activeIndex = currentResults.length ? 0 : -1;
    if (!query.trim()) {
      resultsEl.innerHTML = '<div class="search-empty">Empezá a escribir para buscar en todo el sitio.</div>';
      return;
    }
    if (!currentResults.length) {
      resultsEl.innerHTML = '<div class="search-empty">Sin resultados. Probá con otra palabra, o escribinos por WhatsApp.</div>';
      return;
    }
    resultsEl.innerHTML = currentResults.map((item, i) => `
      <div class="search-result${i === 0 ? ' active' : ''}" data-idx="${i}">
        <span class="search-result-cat">${item.cat}</span>
        <span class="search-result-title">${highlight(item.title, query.trim())}</span>
        <span class="search-result-snippet">${item.snippet}</span>
      </div>`).join('');
  }

  function setActive(i) {
    const els = resultsEl.querySelectorAll('.search-result');
    els.forEach((el) => el.classList.remove('active'));
    if (els[i]) { els[i].classList.add('active'); els[i].scrollIntoView({ block: 'nearest' }); }
    activeIndex = i;
  }

  function go(item) {
    if (!item) return;
    if (item.url.startsWith('http')) window.open(item.url, '_blank', 'noopener');
    else window.location.href = item.url;
  }

  function openSearch() {
    overlay.classList.add('open');
    document.body.style.overflow = 'hidden';
    input.value = '';
    render('');
    setTimeout(() => input.focus(), 50);
  }
  function closeSearch() {
    overlay.classList.remove('open');
    document.body.style.overflow = '';
  }

  trigger.addEventListener('click', openSearch);
  overlay.addEventListener('click', (e) => { if (e.target === overlay) closeSearch(); });
  input.addEventListener('input', () => render(input.value));
  resultsEl.addEventListener('click', (e) => {
    const row = e.target.closest('.search-result');
    if (row) go(currentResults[Number(row.dataset.idx)]);
  });
  overlay.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') { closeSearch(); return; }
    if (e.key === 'ArrowDown') { e.preventDefault(); setActive(Math.min(activeIndex + 1, currentResults.length - 1)); }
    if (e.key === 'ArrowUp') { e.preventDefault(); setActive(Math.max(activeIndex - 1, 0)); }
    if (e.key === 'Enter') { e.preventDefault(); go(currentResults[activeIndex]); }
  });
  document.addEventListener('keydown', (e) => {
    const typing = ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName);
    if ((e.key === 'k' && (e.metaKey || e.ctrlKey)) || (e.key === '/' && !typing)) {
      e.preventDefault();
      overlay.classList.contains('open') ? closeSearch() : openSearch();
    }
  });
}

// ---------- FICHA DE TRATAMIENTO (beneficios + casos reales) ----------
// Beneficios generales y honestos por tratamiento (los de servicios ya estaban escritos como
// checks en servicios.html, acá se reutilizan tal cual). Para "casos donde fue efectivo" no
// inventamos testimonios: sólo mostramos las 2 reseñas reales de Google ya publicadas en
// opiniones.html que mencionan Kinesiología puntualmente; para el resto del tratamiento no hay
// una reseña que lo etiquete específicamente, así que se muestra la calificación real agregada
// del centro (4.4/5, +60 reseñas) con link a las reseñas completas, en vez de fabricar una cita.
const FKT_TREATMENTS = {
  'kinesiologia': {
    cat: 'Servicio', title: 'Kinesiología', img: 'assets/img/servicios/kinesiologia.jpg',
    intro: 'Evaluación y tratamiento kinésico para recuperar movilidad, fuerza y funcionalidad después de una lesión, cirugía o dolor crónico. Trabajamos con planes individuales según tu diagnóstico, edad y actividad física.',
    benefits: ['Evaluación y diagnóstico funcional inicial', 'Plan individual según tu diagnóstico y actividad', 'Seguimiento y reevaluación en cada control'],
    reviews: [
      { text: 'Hace mucho tiempo que se atiende con Fernando y destaca su ayuda para recuperarse de varias lesiones.', who: 'Paciente de Kinesiología · Google' },
      { text: 'Un buen espacio para trabajar la kinesiología y mejorar la movilidad, con un grupo de profesionales muy sólido.', who: 'Paciente de Kinesiología · Google' },
    ],
    url: 'servicios.html#kinesiologia',
  },
  'rpd': {
    cat: 'Servicio', title: 'RPD Postural (Reeducación Postural Dinámica)', img: 'assets/img/servicios/rpg.jpg',
    intro: 'Técnica que trabaja cadenas musculares completas —no músculos aislados— para corregir la postura, aliviar contracturas y tratar dolores de espalda de origen postural.',
    benefits: ['Trabajo por cadenas musculares, no músculos aislados', 'Corrección postural progresiva', 'Alivio de contracturas y dolor de espalda'],
    reviews: [],
    url: 'servicios.html#rpd',
  },
  'ondas-de-choque': {
    cat: 'Servicio', title: 'Ondas de choque radiales', img: 'assets/img/servicios/ondas-de-choque.jpg',
    intro: 'Tecnología no invasiva indicada para dolor crónico, tendinopatías y lesiones que no respondieron a otros tratamientos, con sesiones breves y sin tiempo de recuperación posterior. Indicada para tendinitis calcificante del manguito rotador, codo de tenista, trocanteritis, tendinitis rotuliana, fascitis plantar, espolón calcáneo, y epicondilitis o epitrocleitis crónica.',
    benefits: ['Sesiones breves, sin tiempo de recuperación posterior', 'Indicada cuando otros tratamientos no funcionaron', 'Favorece la regeneración tisular'],
    reviews: [],
    url: 'servicios.html#ondas-de-choque',
  },
  'acupuntura': {
    cat: 'Servicio', title: 'Acupuntura kinésica', img: 'assets/img/servicios/acupuntura.jpg',
    intro: 'Estimulación de puntos específicos para reducir el dolor y favorecer la recuperación, integrada al plan de tratamiento kinésico general.',
    benefits: ['Estimulación de puntos específicos', 'Reducción del dolor asociado al tratamiento', 'Integrada a tu plan kinésico general'],
    reviews: [],
    url: 'servicios.html#acupuntura',
  },
  'traumatologia': {
    cat: 'Especialidad', title: 'Traumatología', img: 'assets/img/especialidades/traumatologia.jpg',
    intro: 'Diagnóstico y tratamiento de lesiones óseas, articulares y musculares, con un abordaje de bio-ortopedia para los casos que lo requieren.',
    benefits: ['Diagnóstico y tratamiento de fracturas, esguinces y lesiones articulares', 'Bio-ortopedia: artrosis, tendinopatías, lumbociatalgias y cervicobraquialgias, con plasma rico en plaquetas, ozonoterapia y terapia neural', 'Derivación coordinada con kinesiología para la rehabilitación posterior'],
    reviews: [], url: 'especialidades.html#traumatologia',
  },
  'reumatologia': {
    cat: 'Especialidad', title: 'Reumatología', img: 'assets/img/especialidades/reumatologia.jpg',
    intro: 'Diagnóstico y seguimiento de enfermedades del sistema músculo-esquelético.',
    benefits: ['Diagnóstico y tratamiento de artritis, artrosis y enfermedades autoinmunes', 'Control de la salud ósea y prevención de osteoporosis', 'Tratamiento coordinado con kinesiología cuando hay compromiso articular'],
    reviews: [], url: 'especialidades.html#reumatologia',
  },
  'flebologia': {
    cat: 'Especialidad', title: 'Flebología', img: 'assets/img/especialidades/flebologia.jpg',
    intro: 'Diagnóstico y tratamiento de la salud venosa y circulatoria.',
    benefits: ['Diagnóstico y tratamiento de várices e insuficiencia venosa', 'Evaluación de la circulación en piernas y pies', 'Opciones que incluyen cirugía plástica cuando es necesario'],
    reviews: [], url: 'especialidades.html#flebologia',
  },
  'dermopatologia': {
    cat: 'Especialidad', title: 'Dermopatología', img: 'assets/img/especialidades/dermopatologia.jpg',
    intro: 'Diagnóstico y cuidado de la piel.',
    benefits: ['Diagnóstico de lesiones y enfermedades de la piel', 'Seguimiento dermatológico periódico', 'Derivación oportuna ante lesiones que requieren estudio'],
    reviews: [], url: 'especialidades.html#dermopatologia',
  },
  'nutricion': {
    cat: 'Especialidad', title: 'Nutrición', img: 'assets/img/especialidades/nutricion.jpg',
    intro: 'Planes alimentarios personalizados.',
    benefits: ['Planes alimentarios adaptados a tu objetivo y actividad física', 'Acompañamiento nutricional durante tu tratamiento kinésico', 'Seguimiento periódico para ajustar el plan'],
    reviews: [], url: 'especialidades.html#nutricion',
  },
  'psicologia': {
    cat: 'Especialidad', title: 'Psicología', img: 'assets/img/especialidades/psicologia.jpg',
    intro: 'Acompañamiento y salud mental.',
    benefits: ['Acompañamiento psicológico durante procesos de recuperación', 'Espacio de escucha para el impacto emocional de una lesión o cirugía', 'Sesiones individuales con seguimiento continuo'],
    reviews: [], url: 'especialidades.html#psicologia',
  },
  'psiquiatria': {
    cat: 'Especialidad', title: 'Psiquiatría', img: 'assets/img/especialidades/psiquiatria.jpg',
    intro: 'Diagnóstico y tratamiento psiquiátrico.',
    benefits: ['Diagnóstico y tratamiento de trastornos de salud mental', 'Seguimiento médico de tratamientos psicofarmacológicos', 'Trabajo coordinado con psicología cuando hace falta'],
    reviews: [], url: 'especialidades.html#psiquiatria',
  },
  'psicopedagogia': {
    cat: 'Especialidad', title: 'Psicopedagogía', img: 'assets/img/especialidades/psicopedagogia.jpg',
    intro: 'Aprendizaje y desarrollo.',
    benefits: ['Evaluación de dificultades de aprendizaje', 'Acompañamiento en el desarrollo cognitivo de niños y adolescentes', 'Trabajo articulado con la familia y la escuela'],
    reviews: [], url: 'especialidades.html#psicopedagogia',
  },
  'genetica-adn': {
    cat: 'Especialidad', title: 'Genética ADN', img: 'assets/img/especialidades/genetica-adn.jpg',
    intro: 'Estudios genéticos y de filiación.',
    benefits: ['Estudios de ADN para determinar filiación', 'Investigaciones biomoleculares', 'Asesoramiento sobre el estudio más adecuado para tu caso'],
    reviews: [], url: 'especialidades.html#genetica-adn',
  },
  'estetica': {
    cat: 'Especialidad', title: 'Estética', img: 'assets/img/especialidades/estetica.jpg',
    intro: 'Tratamientos faciales y corporales.',
    benefits: ['Tratamientos faciales personalizados', 'Tratamientos corporales según tu objetivo', 'Evaluación previa para elegir el tratamiento adecuado'],
    reviews: [], url: 'especialidades.html#estetica',
  },
  'pilates': {
    cat: 'Especialidad', title: 'Pilates', img: 'assets/img/especialidades/pilates.jpg',
    intro: 'Fortalecimiento y control corporal.',
    benefits: ['Fortalecimiento muscular con bajo impacto articular', 'Mejora del control corporal y la postura', 'Puede complementar tu tratamiento kinésico'],
    reviews: [], url: 'especialidades.html#pilates',
  },
};

function fktInitTreatments() {
  const cards = document.querySelectorAll('[data-treatment]');
  if (!cards.length) return;

  cards.forEach((card) => {
    const body = card.querySelector('.scard-body, .spec-body');
    if (body && !body.querySelector('.treat-hint')) {
      const hint = document.createElement('span');
      hint.className = 'treat-hint';
      hint.innerHTML = 'Ver beneficios <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 10h12M11 5l5 5-5 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>';
      body.appendChild(hint);
    }
  });

  const overlay = document.createElement('div');
  overlay.className = 'ficha-overlay';
  overlay.setAttribute('role', 'dialog');
  overlay.setAttribute('aria-modal', 'true');
  overlay.innerHTML = `
    <div class="ficha-card treat-card">
      <button class="ficha-cerrar" aria-label="Cerrar">&times;</button>
      <div class="treat-photo"><img src="" alt=""></div>
      <span class="treat-cat"></span>
      <h3 class="treat-title"></h3>
      <p class="treat-intro"></p>
      <div class="treat-benefits-wrap">
        <div class="treat-label">Beneficios</div>
        <ul class="treat-benefits"></ul>
      </div>
      <div class="treat-cases-wrap">
        <div class="treat-label">Casos y resultados</div>
        <div class="treat-cases"></div>
      </div>
      <div class="treat-footer">
        <a class="btn btn-primary treat-wa" href="#" target="_blank" rel="noopener">Consultar por WhatsApp</a>
        <a class="btn btn-outline treat-link" href="#">Ver la página completa →</a>
      </div>
    </div>`;
  document.body.appendChild(overlay);

  const closeBtn = overlay.querySelector('.ficha-cerrar');
  const CHECK_SVG = '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 10.5 L8 14.5 L16 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';

  function open(slug) {
    const t = FKT_TREATMENTS[slug];
    if (!t) return;
    overlay.querySelector('.treat-photo img').src = t.img;
    overlay.querySelector('.treat-photo img').alt = t.title;
    overlay.querySelector('.treat-cat').textContent = t.cat;
    overlay.querySelector('.treat-title').textContent = t.title;
    overlay.querySelector('.treat-intro').textContent = t.intro;
    overlay.querySelector('.treat-benefits').innerHTML = t.benefits.map((b) => `<li>${CHECK_SVG}<span>${b}</span></li>`).join('');

    const casesEl = overlay.querySelector('.treat-cases');
    if (t.reviews.length) {
      casesEl.innerHTML = t.reviews.map((r) => `<div class="treat-review"><p>"${r.text}"</p><span>${r.who}</span></div>`).join('');
    } else {
      casesEl.innerHTML = `<div class="treat-social-proof"><strong>4.4/5</strong><p>Calificación real en Google, sobre más de 60 reseñas de pacientes del centro. <a href="opiniones.html">Ver las reseñas →</a></p></div>`;
    }

    overlay.querySelector('.treat-wa').href = 'https://wa.me/5491123515402?text=' + encodeURIComponent('Hola FKT Lomas, quiero consultar por ' + t.title + '.');
    overlay.querySelector('.treat-link').href = t.url;

    overlay.classList.add('open');
    document.body.style.overflow = 'hidden';
    closeBtn.focus();
  }

  function close() {
    overlay.classList.remove('open');
    document.body.style.overflow = '';
  }

  cards.forEach((card) => {
    card.addEventListener('click', () => open(card.dataset.treatment));
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); open(card.dataset.treatment); }
    });
  });
  closeBtn.addEventListener('click', close);
  overlay.addEventListener('click', (e) => { if (e.target === overlay) close(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && overlay.classList.contains('open')) close(); });
}

// ---------- BARRA STICKY DE RESERVA ----------
function fktInitStickyCta() {
  const hero = document.querySelector('.hero, .page-hero, .team-banner');
  const cta = document.createElement('div');
  cta.className = 'sticky-cta';
  cta.id = 'stickyCta';
  cta.innerHTML = `
    <a class="btn btn-primary" href="http://localhost:5173" target="_blank" rel="noopener">Pedir turno</a>
    <a class="btn btn-outline sticky-cta-wa" href="https://wa.me/5491123515402?text=Hola%20FKT%20Lomas%2C%20quiero%20consultar%20por%20un%20turno." target="_blank" rel="noopener">WhatsApp</a>`;
  document.body.appendChild(cta);

  const threshold = hero ? hero.offsetHeight * 0.6 + hero.offsetTop : 420;
  function onScroll() { cta.classList.toggle('visible', window.scrollY > threshold); }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
}

// ---------- CONTADORES ANIMADOS ----------
function fktInitCounters(reduceMotion) {
  const els = document.querySelectorAll('[data-count-to]');
  if (!els.length) return;
  function animate(el) {
    const to = parseFloat(el.dataset.countTo);
    const decimals = el.dataset.countDecimals ? Number(el.dataset.countDecimals) : 0;
    const suffix = el.dataset.countSuffix || '';
    if (reduceMotion) { el.textContent = to.toFixed(decimals) + suffix; return; }
    const duration = 1100, start = performance.now();
    function tick(now) {
      const t = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = (to * eased).toFixed(decimals) + suffix;
      if (t < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }
  if (reduceMotion) { els.forEach(animate); return; }
  const io = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) { animate(entry.target); io.unobserve(entry.target); }
    });
  }, { threshold: 0.5 });
  els.forEach((el) => io.observe(el));
}

// ---------- PARALAJE SUAVE DEL HERO ----------
function fktInitHeroParallax(reduceMotion) {
  const img = document.querySelector('.hero-visual-bleed img');
  if (!img || reduceMotion) return;
  const wrap = img.closest('.hero-visual-bleed');
  let ticking = false;
  function update() {
    const rect = wrap.getBoundingClientRect();
    const vh = window.innerHeight;
    if (rect.bottom > 0 && rect.top < vh) {
      const progress = (rect.top) / vh; // ~1 al entrar, ~0 centrado, negativo al salir
      img.style.transform = `translateY(${progress * -22}px)`;
    }
    ticking = false;
  }
  window.addEventListener('scroll', () => {
    if (!ticking) { requestAnimationFrame(update); ticking = true; }
  }, { passive: true });
  update();
}

// ---------- RIEL DE PROGRESO DE LOS PASOS NUMERADOS ----------
function fktInitProcessRail() {
  document.querySelectorAll('.process').forEach((process) => {
    const fill = document.createElement('div');
    fill.className = 'process-rail-fill';
    process.appendChild(fill);
    let ticking = false;
    function update() {
      const rect = process.getBoundingClientRect();
      const vh = window.innerHeight;
      const total = rect.height + vh * 0.5;
      const scrolled = vh * 0.85 - rect.top;
      const pct = Math.max(0, Math.min(1, scrolled / total));
      fill.style.width = (pct * 100) + '%';
      ticking = false;
    }
    window.addEventListener('scroll', () => {
      if (!ticking) { requestAnimationFrame(update); ticking = true; }
    }, { passive: true });
    update();
  });
}

document.addEventListener('DOMContentLoaded', () => {
  const burger = document.getElementById('burgerBtn');
  const header = document.getElementById('site-header');
  if (burger && header) {
    burger.addEventListener('click', () => {
      const isOpen = header.classList.toggle('open');
      burger.setAttribute('aria-expanded', isOpen);
    });
    document.querySelectorAll('#mobilePanel a').forEach((a) => {
      a.addEventListener('click', () => {
        header.classList.remove('open');
        burger.setAttribute('aria-expanded', false);
      });
    });
    const progressBar = document.createElement('div');
    progressBar.id = 'scroll-progress';
    progressBar.setAttribute('aria-hidden', 'true');
    header.appendChild(progressBar);

    function onScroll() {
      header.classList.toggle('scrolled', window.scrollY > 8);
      const scrollable = document.documentElement.scrollHeight - window.innerHeight;
      progressBar.style.width = (scrollable > 0 ? (window.scrollY / scrollable) * 100 : 0) + '%';
    }
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const revealEls = document.querySelectorAll('.reveal');
  if (reduceMotion) {
    revealEls.forEach((el) => el.classList.add('in-view'));
  } else {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view');
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.15 });
    revealEls.forEach((el) => io.observe(el));
  }

  fktInitSearch();
  fktInitTreatments();
  fktInitStickyCta();
  fktInitCounters(reduceMotion);
  fktInitHeroParallax(reduceMotion);
  fktInitProcessRail();
});
