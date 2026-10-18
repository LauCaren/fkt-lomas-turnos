export function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

// Fecha y hora de un instante del backend (Instant serializado, ej. "2026-10-17T22:40:00Z"),
// en hora local: "17/10/2026, 19:40:00". Un solo lugar para que todas las bandejas (mensajes,
// postulaciones, auditoría, caja...) muestren las fechas igual.
export function formatFechaHora(instanteISO) {
  return instanteISO ? new Date(instanteISO).toLocaleString('es-AR') : '';
}

export function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Recibe una fecha "YYYY-MM-DD" y devuelve "DD/MM" — a propósito por texto, no Date(),
// para no arrastrar corrimientos de huso horario al parsear una fecha sin hora.
export function formatFechaCorta(fechaISO) {
  const [, mes, dia] = fechaISO.split('-');
  return `${dia}/${mes}`;
}
