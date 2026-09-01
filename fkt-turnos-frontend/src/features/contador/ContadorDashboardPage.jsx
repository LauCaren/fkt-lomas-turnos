import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';

function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

function ResumenCajaTab() {
  const [resumen, setResumen] = useState(null);
  const [ingresos, setIngresos] = useState([]);
  const [gastos, setGastos] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([
      axiosClient.get('/api/contador/caja/resumen'),
      axiosClient.get('/api/contador/caja/ingresos'),
      axiosClient.get('/api/contador/caja/gastos'),
    ]).then(([r, i, g]) => {
      setResumen(r.data);
      setIngresos(i.data);
      setGastos(g.data);
    }).catch((err) => setError(mensajeDeError(err)));
  }, []);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!resumen) return <p style={{ color: 'var(--muted)' }}>Cargando…</p>;

  return (
    <>
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 28 }}>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>Caja física</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.cajaFisica)}</div>
        </div>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>Ingresos totales</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.ingresosTotal)}</div>
        </div>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>Retiros totales</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.retirosTotal)}</div>
        </div>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>Gastos totales</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.gastosTotal)}</div>
        </div>
      </div>

      <h2 style={{ fontSize: 18, marginBottom: 12 }}>Últimos ingresos</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 28 }}>
        {ingresos.length === 0 && <p style={{ color: 'var(--muted)' }}>Sin ingresos cargados.</p>}
        {ingresos.slice(0, 20).map((i) => (
          <div key={i.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', opacity: i.anulado ? 0.55 : 1 }}>
            <span>{i.medicoNombre} · {i.fecha}</span>
            <strong>{formatMoneda(i.monto)}</strong>
          </div>
        ))}
      </div>

      <h2 style={{ fontSize: 18, marginBottom: 12 }}>Últimos gastos</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {gastos.length === 0 && <p style={{ color: 'var(--muted)' }}>Sin gastos cargados.</p>}
        {gastos.slice(0, 20).map((g) => (
          <div key={g.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', opacity: g.anulado ? 0.55 : 1 }}>
            <span>{g.categoriaNombre} · {g.fecha}</span>
            <strong>{formatMoneda(g.monto)}</strong>
          </div>
        ))}
      </div>
    </>
  );
}

const TABS = [
  { id: 'caja', label: 'Caja' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function ContadorDashboardPage() {
  const usuario = useSelector((state) => state.auth.usuario);
  const [tab, setTab] = useState('caja');

  return (
    <div>
      <span className="eyebrow">Panel de contador</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, {usuario?.nombreCompleto}</h1>

      <div style={{ display: 'flex', gap: 8, marginTop: 24, marginBottom: 28, flexWrap: 'wrap', borderBottom: '1px solid var(--line)' }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
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

      {tab === 'caja' && <ResumenCajaTab />}
      {tab === 'cuenta' && <CuentaSeguridadTab />}
    </div>
  );
}
