// FKT Lomas — comportamiento compartido por las 10 páginas del sitio
// (antes este bloque estaba repetido, casi idéntico, en cada .html).

function imgFallback(img) {
  img.onerror = null;
  img.style.objectFit = 'cover';
  img.src = 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#F3F9F9"/><path d="M150 190 A50 50 0 0 1 250 190" fill="none" stroke="#DFEAEA" stroke-width="4"/><circle cx="200" cy="190" r="4" fill="#239A96"/></svg>');
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
    window.addEventListener('scroll', () => {
      header.classList.toggle('scrolled', window.scrollY > 8);
    }, { passive: true });
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

  const counterEl = document.getElementById('degCounter');
  if (counterEl) {
    const TARGET = 132, DURATION = 1700, DELAY = 300;
    if (reduceMotion) {
      counterEl.textContent = TARGET + '°';
    } else {
      const start = performance.now() + DELAY;
      const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);
      const tick = (now) => {
        const elapsed = now - start;
        if (elapsed < 0) { requestAnimationFrame(tick); return; }
        const t = Math.min(elapsed / DURATION, 1);
        counterEl.textContent = Math.round(easeOutCubic(t) * TARGET) + '°';
        if (t < 1) requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    }
  }
});
