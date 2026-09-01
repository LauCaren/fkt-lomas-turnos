import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchAgendaHoy, fetchColaEsperaMedico, marcarAtendido, crearEvolucion,
  fetchEvolucionesDePaciente, fetchSaldoMedico, limpiarMensajesMedico,
} from './medicoSlice';
import CuentaSeguridadTab from '../../components/CuentaSeguridadTab';

function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

function FormularioEvolucion({ turno, onListo }) {
  const dispatch = useDispatch();
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    if (!texto.trim()) return;
    setEnviando(true);
    dispatch(crearEvolucion({ turnoId: turno.id, texto: texto.trim() })).unwrap()
      .then(() => { setTexto(''); onListo(); })
      .finally(() => setEnviando(false));
  }

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
      <textarea
        rows={3}
        placeholder="Evolución de la consulta…"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        required
      />
      <button type="submit" className="btn btn-outline" style={{ alignSelf: 'flex-start' }} disabled={enviando}>
        {enviando ? 'Guardando…' : 'Guardar evolución'}
      </button>
    </form>
  );
}

function FilaTurno({ turno, mostrarBotonAtender, mostrarEvolucion }) {
  const dispatch = useDispatch();
  const evoluciones = useSelector((state) => state.medico.evolucionesPorPaciente[turno.pacienteId]) || [];
  const [verHistorial, setVerHistorial] = useState(false);
  const [cargandoEvolucion, setCargandoEvolucion] = useState(false);

  function toggleHistorial() {
    if (!verHistorial) dispatch(fetchEvolucionesDePaciente(turno.pacienteId));
    setVerHistorial((v) => !v);
  }

  return (
    <div className="card" style={{ padding: '16px 20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ fontWeight: 600, fontSize: 15 }}>{turno.horaInicio.slice(0, 5)} a {turno.horaFin.slice(0, 5)}</div>
          {turno.pacienteNombre && (
            <div style={{ marginTop: 6, fontSize: 14 }}>
              <strong>{turno.pacienteNombre}</strong> · DNI {turno.pacienteDni}
              {turno.motivoConsulta && <div style={{ color: 'var(--muted)', marginTop: 2 }}>Motivo: {turno.motivoConsulta}</div>}
              {turno.tieneEvolucion && <div style={{ color: 'var(--teal-dark)', marginTop: 2, fontSize: 13 }}>Evolución cargada</div>}
            </div>
          )}
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <span className={`badge badge-${turno.estado === 'DISPONIBLE' ? 'disponible' : 'reservado'}`}>
            {turno.estadoAsistencia === 'LLEGO' ? 'En sala de espera' : turno.estadoAsistencia === 'ATENDIDO' ? 'Atendido' : turno.estadoAsistencia === 'AUSENTE' ? 'Ausente' : turno.estado === 'RESERVADO' ? 'Reservado' : 'Disponible'}
          </span>
          {mostrarBotonAtender && (
            <button
              className="btn btn-primary"
              style={{ padding: '6px 14px', fontSize: 13 }}
              disabled={cargandoEvolucion}
              onClick={() => { setCargandoEvolucion(true); dispatch(marcarAtendido(turno.id)).finally(() => setCargandoEvolucion(false)); }}
            >
              Atender
            </button>
          )}
          {turno.pacienteId && (
            <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={toggleHistorial}>
              {verHistorial ? 'Ocultar historial' : 'Ver historial'}
            </button>
          )}
        </div>
      </div>

      {mostrarEvolucion && turno.pacienteId && <FormularioEvolucion turno={turno} onListo={() => dispatch(fetchEvolucionesDePaciente(turno.pacienteId))} />}

      {verHistorial && (
        <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)' }}>
          {evoluciones.length === 0 && <p style={{ fontSize: 13, color: 'var(--muted)' }}>No cargaste evoluciones para este paciente todavía.</p>}
          {evoluciones.map((e) => (
            <div key={e.id} style={{ fontSize: 13.5, marginBottom: 8 }}>
              <div style={{ color: 'var(--muted)' }}>{new Date(e.createdAt).toLocaleString('es-AR')}</div>
              <div>{e.texto}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function AgendaHoyTab() {
  const dispatch = useDispatch();
  const { agendaHoy, agendaCargando } = useSelector((state) => state.medico);

  useEffect(() => { dispatch(fetchAgendaHoy()); }, [dispatch]);

  const reservados = agendaHoy.filter((t) => t.estado === 'RESERVADO');

  return (
    <div>
      {agendaCargando && <p style={{ color: 'var(--muted)' }}>Cargando agenda…</p>}
      {!agendaCargando && reservados.length === 0 && <p style={{ color: 'var(--muted)' }}>No tenés turnos reservados para hoy.</p>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {reservados.map((t) => <FilaTurno key={t.id} turno={t} mostrarBotonAtender={false} mostrarEvolucion={t.estadoAsistencia === 'ATENDIDO' && !t.tieneEvolucion} />)}
      </div>
    </div>
  );
}

function ColaEsperaTab() {
  const dispatch = useDispatch();
  const { colaEspera, colaEsperaCargando } = useSelector((state) => state.medico);

  useEffect(() => {
    dispatch(fetchColaEsperaMedico());
    const intervalo = setInterval(() => dispatch(fetchColaEsperaMedico()), 10000);
    return () => clearInterval(intervalo);
  }, [dispatch]);

  return (
    <div>
      {colaEsperaCargando && <p style={{ color: 'var(--muted)' }}>Cargando cola de espera…</p>}
      {!colaEsperaCargando && colaEspera.length === 0 && <p style={{ color: 'var(--muted)' }}>No hay pacientes esperando en este momento.</p>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {colaEspera.map((t) => <FilaTurno key={t.id} turno={t} mostrarBotonAtender mostrarEvolucion={false} />)}
      </div>
    </div>
  );
}

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

      {error && <div className="alert alert-error" style={{ marginTop: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" style={{ marginTop: 20 }}>{mensajeExito}</div>}

      <div style={{ display: 'flex', gap: 8, marginTop: 24, marginBottom: 28, flexWrap: 'wrap', borderBottom: '1px solid var(--line)' }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => { setTab(t.id); dispatch(limpiarMensajesMedico()); }}
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

      {tab === 'agenda' && <AgendaHoyTab />}
      {tab === 'cola-espera' && <ColaEsperaTab />}
      {tab === 'cuenta' && <CuentaSeguridadTab />}
    </div>
  );
}
