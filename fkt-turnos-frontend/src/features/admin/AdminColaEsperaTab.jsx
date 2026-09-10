import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchColaEsperaAdmin, fetchMedicosAdmin } from './adminSlice';
import { hoyISO } from '../../utils/formato';
import SalaEsperaQr from './SalaEsperaQr';
import EstadoVacio from '../../components/EstadoVacio';

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
      <SalaEsperaQr />

      <p style={{ color: 'var(--muted)', marginBottom: 16 }}>
        Pacientes marcados como "Llegó" en la agenda de hoy, todavía no atendidos por el médico — ordenados por el
        horario de su turno.
      </p>

      <div className="filter-bar">
        <div className="field" style={{ maxWidth: 280 }}>
          <label htmlFor="colaEsperaMedico">Médico</label>
          <select id="colaEsperaMedico" value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
            <option value="">Todos los médicos</option>
            {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
      </div>

      {colaEsperaCargando && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[0, 1].map((i) => (
            <div key={i} className="card" style={{ padding: '14px 20px' }}>
              <span className="skeleton skeleton-line" style={{ width: '30%' }} />
              <span className="skeleton skeleton-line" style={{ width: '55%' }} />
            </div>
          ))}
        </div>
      )}
      {!colaEsperaCargando && colaEspera.length === 0 && <EstadoVacio titulo="Sin pacientes esperando" subtitulo="No hay pacientes esperando en este momento." />}

      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
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
