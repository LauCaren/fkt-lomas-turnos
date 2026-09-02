export function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
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
