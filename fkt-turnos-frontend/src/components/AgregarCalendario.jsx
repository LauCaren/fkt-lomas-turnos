import { urlGoogleCalendar, descargarICS } from '../utils/calendario';

export default function AgregarCalendario({ turno }) {
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      <a
        className="btn btn-outline"
        style={{ padding: '6px 14px', fontSize: 13 }}
        href={urlGoogleCalendar(turno)}
        target="_blank"
        rel="noopener noreferrer"
      >
        Google Calendar
      </a>
      <button
        type="button"
        className="btn btn-outline"
        style={{ padding: '6px 14px', fontSize: 13 }}
        onClick={() => descargarICS(turno)}
      >
        Apple / Outlook (.ics)
      </button>
    </div>
  );
}
