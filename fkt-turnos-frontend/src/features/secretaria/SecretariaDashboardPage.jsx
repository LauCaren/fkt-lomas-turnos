import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import TabNav, { TabPanel } from '../../components/ui/TabNav';
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
import AdminCuentaTab from '../admin/AdminCuentaTab';

// Reusa los mismos componentes que el panel de admin (pegan a /api/admin/**, que esta cuenta
// también puede usar — ver SecurityConfig) en vez de duplicarlos: solo cambia qué subconjunto
// de tabs se muestra. Caja, médicos/especialidades, kinesiología clínica y reportes financieros
// quedan afuera a propósito — no son tarea de secretaria, y el backend los bloquea igual aunque
// alguien arme la URL a mano.
const TABS = [
  { id: 'agenda', label: 'Agenda' },
  { id: 'turnos-agendados', label: 'Ver Turnos Agendados' },
  { id: 'cola-espera', label: 'Cola de espera' },
  { id: 'buscar-paciente', label: 'Buscar paciente' },
  { id: 'recordatorios', label: 'Recordatorios' },
  { id: 'mensajes', label: 'Alquiler de consultorios' },
  { id: 'postulaciones', label: 'Postulaciones' },
  { id: 'solicitudes-baja', label: 'Pedidos de baja' },
  { id: 'resenas', label: 'Reseñas' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function SecretariaDashboardPage() {
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const perfil = useSelector((state) => state.cuenta.perfil);
  const [tab, setTab] = useState('agenda');

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);

  // Mismo gate que en AdminDashboardPage (esta cuenta pega a los mismos endpoints /api/admin/**):
  // el backend ya bloquea el acceso hasta activar el 2FA, esto es solo para que la experiencia
  // sea clara. Usa el /api/mi-cuenta genérico (CuentaSeguridadTab), no AdminCuentaTab: mientras
  // el 2FA está pendiente esta cuenta tampoco tiene el rol para llegar a /api/admin/mfa/**.
  const mfaPendiente = perfil && !perfil.mfaHabilitado;

  return (
    <div>
      <span className="eyebrow">Panel de secretaría</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, {usuario?.nombreCompleto || ''}</h1>

      {perfil === null ? (
        <p style={{ color: 'var(--muted)', marginTop: 24 }}>Cargando…</p>
      ) : mfaPendiente ? (
        <>
          <div className="alert alert-error" style={{ marginTop: 24 }}>
            Por seguridad, la cuenta de secretaría tiene que activar la verificación en dos pasos antes de poder usar el panel.
          </div>
          <div style={{ marginTop: 20 }}>
            <CuentaSeguridadTab />
          </div>
        </>
      ) : (
        <>
          <TabNav tabs={TABS} active={tab} onChange={(id) => { setTab(id); dispatch(limpiarMensajes()); }} />

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
            {tab === 'cuenta' && <AdminCuentaTab />}
          </TabPanel>
        </>
      )}
    </div>
  );
}
