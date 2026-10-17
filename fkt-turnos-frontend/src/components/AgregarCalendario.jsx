import { CalendarPlus, Download } from 'lucide-react';
import { urlGoogleCalendar, descargarICS } from '../utils/calendario';

export default function AgregarCalendario({ turno, conTitulo = false }) {
  return (
    <div className="agendar">
      {conTitulo && <p className="agendar-titulo">Agendalo en tu calendario</p>}
      <div className="agendar-botones">
        <a
          className="btn btn-outline"
          href={urlGoogleCalendar(turno)}
          target="_blank"
          rel="noopener noreferrer"
        >
          <CalendarPlus size={18} aria-hidden="true" />
          Google Calendar
        </a>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => descargarICS(turno)}
        >
          <Download size={18} aria-hidden="true" />
          Apple / Outlook
        </button>
      </div>
    </div>
  );
}
