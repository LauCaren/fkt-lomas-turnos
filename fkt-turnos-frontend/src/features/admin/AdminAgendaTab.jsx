import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAgenda, fetchMedicosAdmin, reservarParaPaciente } from './adminSlice';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';

function hoyISO() {
  return new Date().toISOString().slice(0, 10);
}

const PACIENTE_VACIO = { dni: '', nombreCompleto: '', telefono: '', obraSocial: '', email: '', motivoConsulta: '' };

function FormularioReserva({ turno, onCancelar }) {
  const dispatch = useDispatch();
  const [datos, setDatos] = useState(PACIENTE_VACIO);
  const [enviando, setEnviando] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    setEnviando(true);
    dispatch(reservarParaPaciente({ turnoId: turno.id, payload: datos })).unwrap()
      .then(() => onCancelar())
      .catch(() => setEnviando(false));
  }

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)', display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
          <label>DNI</label>
          <input required value={datos.dni} onChange={(e) => setDatos({ ...datos, dni: e.target.value })} placeholder="30123456" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '2 1 220px' }}>
          <label>Nombre completo</label>
          <input required value={datos.nombreCompleto} onChange={(e) => setDatos({ ...datos, nombreCompleto: e.target.value })} />
        </div>
      </div>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
          <label>Teléfono</label>
          <input required value={datos.telefono} onChange={(e) => setDatos({ ...datos, telefono: e.target.value })} placeholder="11 5950-5888" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
          <label>Obra social</label>
          <select required value={datos.obraSocial} onChange={(e) => setDatos({ ...datos, obraSocial: e.target.value })}>
            <option value="">Seleccioná una obra social</option>
            <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
            {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
            <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 200px' }}>
          <label>Email (opcional)</label>
          <input type="email" value={datos.email} onChange={(e) => setDatos({ ...datos, email: e.target.value })} />
        </div>
      </div>
      <div className="field" style={{ marginBottom: 0 }}>
        <label>Motivo de consulta (opcional)</label>
        <input value={datos.motivoConsulta} onChange={(e) => setDatos({ ...datos, motivoConsulta: e.target.value })} />
      </div>
      <p style={{ fontSize: 12.5, color: 'var(--muted)' }}>Si el DNI ya está registrado, se usa esa cuenta y se ignoran los demás datos.</p>
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Reservando…' : 'Confirmar reserva'}</button>
        <button type="button" className="btn btn-outline" onClick={onCancelar}>Cancelar</button>
      </div>
    </form>
  );
}

export default function AdminAgendaTab() {
  const dispatch = useDispatch();
  const { medicos, agenda, agendaCargando, error, mensajeExito } = useSelector((state) => state.admin);
  const [fecha, setFecha] = useState(hoyISO());
  const [medicoId, setMedicoId] = useState('');
  const [reservandoId, setReservandoId] = useState(null);

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchAgenda({ fecha, medicoId }));
  }, [fecha, medicoId, dispatch]);

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 24 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Día</label>
          <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Médico</label>
          <select value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
            <option value="">Todos los médicos</option>
            {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
      </div>

      {agendaCargando && <p style={{ color: 'var(--muted)' }}>Cargando agenda…</p>}
      {!agendaCargando && agenda.length === 0 && (
        <p style={{ color: 'var(--muted)' }}>No hay turnos generados para ese día{medicoId ? ' con ese médico' : ''}.</p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {agenda.map((t) => (
          <div key={t.id} className="card" style={{ padding: '16px 20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 15 }}>
                  {t.horaInicio.slice(0, 5)} a {t.horaFin.slice(0, 5)} · {t.medicoNombre}
                </div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>{t.especialidadNombre}</div>
                {t.estado === 'RESERVADO' && (
                  <div style={{ marginTop: 8, fontSize: 14 }}>
                    <div><strong>{t.pacienteNombre}</strong> · DNI {t.pacienteDni}</div>
                    <div style={{ color: 'var(--muted)' }}>{t.pacienteTelefono} · {t.pacienteObraSocial}</div>
                    {t.motivoConsulta && <div style={{ color: 'var(--muted)', marginTop: 2 }}>Motivo: {t.motivoConsulta}</div>}
                  </div>
                )}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span className={`badge badge-${t.estado === 'DISPONIBLE' ? 'disponible' : 'reservado'}`}>
                  {t.estado === 'DISPONIBLE' ? 'Disponible' : 'Reservado'}
                </span>
                {t.estado === 'DISPONIBLE' && reservandoId !== t.id && (
                  <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setReservandoId(t.id)}>
                    Reservar por teléfono
                  </button>
                )}
              </div>
            </div>
            {reservandoId === t.id && <FormularioReserva turno={t} onCancelar={() => setReservandoId(null)} />}
          </div>
        ))}
      </div>
    </div>
  );
}
