import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  LayoutDashboard, CalendarDays, CalendarPlus, ClipboardList, Users, Search, Stethoscope, Activity,
  Wallet, BarChart3, BellRing, Building2, FileText, UserCog, UserMinus, Star, History, Settings,
} from 'lucide-react';
import SidebarNav from '../../components/ui/SidebarNav';
import CommandPalette from '../../components/ui/CommandPalette';
import { TabPanel } from '../../components/ui/TabNav';
import { limpiarMensajes, setPacienteAAbrir } from './adminSlice';
import axiosClient from '../../api/axiosClient';
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
      { id: 'buscar-paciente', label: 'Buscar paciente', icon: Search },
      { id: 'cola-espera', label: 'Cola de espera', icon: Users },
      { id: 'turnos-agendados', label: 'Buscar turnos por rango', icon: ClipboardList },
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

// Qué campo del resumen (ya polleado cada 30s por AdminResumenTab) alimenta el numerito de
// cada sección del sidebar — sin pollear nada nuevo, solo reutilizando ese mismo dato.
const CAMPO_RESUMEN_POR_ITEM = {
  agenda: 'turnosSinConfirmarHoy',
  'cola-espera': 'pacientesEnEspera',
  mensajes: 'mensajesSinLeer',
  postulaciones: 'postulacionesSinRevisar',
  'solicitudes-baja': 'solicitudesBajaPendientes',
};

function conBadges(groups, resumen) {
  if (!resumen) return groups;
  return groups.map((g) => ({
    ...g,
    items: g.items.map((item) => {
      const campo = CAMPO_RESUMEN_POR_ITEM[item.id];
      return campo ? { ...item, badge: resumen[campo] } : item;
    }),
  }));
}

export default function AdminDashboardPage() {
  const dispatch = useDispatch();
  const perfil = useSelector((state) => state.cuenta.perfil);
  const resumen = useSelector((state) => state.admin.resumen);
  const [tab, setTab] = useState('resumen');
  const gruposNav = useMemo(() => conBadges(GRUPOS_NAV, resumen), [resumen]);

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);

  // El backend ya bloquea /api/admin/** hasta que esta cuenta active el 2FA (escribe sobre
  // historia clínica y caja física, ya no es opcional para ADMIN/SECRETARIA) — este gate del
  // lado del cliente es solo para que la experiencia sea clara en vez de una pantalla llena
  // de 403. Usa el /api/mi-cuenta genérico (CuentaSeguridadTab): mientras el 2FA está
  // pendiente esta cuenta tampoco tiene el rol para llegar a /api/admin/**.
  const mfaPendiente = perfil && !perfil.mfaHabilitado;

  const irATab = useCallback((id) => {
    setTab(id);
    dispatch(limpiarMensajes());
  }, [dispatch]);

  function buscarPacientesGlobal(q) {
    return axiosClient.get('/api/admin/pacientes/buscar', { params: { q } }).then((r) => r.data);
  }

  function abrirPaciente(paciente) {
    dispatch(setPacienteAAbrir(paciente.id));
    irATab('buscar-paciente');
  }

  const comandosPalette = [
    { id: 'cmd-nuevo-turno', label: 'Nuevo turno', icon: CalendarPlus, run: () => irATab('agenda') },
  ];

  // Atajos de teclado a nivel de todo el panel (punto 10): se ignoran si el foco está en un
  // campo de texto/select, igual que las flechas de la agenda, para no interferir con el tipeo.
  // Ninguno ataca algo destructivo — solo navegan.
  useEffect(() => {
    function handleKeyDown(e) {
      if (mfaPendiente || perfil === null) return;
      const tag = document.activeElement?.tagName;
      if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
      const key = e.key.toLowerCase();
      if (key === 'n' || key === 'a') irATab('agenda');
      else if (key === 'p') irATab('buscar-paciente');
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mfaPendiente, perfil, irATab]);

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
        <>
        <CommandPalette
          groups={GRUPOS_NAV}
          onNavigate={irATab}
          comandos={comandosPalette}
          onBuscarPacientes={buscarPacientesGlobal}
          onSeleccionarPaciente={abrirPaciente}
        />
        <div className="panel-shell">
          <SidebarNav groups={gruposNav} active={tab} onChange={irATab} />

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
              {tab === 'buscar-paciente' && <AdminBusquedaPacienteTab irAAgenda={() => setTab('agenda')} />}
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
        </>
      )}
    </div>
  );
}
