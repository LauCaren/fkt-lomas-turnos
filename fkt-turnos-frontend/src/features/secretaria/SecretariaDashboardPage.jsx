import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  CalendarDays, CalendarPlus, ClipboardList, Users, Search, BellRing, Building2, FileText, Star,
  UserMinus, Settings,
} from 'lucide-react';
import SidebarNav from '../../components/ui/SidebarNav';
import CommandPalette from '../../components/ui/CommandPalette';
import { TabPanel } from '../../components/ui/TabNav';
import { limpiarMensajes, setPacienteAAbrir, fetchResumenAdmin } from '../admin/adminSlice';
import axiosClient from '../../api/axiosClient';
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
      { id: 'buscar-paciente', label: 'Buscar paciente', icon: Search },
      { id: 'cola-espera', label: 'Cola de espera', icon: Users },
      { id: 'turnos-agendados', label: 'Buscar turnos por rango', icon: ClipboardList },
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

// Mismo criterio que AdminDashboardPage: qué campo del resumen alimenta el numerito de cada
// sección — acá no hay una pestaña "Resumen" que lo pollee sola, así que se pide una vez al
// entrar (ver el useEffect de abajo), sin repetirlo cada 30s.
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

export default function SecretariaDashboardPage() {
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const perfil = useSelector((state) => state.cuenta.perfil);
  const resumen = useSelector((state) => state.admin.resumen);
  const [tab, setTab] = useState('agenda');
  const gruposNav = useMemo(() => conBadges(GRUPOS_NAV, resumen), [resumen]);

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);
  useEffect(() => { dispatch(fetchResumenAdmin()); }, [dispatch]);

  // Mismo gate que en AdminDashboardPage (esta cuenta pega a los mismos endpoints /api/admin/**):
  // el backend ya bloquea el acceso hasta activar el 2FA, esto es solo para que la experiencia
  // sea clara. Usa el /api/mi-cuenta genérico (CuentaSeguridadTab): mientras el 2FA está
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
              {tab === 'agenda' && <AdminAgendaTab />}
              {tab === 'turnos-agendados' && <AdminTurnosAgendadosTab />}
              {tab === 'cola-espera' && <AdminColaEsperaTab />}
              {tab === 'buscar-paciente' && <AdminBusquedaPacienteTab irAAgenda={() => setTab('agenda')} />}
              {tab === 'recordatorios' && <AdminRecordatoriosTab />}
              {tab === 'mensajes' && <AdminMensajesTab />}
              {tab === 'postulaciones' && <AdminPostulacionesTab />}
              {tab === 'solicitudes-baja' && <AdminSolicitudesBajaTab />}
              {tab === 'resenas' && <AdminResenasTab />}
              {tab === 'cuenta' && <CuentaSeguridadTab />}
            </TabPanel>
          </div>
        </div>
        </>
      )}
    </div>
  );
}
