// Script propio de el-centro.html (antes estaba embebido en el HTML; se movió a un archivo
// aparte para que la Content-Security-Policy del sitio pueda prohibir scripts inline —
// ver _headers). Se carga después de config.js y site.js.

// ---------- LIGHTBOX ----------
const lightbox = document.getElementById('lightbox');
const lightboxImg = document.getElementById('lightboxImg');
const lightboxTrap = window.fktCreateFocusTrap(lightbox);

function abrirLightbox(src, alt) {
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

document.querySelectorAll('.gallery-item').forEach((item) => {
  const img = item.querySelector('img');
  item.setAttribute('role', 'button');
  item.setAttribute('tabindex', '0');
  item.addEventListener('click', () => abrirLightbox(img.src, img.alt));
  item.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); abrirLightbox(img.src, img.alt); } });
});

document.getElementById('lightboxCerrar').addEventListener('click', cerrarLightbox);
lightbox.addEventListener('click', (e) => { if (e.target === lightbox) cerrarLightbox(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && lightbox.classList.contains('open')) cerrarLightbox(); });
