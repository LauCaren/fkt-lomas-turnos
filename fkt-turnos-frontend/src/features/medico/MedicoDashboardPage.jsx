import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchSaldoMedico, limpiarMensajesMedico } from './medicoSlice';
import MedicoAgendaTab from './MedicoAgendaTab';
import MedicoColaEsperaTab from './MedicoColaEsperaTab';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';

function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

const TABS = [
  { id: 'agenda', label: 'Mi agenda de hoy' },
  { id: 'cola-espera', label: 'Cola de espera' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function MedicoDashboardPage() {
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const { error, mensajeExito, saldo } = useSelector((state) => state.medico);
  const [tab, setTab] = useState('agenda');

  useEffect(() => { dispatch(fetchSaldoMedico()); }, [dispatch]);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <span className="eyebrow">Panel del médico</span>
          <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, {usuario?.nombreCompleto}</h1>
        </div>
        {saldo && (
          <div className="card" style={{ padding: '10px 18px', textAlign: 'right' }}>
            <div style={{ fontSize: 12.5, color: 'var(--muted)' }}>Tu saldo acumulado</div>
            <div style={{ fontSize: 20, fontWeight: 700 }}>{formatMoneda(saldo.saldo)}</div>
          </div>
        )}
      </div>

      {error && <div className="alert alert-error" style={{ marginTop: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" style={{ marginTop: 20 }}>{mensajeExito}</div>}

      <div style={{ display: 'flex', gap: 8, marginTop: 24, marginBottom: 28, flexWrap: 'wrap', borderBottom: '1px solid var(--line)' }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => { setTab(t.id); dispatch(limpiarMensajesMedico()); }}
            style={{
              padding: '10px 4px',
              marginRight: 20,
              border: 'none',
              background: 'none',
              fontWeight: 600,
              fontSize: 14.5,
              color: tab === t.id ? 'var(--ink)' : 'var(--muted)',
              borderBottom: tab === t.id ? '2px solid var(--teal)' : '2px solid transparent',
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'agenda' && <MedicoAgendaTab />}
      {tab === 'cola-espera' && <MedicoColaEsperaTab />}
      {tab === 'cuenta' && <CuentaSeguridadTab />}
    </div>
  );
}
