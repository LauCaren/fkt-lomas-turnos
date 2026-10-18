import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { marcarAtendido, crearEvolucion, fetchEvolucionesDePaciente } from './medicoSlice';
import { formatFechaHora } from '../../utils/formato';

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
        {enviando && <span className="spinner" aria-hidden="true" />}
        {enviando ? 'Guardando…' : 'Guardar evolución'}
      </button>
    </form>
  );
}

export default function MedicoTurnoCard({ turno, mostrarBotonAtender, mostrarEvolucion }) {
  const dispatch = useDispatch();
  const evoluciones = useSelector((state) => state.medico.evolucionesPorPaciente[turno.pacienteId]) || [];
  const [verHistorial, setVerHistorial] = useState(false);

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
              {turno.tieneEvolucion && <div style={{ color: 'var(--teal-dark)', marginTop: 2, fontSize: 14 }}>Evolución cargada</div>}
            </div>
          )}
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <span className={`badge badge-${turno.estado === 'DISPONIBLE' ? 'disponible' : 'reservado'}`}>
            {turno.estadoAsistencia === 'LLEGO' ? 'En sala de espera' : turno.estadoAsistencia === 'ATENDIDO' ? 'Atendido' : turno.estadoAsistencia === 'AUSENTE' ? 'Ausente' : turno.estado === 'RESERVADO' ? 'Reservado' : 'Disponible'}
          </span>
          {mostrarBotonAtender && <BotonAtender turnoId={turno.id} />}
          {turno.pacienteId && (
            <button className="btn btn-outline btn-sm" onClick={toggleHistorial}>
              {verHistorial ? 'Ocultar historial' : 'Ver historial'}
            </button>
          )}
        </div>
      </div>

      {mostrarEvolucion && turno.pacienteId && <FormularioEvolucion turno={turno} onListo={() => dispatch(fetchEvolucionesDePaciente(turno.pacienteId))} />}

      {verHistorial && (
        <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)' }}>
          {evoluciones.length === 0 && <p style={{ fontSize: 14, color: 'var(--muted)' }}>No cargaste evoluciones para este paciente todavía.</p>}
          {evoluciones.length > 0 && (
            <div className="evolucion-timeline">
              {evoluciones.map((e) => (
                <div key={e.id} className="evolucion-item">
                  <span className="evolucion-dot" aria-hidden="true" />
                  <div className="evolucion-fecha">{formatFechaHora(e.createdAt)}</div>
                  <div className="evolucion-texto">{e.texto}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function BotonAtender({ turnoId }) {
  const dispatch = useDispatch();
  const [cargando, setCargando] = useState(false);
  return (
    <button
      className="btn btn-primary btn-sm"
      disabled={cargando}
      onClick={() => { setCargando(true); dispatch(marcarAtendido(turnoId)).finally(() => setCargando(false)); }}
    >
      {cargando && <span className="spinner" aria-hidden="true" />}
      Atender
    </button>
  );
}
