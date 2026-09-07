import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import TabNav, { TabPanel } from '../../components/ui/TabNav';
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
import AdminCuentaTab from './AdminCuentaTab';
import AdminCajaTab from '../caja/AdminCajaTab';
import AdminKinesiologiaTab from '../kinesiologia/AdminKinesiologiaTab';
import AdminReportesTab from './AdminReportesTab';
import AdminBusquedaPacienteTab from './AdminBusquedaPacienteTab';
import AdminSecretariasTab from './AdminSecretariasTab';
import AdminAuditoriaTab from './AdminAuditoriaTab';
import AdminSolicitudesBajaTab from './AdminSolicitudesBajaTab';

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
  { id: 'secretarias', label: 'Cuentas de secretaria' },
  { id: 'solicitudes-baja', label: 'Pedidos de baja' },
  { id: 'auditoria', label: 'Auditoría' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function AdminDashboardPage() {
  const dispatch = useDispatch();
  const perfil = useSelector((state) => state.cuenta.perfil);
  const [tab, setTab] = useState('resumen');

  useEffect(() => { dispatch(fetchMiPerfil()); }, [dispatch]);

  // El backend ya bloquea /api/admin/** hasta que esta cuenta active el 2FA (escribe sobre
  // historia clínica y caja física, ya no es opcional para ADMIN/SECRETARIA) — este gate del
  // lado del cliente es solo para que la experiencia sea clara en vez de una pantalla llena
  // de 403. Usa el /api/mi-cuenta genérico (CuentaSeguridadTab), no /api/admin/mfa/**: mientras
  // el 2FA está pendiente esta cuenta tampoco tiene el rol para llegar a ese último endpoint.
  const mfaPendiente = perfil && !perfil.mfaHabilitado;

  return (
    <div>
      <span className="eyebrow">Panel de administración</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, Fernando</h1>

      {perfil === null ? (
        <p style={{ color: 'var(--muted)', marginTop: 24 }}>Cargando…</p>
      ) : mfaPendiente ? (
        <>
          <div className="alert alert-error" style={{ marginTop: 24 }}>
            Por seguridad, la cuenta de administración tiene que activar la verificación en dos pasos antes de poder usar el panel.
          </div>
          <div style={{ marginTop: 20 }}>
            <CuentaSeguridadTab />
          </div>
        </>
      ) : (
        <>
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
            {tab === 'secretarias' && <AdminSecretariasTab />}
            {tab === 'solicitudes-baja' && <AdminSolicitudesBajaTab />}
            {tab === 'auditoria' && <AdminAuditoriaTab />}
            {tab === 'cuenta' && <AdminCuentaTab />}
          </TabPanel>
        </>
      )}
    </div>
  );
}
