const PREFIJO = 'fkt.uiPrefs.v1.';

// Preferencias de UI por pantalla (vista elegida, filtros, densidad) para que la secretaria
// no tenga que reconfigurar lo mismo cada vez que entra — nunca la fecha, que siempre debe
// arrancar en hoy. Todo en localStorage: es por-navegador, no hace falta backend para esto.
export function leerPref(clave, porDefecto) {
  try {
    const guardado = localStorage.getItem(PREFIJO + clave);
    return guardado !== null ? JSON.parse(guardado) : porDefecto;
  } catch {
    return porDefecto;
  }
}

export function guardarPref(clave, valor) {
  try {
    localStorage.setItem(PREFIJO + clave, JSON.stringify(valor));
  } catch {
    // localStorage puede fallar (modo privado, cuota llena) — no es crítico, se sigue sin persistir.
  }
}
