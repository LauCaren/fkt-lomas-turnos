import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import TabNav, { TabPanel } from '../../components/ui/TabNav';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
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
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const perfil = useSelector((state) => state.cuenta.perfil);
  const [tab, setTab] = useState('resumen');

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);

  // El backend ya bloquea /api/contador/** hasta que este rol active el 2FA (ve toda la
  // plata del centro, a diferencia de médico/admin donde es opcional) — este gate del lado
  // del cliente es solo para que la experiencia sea clara en vez de un 403 en blanco.
  const mfaPendiente = perfil && !perfil.mfaHabilitado;

  return (
    <div>
      <span className="eyebrow">Panel de contador</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, {usuario?.nombreCompleto}</h1>

      {perfil === null ? (
        <p style={{ color: 'var(--muted)', marginTop: 24 }}>Cargando…</p>
      ) : mfaPendiente ? (
        <>
          <div className="alert alert-error" style={{ marginTop: 24 }}>
            Por seguridad, la cuenta de contador tiene que activar la verificación en dos pasos antes de poder ver la caja.
          </div>
          <div style={{ marginTop: 20 }}>
            <CuentaSeguridadTab />
          </div>
        </>
      ) : (
        <>
          <TabNav tabs={TABS} active={tab} onChange={setTab} />

          <TabPanel tabKey={tab}>
            {tab === 'resumen' && <ContadorResumenTab />}
            {tab === 'ingresos' && <ContadorIngresosTab />}
            {tab === 'retiros' && <ContadorRetirosTab />}
            {tab === 'gastos' && <ContadorGastosTab />}
            {tab === 'cuenta' && <CuentaSeguridadTab />}
          </TabPanel>
        </>
      )}
    </div>
  );
}
