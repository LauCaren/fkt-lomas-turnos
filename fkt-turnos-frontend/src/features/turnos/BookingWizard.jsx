import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  fetchEspecialidades, fetchMedicos, fetchTurnosDisponibles, reservarTurno, limpiarReserva, limpiarDisponibles,
} from './turnosSlice';

function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export default function BookingWizard() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { especialidades, medicos, disponibles, status, error, reservaExitosa } = useSelector((state) => state.turnos);

  const [especialidadId, setEspecialidadId] = useState('');
  const [medicoId, setMedicoId] = useState('');
  const [fecha, setFecha] = useState(hoyISO());
  const [turnoSeleccionado, setTurnoSeleccionado] = useState(null);
  const [motivoConsulta, setMotivoConsulta] = useState('');

  useEffect(() => {
    dispatch(fetchEspecialidades());
  }, [dispatch]);

  useEffect(() => {
    if (especialidadId) {
      setMedicoId('');
      dispatch(limpiarDisponibles());
      dispatch(fetchMedicos(especialidadId));
    }
  }, [especialidadId, dispatch]);

  useEffect(() => {
    if (medicoId && fecha) {
      dispatch(fetchTurnosDisponibles({ medicoId, fecha }));
    }
  }, [medicoId, fecha, dispatch]);

  function handleReservar() {
    dispatch(reservarTurno({ turnoId: turnoSeleccionado.id, motivoConsulta }));
  }

  if (reservaExitosa) {
    return (
      <div className="card" style={{ maxWidth: 480, margin: '0 auto', textAlign: 'center' }}>
        <div className="alert alert-success">¡Turno reservado con éxito!</div>
        <h2 style={{ fontSize: 22 }}>{reservaExitosa.especialidadNombre} con {reservaExitosa.medicoNombre}</h2>
        <p style={{ marginTop: 10, color: 'var(--muted)' }}>
          {reservaExitosa.fecha} de {reservaExitosa.horaInicio.slice(0, 5)} a {reservaExitosa.horaFin.slice(0, 5)}
        </p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 24 }}>
          <button className="btn btn-outline" onClick={() => { dispatch(limpiarReserva()); setTurnoSeleccionado(null); }}>
            Sacar otro turno
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/turnos/mis-turnos')}>Ver mis turnos</button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <span className="eyebrow">Sacar turno</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Elegí especialidad, médico y horario</h1>

      <div className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error">{error}</div>}

        <div className="field">
          <label htmlFor="especialidad">Especialidad</label>
          <select id="especialidad" value={especialidadId} onChange={(e) => setEspecialidadId(e.target.value)}>
            <option value="">Seleccioná una especialidad</option>
            {especialidades.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
          </select>
        </div>

        {especialidadId && (
          <div className="field">
            <label htmlFor="medico">Médico</label>
            <select id="medico" value={medicoId} onChange={(e) => { setMedicoId(e.target.value); setTurnoSeleccionado(null); }}>
              <option value="">Seleccioná un médico</option>
              {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
            {medicos.length === 0 && <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>No hay médicos cargados para esta especialidad todavía.</p>}
          </div>
        )}

        {medicoId && (
          <div className="field">
            <label htmlFor="fecha">Fecha</label>
            <input id="fecha" type="date" min={hoyISO()} value={fecha} onChange={(e) => { setFecha(e.target.value); setTurnoSeleccionado(null); }} />
          </div>
        )}

        {medicoId && (
          <div className="field">
            <label>Horarios disponibles</label>
            {status === 'loading' && <p style={{ color: 'var(--muted)' }}>Buscando horarios…</p>}
            {status !== 'loading' && disponibles.length === 0 && (
              <p style={{ color: 'var(--muted)' }}>No hay horarios disponibles para esa fecha. Probá con otro día.</p>
            )}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {disponibles.map((t) => (
                <button
                  type="button"
                  key={t.id}
                  onClick={() => setTurnoSeleccionado(t)}
                  className="btn"
                  style={{
                    border: turnoSeleccionado?.id === t.id ? '1.5px solid var(--teal)' : '1.5px solid var(--line)',
                    background: turnoSeleccionado?.id === t.id ? 'var(--teal-tint)' : 'var(--white)',
                    color: 'var(--ink)',
                    padding: '9px 16px',
                    fontSize: 14,
                  }}
                >
                  {t.horaInicio.slice(0, 5)}
                </button>
              ))}
            </div>
          </div>
        )}

        {turnoSeleccionado && (
          <>
            <div className="field">
              <label htmlFor="motivo">Motivo de consulta (opcional)</label>
              <textarea id="motivo" rows={3} value={motivoConsulta} onChange={(e) => setMotivoConsulta(e.target.value)} />
            </div>
            <button className="btn btn-primary btn-block" onClick={handleReservar} disabled={status === 'loading'}>
              {status === 'loading' ? 'Reservando…' : `Confirmar turno de ${turnoSeleccionado.horaInicio.slice(0, 5)}`}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
