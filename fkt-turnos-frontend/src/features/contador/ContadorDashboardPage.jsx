import { useState } from 'react';
import { useSelector } from 'react-redux';
import ContadorResumenTab from './ContadorResumenTab';
import ContadorIngresosTab from './ContadorIngresosTab';
import ContadorRetirosTab from './ContadorRetirosTab';
import ContadorGastosTab from './ContadorGastosTab';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';

const TABS = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'ingresos', label: 'Ingresos' },
  { id: 'retiros', label: 'Retiros' },
  { id: 'gastos', label: 'Gastos' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function ContadorDashboardPage() {
  const usuario = useSelector((state) => state.auth.usuario);
  const [tab, setTab] = useState('resumen');

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

      {tab === 'resumen' && <ContadorResumenTab />}
      {tab === 'ingresos' && <ContadorIngresosTab />}
      {tab === 'retiros' && <ContadorRetirosTab />}
      {tab === 'gastos' && <ContadorGastosTab />}
      {tab === 'cuenta' && <CuentaSeguridadTab />}
    </div>
  );
}
