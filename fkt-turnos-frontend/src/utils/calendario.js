function pad(n) {
  return String(n).padStart(2, '0');
}

// Argentina está en UTC-3 todo el año (sin horario de verano desde 2009),
// así que la conversión es siempre sumar 3 horas, sin casos especiales.
function aUTC(fecha, hora) {
  const [y, m, d] = fecha.split('-').map(Number);
  const [hh, mm] = hora.split(':').map(Number);
  return new Date(Date.UTC(y, m - 1, d, hh + 3, mm));
}

function formatoUTC(date) {
  return `${date.getUTCFullYear()}${pad(date.getUTCMonth() + 1)}${pad(date.getUTCDate())}T${pad(date.getUTCHours())}${pad(date.getUTCMinutes())}00Z`;
}

function escapeICS(texto) {
  return String(texto).replace(/\\/g, '\\\\').replace(/,/g, '\\,').replace(/;/g, '\\;').replace(/\n/g, '\\n');
}

const UBICACION = 'Juan Larrea 496, Lomas de Zamora, Buenos Aires';

export function urlGoogleCalendar(turno) {
  const inicio = formatoUTC(aUTC(turno.fecha, turno.horaInicio));
  const fin = formatoUTC(aUTC(turno.fecha, turno.horaFin));
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: `${turno.especialidadNombre} con ${turno.medicoNombre} — FKT Lomas`,
    dates: `${inicio}/${fin}`,
    details: 'Turno en FKT Lomas, Fisio Kinesio Terapia.',
    location: UBICACION,
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

export function descargarICS(turno) {
  const inicio = formatoUTC(aUTC(turno.fecha, turno.horaInicio));
  const fin = formatoUTC(aUTC(turno.fecha, turno.horaFin));
  const ahora = formatoUTC(new Date());
  const resumen = escapeICS(`${turno.especialidadNombre} con ${turno.medicoNombre} — FKT Lomas`);

  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//FKT Lomas//Turnos//ES',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:turno-${turno.id}@fktlomas.com`,
    `DTSTAMP:${ahora}`,
    `DTSTART:${inicio}`,
    `DTEND:${fin}`,
    `SUMMARY:${resumen}`,
    'DESCRIPTION:Turno en FKT Lomas\\, Fisio Kinesio Terapia.',
    `LOCATION:${escapeICS(UBICACION)}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ].join('\r\n');

  const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `turno-fkt-lomas-${turno.fecha}.ics`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
