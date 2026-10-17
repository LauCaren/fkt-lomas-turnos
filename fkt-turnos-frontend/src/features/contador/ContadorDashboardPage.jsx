import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { LayoutDashboard, ArrowDownToLine, ArrowUpFromLine, Receipt, Settings } from 'lucide-react';
import SidebarNav from '../../components/ui/SidebarNav';
import CommandPalette from '../../components/ui/CommandPalette';
import { TabPanel } from '../../components/ui/TabNav';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import ContadorResumenTab from './ContadorResumenTab';
import ContadorIngresosTab from './ContadorIngresosTab';
import ContadorRetirosTab from './ContadorRetirosTab';
import ContadorGastosTab from './ContadorGastosTab';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';
import PanelSaludo from '../../components/ui/PanelSaludo';

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
      <PanelSaludo nombre={usuario?.nombreCompleto} panel="Panel de contador" />

      {perfil === null ? (
        <div style={{ marginTop: 24, maxWidth: 320 }}>
          <div className="skeleton skeleton-line" style={{ width: '50%' }} />
          <div className="skeleton skeleton-line" />
        </div>
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
        <>
        <CommandPalette groups={GRUPOS_NAV} onNavigate={setTab} />
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
        </>
      )}
    </div>
  );
}
