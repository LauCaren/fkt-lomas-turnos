import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import TabNav, { TabPanel } from '../../components/ui/TabNav';
import { fetchSaldoMedico, limpiarMensajesMedico } from './medicoSlice';
import MedicoAgendaTab from './MedicoAgendaTab';
import MedicoColaEsperaTab from './MedicoColaEsperaTab';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';
import { formatMoneda } from '../../utils/formato';

const TABS = [
  { id: 'agenda', label: 'Mi agenda de hoy' },
  { id: 'cola-espera', label: 'Cola de espera' },
  { id: 'cuenta', label: 'Mi cuenta' },
];

export default function MedicoDashboardPage() {
  const dispatch = useDispatch();
  const usuario = useSelector((state) => state.auth.usuario);
  const { error, mensajeExito, saldo } = useSelector((state) => state.medico);
  const [tab, setTab] = useState('agenda');

  useEffect(() => { dispatch(fetchSaldoMedico()); }, [dispatch]);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <span className="eyebrow">Panel del médico</span>
          <h1 style={{ marginTop: 12, fontSize: 30 }}>Hola, {usuario?.nombreCompleto}</h1>
        </div>
        {saldo && (
          <div className="card" style={{ padding: '10px 18px', textAlign: 'right' }}>
            <div style={{ fontSize: 12.5, color: 'var(--muted)' }}>Tu saldo acumulado</div>
            <div style={{ fontSize: 20, fontWeight: 700 }}>{formatMoneda(saldo.saldo)}</div>
          </div>
        )}
      </div>

      {error && <div className="alert alert-error" role="alert" style={{ marginTop: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginTop: 20 }}>{mensajeExito}</div>}

      <TabNav tabs={TABS} active={tab} onChange={(id) => { setTab(id); dispatch(limpiarMensajesMedico()); }} />

      <TabPanel tabKey={tab}>
        {tab === 'agenda' && <MedicoAgendaTab />}
        {tab === 'cola-espera' && <MedicoColaEsperaTab />}
        {tab === 'cuenta' && <CuentaSeguridadTab />}
      </TabPanel>
    </div>
  );
}
