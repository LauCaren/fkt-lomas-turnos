// URL del sitio de marketing estático (FKT Pagina/). En producción hay que
// configurar VITE_SITE_URL con la URL real una vez desplegado.
export const SITE_URL = import.meta.env.VITE_SITE_URL || 'http://localhost:5500';

// electron/main.cjs no pisa el user agent por defecto de Electron, así que
// esto alcanza para distinguir la app de escritorio (solo paneles internos)
// del navegador normal, sin necesidad de un preload/contextBridge.
export const isElectron = typeof navigator !== 'undefined' && /Electron/i.test(navigator.userAgent);
