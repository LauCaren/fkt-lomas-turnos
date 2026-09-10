import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { LayoutDashboard, ArrowDownToLine, ArrowUpFromLine, Receipt, Settings } from 'lucide-react';
import SidebarNav from '../../components/ui/SidebarNav';
import { TabPanel } from '../../components/ui/TabNav';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import ContadorResumenTab from './ContadorResumenTab';
import ContadorIngresosTab from './ContadorIngresosTab';
import ContadorRetirosTab from './ContadorRetirosTab';
import ContadorGastosTab from './ContadorGastosTab';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';

const GRUPOS_NAV = [
  { items: [{ id: 'resumen', label: 'Resumen', icon: LayoutDashboard }] },
  {
    label: 'Caja',
    items: [
      { id: 'ingresos', label: 'Ingresos', icon: ArrowDownToLine },
      { id: 'retiros', label: 'Retiros', icon: ArrowUpFromLine },
      { id: 'gastos', label: 'Gastos', icon: Receipt },
    ],
  },
  { items: [{ id: 'cuenta', label: 'Mi cuenta', icon: Settings }] },
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
          <div className="alert alert-error" role="alert" style={{ marginTop: 24 }}>
            Por seguridad, la cuenta de contador tiene que activar la verificación en dos pasos antes de poder ver la caja.
          </div>
          <div style={{ marginTop: 20 }}>
            <CuentaSeguridadTab />
          </div>
        </>
      ) : (
        <div className="panel-shell">
          <SidebarNav groups={GRUPOS_NAV} active={tab} onChange={setTab} />

          <div className="panel-content">
            <TabPanel tabKey={tab}>
              {tab === 'resumen' && <ContadorResumenTab />}
              {tab === 'ingresos' && <ContadorIngresosTab />}
              {tab === 'retiros' && <ContadorRetirosTab />}
              {tab === 'gastos' && <ContadorGastosTab />}
              {tab === 'cuenta' && <CuentaSeguridadTab />}
            </TabPanel>
          </div>
        </div>
      )}
    </div>
  );
}
