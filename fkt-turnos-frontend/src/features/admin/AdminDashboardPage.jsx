import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  LayoutDashboard, CalendarDays, ClipboardList, Users, Search, Stethoscope, Activity,
  Wallet, BarChart3, BellRing, Building2, FileText, UserCog, UserMinus, Star, History, Settings,
} from 'lucide-react';
import SidebarNav from '../../components/ui/SidebarNav';
import { TabPanel } from '../../components/ui/TabNav';
import { limpiarMensajes } from './adminSlice';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';
import AdminResumenTab from './AdminResumenTab';
import AdminAgendaTab from './AdminAgendaTab';
import AdminTurnosAgendadosTab from './AdminTurnosAgendadosTab';
import AdminMedicosTab from './AdminMedicosTab';
import AdminColaEsperaTab from './AdminColaEsperaTab';
import AdminMensajesTab from './AdminMensajesTab';
import AdminPostulacionesTab from './AdminPostulacionesTab';
import AdminRecordatoriosTab from './AdminRecordatoriosTab';
import AdminCajaTab from '../caja/AdminCajaTab';
import AdminKinesiologiaTab from '../kinesiologia/AdminKinesiologiaTab';
import AdminReportesTab from './AdminReportesTab';
import AdminBusquedaPacienteTab from './AdminBusquedaPacienteTab';
import AdminSecretariasTab from './AdminSecretariasTab';
import AdminAuditoriaTab from './AdminAuditoriaTab';
import AdminSolicitudesBajaTab from './AdminSolicitudesBajaTab';
import AdminResenasTab from './AdminResenasTab';

const GRUPOS_NAV = [
  { items: [{ id: 'resumen', label: 'Resumen', icon: LayoutDashboard }] },
  {
    label: 'Turnos',
    items: [
      { id: 'agenda', label: 'Agenda', icon: CalendarDays },
      { id: 'turnos-agendados', label: 'Ver turnos agendados', icon: ClipboardList },
      { id: 'cola-espera', label: 'Cola de espera', icon: Users },
      { id: 'buscar-paciente', label: 'Buscar paciente', icon: Search },
    ],
  },
  {
    label: 'Clínica',
    items: [
      { id: 'medicos', label: 'Médicos y horarios', icon: Stethoscope },
      { id: 'kinesiologia', label: 'Kinesiología', icon: Activity },
    ],
  },
  {
    label: 'Negocio',
    items: [
      { id: 'caja', label: 'Caja', icon: Wallet },
      { id: 'reportes', label: 'Reportes', icon: BarChart3 },
    ],
  },
  {
    label: 'Comunicación',
    items: [
      { id: 'recordatorios', label: 'Recordatorios', icon: BellRing },
      { id: 'mensajes', label: 'Alquiler de consultorios', icon: Building2 },
      { id: 'postulaciones', label: 'Postulaciones', icon: FileText },
      { id: 'resenas', label: 'Reseñas', icon: Star },
    ],
  },
  {
    label: 'Cuentas y seguridad',
    items: [
      { id: 'secretarias', label: 'Cuentas de secretaria', icon: UserCog },
      { id: 'solicitudes-baja', label: 'Pedidos de baja', icon: UserMinus },
      { id: 'auditoria', label: 'Auditoría', icon: History },
    ],
  },
  { items: [{ id: 'cuenta', label: 'Mi cuenta', icon: Settings }] },
];

export default function AdminDashboardPage() {
  const dispatch = useDispatch();
  const perfil = useSelector((state) => state.cuenta.perfil);
  const [tab, setTab] = useState('resumen');

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);

  // El backend ya bloquea /api/admin/** hasta que esta cuenta active el 2FA (escribe sobre
  // historia clínica y caja física, ya no es opcional para ADMIN/SECRETARIA) — este gate del
  // lado del cliente es solo para que la experiencia sea clara en vez de una pantalla llena
  // de 403. Usa el /api/mi-cuenta genérico (CuentaSeguridadTab): mientras el 2FA está
  // pendiente esta cuenta tampoco tiene el rol para llegar a /api/admin/**.
  const mfaPendiente = perfil && !perfil.mfaHabilitado;

  return (
    <div>
      <div className="page-head">
        <span className="eyebrow">Panel de administración</span>
        <h1>Hola, Fernando</h1>
      </div>

      {perfil === null ? (
        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div className="card"><span className="skeleton skeleton-line" style={{ width: '40%' }} /></div>
          <div className="card"><span className="skeleton skeleton-line" style={{ width: '60%' }} /></div>
        </div>
      ) : mfaPendiente ? (
        <>
          <div className="alert alert-error" role="alert" style={{ marginTop: 24 }}>
            Por seguridad, la cuenta de administración tiene que activar la verificación en dos pasos antes de poder usar el panel.
          </div>
          <div style={{ marginTop: 20 }}>
            <CuentaSeguridadTab />
          </div>
        </>
      ) : (
        <div className="panel-shell">
          <SidebarNav groups={GRUPOS_NAV} active={tab} onChange={(id) => { setTab(id); dispatch(limpiarMensajes()); }} />

          <div className="panel-content">
            <TabPanel tabKey={tab}>
              {tab === 'resumen' && (
                <AdminResumenTab
                  irACola={() => setTab('cola-espera')}
                  irAKinesiologia={() => setTab('kinesiologia')}
                  irACaja={() => setTab('caja')}
                  irATurnosAgendados={() => setTab('turnos-agendados')}
                  irAMensajes={() => setTab('mensajes')}
                  irAPostulaciones={() => setTab('postulaciones')}
                  irASolicitudesBaja={() => setTab('solicitudes-baja')}
                  irAAgenda={() => setTab('agenda')}
                  irABuscarPaciente={() => setTab('buscar-paciente')}
                />
              )}
              {tab === 'agenda' && <AdminAgendaTab />}
              {tab === 'turnos-agendados' && <AdminTurnosAgendadosTab />}
              {tab === 'cola-espera' && <AdminColaEsperaTab />}
              {tab === 'buscar-paciente' && <AdminBusquedaPacienteTab />}
              {tab === 'medicos' && <AdminMedicosTab />}
              {tab === 'kinesiologia' && <AdminKinesiologiaTab />}
              {tab === 'caja' && <AdminCajaTab />}
              {tab === 'reportes' && <AdminReportesTab />}
              {tab === 'recordatorios' && <AdminRecordatoriosTab />}
              {tab === 'mensajes' && <AdminMensajesTab />}
              {tab === 'postulaciones' && <AdminPostulacionesTab />}
              {tab === 'secretarias' && <AdminSecretariasTab />}
              {tab === 'solicitudes-baja' && <AdminSolicitudesBajaTab />}
              {tab === 'resenas' && <AdminResenasTab />}
              {tab === 'auditoria' && <AdminAuditoriaTab />}
              {tab === 'cuenta' && <CuentaSeguridadTab />}
            </TabPanel>
          </div>
        </div>
      )}
    </div>
  );
}
