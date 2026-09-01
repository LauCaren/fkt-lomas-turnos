import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchColaEsperaAdmin, fetchMedicosAdmin } from './adminSlice';

function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export default function AdminColaEsperaTab() {
  const dispatch = useDispatch();
  const { medicos, colaEspera, colaEsperaCargando } = useSelector((state) => state.admin);
  const [medicoId, setMedicoId] = useState('');
  const fecha = hoyISO();

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchColaEsperaAdmin({ fecha, medicoId }));
    const intervalo = setInterval(() => dispatch(fetchColaEsperaAdmin({ fecha, medicoId })), 10000);
    return () => clearInterval(intervalo);
  }, [dispatch, fecha, medicoId]);

  return (
    <div>
      <p style={{ color: 'var(--muted)', marginBottom: 16 }}>
        Pacientes marcados como "Llegó" en la agenda de hoy, todavía no atendidos por el médico — ordenados por el
        horario de su turno.
      </p>

      <div className="field" style={{ maxWidth: 280, marginBottom: 20 }}>
        <label>Médico</label>
        <select value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
          <option value="">Todos los médicos</option>
          {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
        </select>
      </div>

      {colaEsperaCargando && <p style={{ color: 'var(--muted)' }}>Cargando…</p>}
      {!colaEsperaCargando && colaEspera.length === 0 && <p style={{ color: 'var(--muted)' }}>No hay pacientes esperando en este momento.</p>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {colaEspera.map((t, i) => (
          <div key={t.id} className="card" style={{ padding: '14px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <span style={{ fontSize: 20, fontWeight: 700, color: 'var(--teal)', minWidth: 24 }}>{i + 1}</span>
              <div>
                <div style={{ fontWeight: 600 }}>{t.pacienteNombre}</div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>
                  Turno {t.horaInicio.slice(0, 5)} · {t.medicoNombre} · {t.especialidadNombre}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
