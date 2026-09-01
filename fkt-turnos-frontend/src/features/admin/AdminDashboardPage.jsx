import { useState } from 'react';
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

const TABS = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'agenda', label: 'Agenda' },
  { id: 'turnos-agendados', label: 'Ver Turnos Agendados' },
  { id: 'cola-espera', label: 'Cola de espera' },
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
  const [tab, setTab] = useState('resumen');

  return (
    <div>
      <span className="eyebrow">Panel de administración</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, Fernando</h1>

      <div style={{ display: 'flex', gap: 8, marginTop: 24, marginBottom: 28, flexWrap: 'wrap', borderBottom: '1px solid var(--line)' }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              padding: '10px 4px',
              marginRight: 20,
              border: 'none',
              background: 'none',
              fontWeight: 600,
              fontSize: 14.5,
              color: tab === t.id ? 'var(--ink)' : 'var(--muted)',
              borderBottom: tab === t.id ? '2px solid var(--teal)' : '2px solid transparent',
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

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
      {tab === 'medicos' && <AdminMedicosTab />}
      {tab === 'kinesiologia' && <AdminKinesiologiaTab />}
      {tab === 'caja' && <AdminCajaTab />}
      {tab === 'reportes' && <AdminReportesTab />}
      {tab === 'recordatorios' && <AdminRecordatoriosTab />}
      {tab === 'mensajes' && <AdminMensajesTab />}
      {tab === 'postulaciones' && <AdminPostulacionesTab />}
      {tab === 'cuenta' && <AdminCuentaTab />}
    </div>
  );
}
