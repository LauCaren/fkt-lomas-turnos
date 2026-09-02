// URL del sitio de marketing estático (FKT Pagina/). En producción hay que
// configurar VITE_SITE_URL con la URL real una vez desplegado.
export const SITE_URL = import.meta.env.VITE_SITE_URL || 'http://localhost:5500';

// electron/main.cjs no pisa el user agent por defecto de Electron, así que
// esto alcanza para distinguir la app de escritorio (solo paneles internos)
// del navegador normal, sin necesidad de un preload/contextBridge.
export const isElectron = typeof navigator !== 'undefined' && /Electron/i.test(navigator.userAgent);

// A dónde va cada rol después de loguearse — una sola fuente de verdad para
// App.jsx (InicioRedirect) y LoginPage, que antes tenían cada uno su propia
// copia del mapeo y podían desincronizarse si se agregaba un rol nuevo.
export const RUTA_POR_ROL = { ADMIN: '/admin', MEDICO: '/medico', CONTADOR: '/contador' };
