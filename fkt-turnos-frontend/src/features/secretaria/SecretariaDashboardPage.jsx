import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  CalendarDays, ClipboardList, Users, Search, BellRing, Building2, FileText, Star,
  UserMinus, Settings,
} from 'lucide-react';
import SidebarNav from '../../components/ui/SidebarNav';
import { TabPanel } from '../../components/ui/TabNav';
import { limpiarMensajes } from '../admin/adminSlice';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';
import AdminAgendaTab from '../admin/AdminAgendaTab';
import AdminTurnosAgendadosTab from '../admin/AdminTurnosAgendadosTab';
import AdminColaEsperaTab from '../admin/AdminColaEsperaTab';
import AdminBusquedaPacienteTab from '../admin/AdminBusquedaPacienteTab';
import AdminMensajesTab from '../admin/AdminMensajesTab';
import AdminPostulacionesTab from '../admin/AdminPostulacionesTab';
import AdminRecordatoriosTab from '../admin/AdminRecordatoriosTab';
import AdminSolicitudesBajaTab from '../admin/AdminSolicitudesBajaTab';
import AdminResenasTab from '../admin/AdminResenasTab';

// Reusa los mismos componentes que el panel de admin (pegan a /api/admin/**, que esta cuenta
// también puede usar — ver SecurityConfig) en vez de duplicarlos: solo cambia qué subconjunto
// de tabs se muestra. Caja, médicos/especialidades, kinesiología clínica y reportes financieros
// quedan afuera a propósito — no son tarea de secretaria, y el backend los bloquea igual aunque
// alguien arme la URL a mano.
const GRUPOS_NAV = [
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
    label: 'Comunicación',
    items: [
      { id: 'recordatorios', label: 'Recordatorios', icon: BellRing },
      { id: 'mensajes', label: 'Alquiler de consultorios', icon: Building2 },
      { id: 'postulaciones', label: 'Postulaciones', icon: FileText },
      { id: 'resenas', label: 'Reseñas', icon: Star },
    ],
  },
  { label: 'Cuentas', items: [{ id: 'solicitudes-baja', label: 'Pedidos de baja', icon: UserMinus }] },
  { items: [{ id: 'cuenta', label: 'Mi cuenta', icon: Settings }] },
];

export default function SecretariaDashboardPage() {
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const perfil = useSelector((state) => state.cuenta.perfil);
  const [tab, setTab] = useState('agenda');

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);

  // Mismo gate que en AdminDashboardPage (esta cuenta pega a los mismos endpoints /api/admin/**):
  // el backend ya bloquea el acceso hasta activar el 2FA, esto es solo para que la experiencia
  // sea clara. Usa el /api/mi-cuenta genérico (CuentaSeguridadTab): mientras el 2FA está
  // pendiente esta cuenta tampoco tiene el rol para llegar a /api/admin/**.
  const mfaPendiente = perfil && !perfil.mfaHabilitado;

  return (
    <div>
      <div className="page-head">
        <span className="eyebrow">Panel de secretaría</span>
        <h1>Hola, {usuario?.nombreCompleto || ''}</h1>
      </div>

      {perfil === null ? (
        <div style={{ marginTop: 24, maxWidth: 320 }}>
          <div className="skeleton skeleton-line" style={{ width: '50%' }} />
          <div className="skeleton skeleton-line" />
        </div>
      ) : mfaPendiente ? (
        <>
          <div className="alert alert-error" role="alert" style={{ marginTop: 24 }}>
            Por seguridad, la cuenta de secretaría tiene que activar la verificación en dos pasos antes de poder usar el panel.
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
              {tab === 'agenda' && <AdminAgendaTab />}
              {tab === 'turnos-agendados' && <AdminTurnosAgendadosTab />}
              {tab === 'cola-espera' && <AdminColaEsperaTab />}
              {tab === 'buscar-paciente' && <AdminBusquedaPacienteTab />}
              {tab === 'recordatorios' && <AdminRecordatoriosTab />}
              {tab === 'mensajes' && <AdminMensajesTab />}
              {tab === 'postulaciones' && <AdminPostulacionesTab />}
              {tab === 'solicitudes-baja' && <AdminSolicitudesBajaTab />}
              {tab === 'resenas' && <AdminResenasTab />}
              {tab === 'cuenta' && <CuentaSeguridadTab />}
            </TabPanel>
          </div>
        </div>
      )}
    </div>
  );
}
