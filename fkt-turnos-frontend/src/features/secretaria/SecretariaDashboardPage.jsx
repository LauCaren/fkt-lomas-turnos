import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import TabNav, { TabPanel } from '../../components/ui/TabNav';
import { limpiarMensajes } from '../admin/adminSlice';
import AdminAgendaTab from '../admin/AdminAgendaTab';
import AdminTurnosAgendadosTab from '../admin/AdminTurnosAgendadosTab';
import AdminColaEsperaTab from '../admin/AdminColaEsperaTab';
import AdminBusquedaPacienteTab from '../admin/AdminBusquedaPacienteTab';
import AdminMensajesTab from '../admin/AdminMensajesTab';
import AdminPostulacionesTab from '../admin/AdminPostulacionesTab';
import AdminRecordatoriosTab from '../admin/AdminRecordatoriosTab';
import AdminSolicitudesBajaTab from '../admin/AdminSolicitudesBajaTab';
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
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function SecretariaDashboardPage() {
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const [tab, setTab] = useState('agenda');

  return (
    <div>
      <span className="eyebrow">Panel de secretaría</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, {usuario?.nombreCompleto || ''}</h1>

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
        {tab === 'cuenta' && <AdminCuentaTab />}
      </TabPanel>
    </div>
  );
}
