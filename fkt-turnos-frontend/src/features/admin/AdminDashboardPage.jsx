import { useState } from 'react';
import { useDispatch } from 'react-redux';
import TabNav, { TabPanel } from '../../components/ui/TabNav';
import { limpiarMensajes } from './adminSlice';
import AdminResumenTab from './AdminResumenTab';
import AdminAgendaTab from './AdminAgendaTab';
import AdminTurnosAgendadosTab from './AdminTurnosAgendadosTab';
import AdminMedicosTab from './AdminMedicosTab';
import AdminColaEsperaTab from './AdminColaEsperaTab';
import AdminMensajesTab from './AdminMensajesTab';
import AdminPostulacionesTab from './AdminPostulacionesTab';
import AdminRecordatoriosTab from './AdminRecordatoriosTab';
import AdminCuentaTab from './AdminCuentaTab';
import AdminCajaTab from '../caja/AdminCajaTab';
import AdminKinesiologiaTab from '../kinesiologia/AdminKinesiologiaTab';
import AdminReportesTab from './AdminReportesTab';
import AdminBusquedaPacienteTab from './AdminBusquedaPacienteTab';

const TABS = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'agenda', label: 'Agenda' },
  { id: 'turnos-agendados', label: 'Ver Turnos Agendados' },
  { id: 'cola-espera', label: 'Cola de espera' },
  { id: 'buscar-paciente', label: 'Buscar paciente' },
  { id: 'medicos', label: 'Médicos y horarios' },
  { id: 'kinesiologia', label: 'Kinesiología' },
  { id: 'caja', label: 'Caja' },
  { id: 'reportes', label: 'Reportes' },
  { id: 'recordatorios', label: 'Recordatorios' },
  { id: 'mensajes', label: 'Alquiler de consultorios' },
  { id: 'postulaciones', label: 'Postulaciones' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function AdminDashboardPage() {
  const dispatch = useDispatch();
  const [tab, setTab] = useState('resumen');

  return (
    <div>
      <span className="eyebrow">Panel de administración</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, Fernando</h1>

      <TabNav tabs={TABS} active={tab} onChange={(id) => { setTab(id); dispatch(limpiarMensajes()); }} />

      <TabPanel tabKey={tab}>
        {tab === 'resumen' && (
          <AdminResumenTab
            irACola={() => setTab('cola-espera')}
            irAKinesiologia={() => setTab('kinesiologia')}
            irACaja={() => setTab('caja')}
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
        {tab === 'cuenta' && <AdminCuentaTab />}
      </TabPanel>
    </div>
  );
}
