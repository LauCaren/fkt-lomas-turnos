import { useCallback, useEffect, useState } from 'react';

const CLAVE_STORAGE = 'fkt-theme';

// Mismo mecanismo que FKT Pagina (assets/js/site.js `fktInitThemeToggle`) — data-theme en
// <html> manda siempre que exista, persistido en localStorage; sin eso, sigue
// prefers-color-scheme del sistema. El script inline en index.html deja seteado data-theme
// antes de que React monte, para que no haya flash de tema claro al cargar en un dispositivo
// en modo oscuro.
function leerTemaActual() {
  const explicito = document.documentElement.getAttribute('data-theme');
  if (explicito === 'dark' || explicito === 'light') return explicito;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function useTema() {
  const [tema, setTema] = useState(leerTemaActual);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema);
    try { localStorage.setItem(CLAVE_STORAGE, tema); } catch { /* localStorage no disponible (modo privado, etc.) */ }
  }, [tema]);

  const alternar = useCallback(() => {
    setTema((actual) => (actual === 'dark' ? 'light' : 'dark'));
  }, []);

  return { tema, alternar };
}
