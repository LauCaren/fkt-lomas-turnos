import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchTurnosOcupados, fetchMedicosAdmin } from './adminSlice';
import { fetchEspecialidades } from '../turnos/turnosSlice';
import { hoyISO } from '../../utils/formato';

function etiquetaFecha(fechaISO) {
  const [y, m, d] = fechaISO.split('-').map(Number);
  const fecha = new Date(Date.UTC(y, m - 1, d));
  const texto = fecha.toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export default function AdminTurnosAgendadosTab() {
  const dispatch = useDispatch();
  const { medicos, turnosOcupados, turnosOcupadosCargando, error } = useSelector((state) => state.admin);
  const especialidades = useSelector((state) => state.turnos.especialidades);

  const [especialidadId, setEspecialidadId] = useState('');
  const [medicoId, setMedicoId] = useState('');
  const [desde, setDesde] = useState(hoyISO());
  const [hasta, setHasta] = useState('');

  const medicosFiltrados = useMemo(() => {
    if (!especialidadId) return medicos;
    return medicos.filter((m) => String(m.especialidadId) === especialidadId);
  }, [medicos, especialidadId]);

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
    dispatch(fetchEspecialidades());
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchTurnosOcupados({ desde: desde || undefined, hasta: hasta || undefined, medicoId, especialidadId }));
  }, [desde, hasta, medicoId, especialidadId, dispatch]);

  function handleEspecialidadChange(nuevaEspecialidadId) {
    setEspecialidadId(nuevaEspecialidadId);
    if (medicoId) {
      const medicoActual = medicos.find((m) => String(m.id) === medicoId);
      if (!medicoActual || (nuevaEspecialidadId && String(medicoActual.especialidadId) !== nuevaEspecialidadId)) {
        setMedicoId('');
      }
    }
  }

  return (
    <div>
      {error && <div className="alert alert-error" role="alert">{error}</div>}

      <p style={{ color: 'var(--muted)', marginBottom: 20 }}>
        Solo turnos ya reservados. Filtrá por profesional para ver únicamente su agenda ocupada.
      </p>

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 20 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Especialidad</label>
          <select value={especialidadId} onChange={(e) => handleEspecialidadChange(e.target.value)}>
            <option value="">Todas las especialidades</option>
            {especialidades.map((esp) => <option key={esp.id} value={esp.id}>{esp.nombre}</option>)}
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Profesional</label>
          <select value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
            <option value="">Todos los profesionales</option>
            {medicosFiltrados.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Desde</label>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Hasta (opcional)</label>
          <input type="date" value={hasta} min={desde || undefined} onChange={(e) => setHasta(e.target.value)} />
        </div>
      </div>

      {turnosOcupadosCargando && <p style={{ color: 'var(--muted)' }}>Buscando turnos…</p>}
      {!turnosOcupadosCargando && turnosOcupados.length === 0 && (
        <p style={{ color: 'var(--muted)' }}>No hay turnos ocupados con esos filtros.</p>
      )}
      {!turnosOcupadosCargando && turnosOcupados.length > 0 && (
        <p style={{ fontSize: 13.5, color: 'var(--muted)', marginBottom: 12 }}>
          {turnosOcupados.length} turno{turnosOcupados.length === 1 ? '' : 's'} ocupado{turnosOcupados.length === 1 ? '' : 's'}
        </p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {turnosOcupados.map((t) => (
          <div key={t.id} className="card" style={{ padding: '16px 20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 15 }}>
                  {etiquetaFecha(t.fecha)} · {t.horaInicio.slice(0, 5)} a {t.horaFin.slice(0, 5)} · {t.medicoNombre}
                </div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>{t.especialidadNombre}</div>
                <div style={{ marginTop: 8, fontSize: 14 }}>
                  <div><strong>{t.pacienteNombre}</strong> · DNI {t.pacienteDni}</div>
                  <div style={{ color: 'var(--muted)' }}>{t.pacienteTelefono} · {t.pacienteObraSocial}</div>
                  {t.motivoConsulta && <div style={{ color: 'var(--muted)', marginTop: 2 }}>Motivo: {t.motivoConsulta}</div>}
                </div>
              </div>
              <span className="badge badge-reservado">Reservado</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
