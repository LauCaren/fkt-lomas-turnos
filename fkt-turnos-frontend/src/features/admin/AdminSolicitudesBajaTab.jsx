import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchSolicitudesBaja, aprobarSolicitudBaja, rechazarSolicitudBaja } from './adminSlice';

const ESTADOS = [
  { value: 'PENDIENTE', label: 'Pendientes' },
  { value: '', label: 'Todos' },
  { value: 'APROBADA', label: 'Aprobados' },
  { value: 'RECHAZADA', label: 'Rechazados' },
];

function FilaSolicitud({ solicitud }) {
  const dispatch = useDispatch();
  const [rechazando, setRechazando] = useState(false);
  const [motivo, setMotivo] = useState('');

  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <div style={{ fontWeight: 600 }}>{solicitud.usuarioNombre}</div>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>{solicitud.usuarioEmail}</div>
          {solicitud.motivo && <div style={{ fontSize: 13.5, marginTop: 6 }}>Motivo: {solicitud.motivo}</div>}
        </div>
        <span className="badge">{solicitud.estado}</span>
      </div>

      {solicitud.estado === 'PENDIENTE' && (
        <div style={{ marginTop: 12 }}>
          {!rechazando ? (
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-primary" style={{ padding: '6px 14px', fontSize: 13 }}
                onClick={() => window.confirm(`¿Aprobar la baja de ${solicitud.usuarioNombre}? Sus datos personales se van a anonimizar de forma permanente.`) && dispatch(aprobarSolicitudBaja(solicitud.id))}>
                Aprobar baja
              </button>
              <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setRechazando(true)}>
                Rechazar
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input placeholder="Motivo del rechazo" value={motivo} onChange={(e) => setMotivo(e.target.value)} style={{ flex: 1, padding: '6px 10px', fontSize: 13 }} />
              <button className="btn btn-primary" style={{ padding: '6px 14px', fontSize: 13 }} disabled={!motivo.trim()}
                onClick={() => { dispatch(rechazarSolicitudBaja({ id: solicitud.id, motivo: motivo.trim() })); setRechazando(false); }}>
                Confirmar rechazo
              </button>
              <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setRechazando(false)}>Cancelar</button>
            </div>
          )}
        </div>
      )}
      {solicitud.motivoRechazo && <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 8 }}>Motivo de rechazo: {solicitud.motivoRechazo}</div>}
    </div>
  );
}

export default function AdminSolicitudesBajaTab() {
  const dispatch = useDispatch();
  const { solicitudesBaja, error, mensajeExito } = useSelector((state) => state.admin);
  const [estado, setEstado] = useState('PENDIENTE');

  useEffect(() => {
    dispatch(fetchSolicitudesBaja(estado));
  }, [dispatch, estado]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginBottom: 14 }}>{mensajeExito}</div>}

      <p style={{ fontSize: 13.5, color: 'var(--muted)', marginBottom: 16, maxWidth: 640 }}>
        Pedidos de baja de cuenta que hicieron los pacientes desde "Mi cuenta". Al aprobar, sus datos
        personales se anonimizan de forma permanente — los turnos y registros ya generados se conservan.
      </p>

      <div className="field" style={{ maxWidth: 220, marginBottom: 20 }}>
        <label>Estado</label>
        <select value={estado} onChange={(e) => setEstado(e.target.value)}>
          {ESTADOS.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
        </select>
      </div>

      {solicitudesBaja.length === 0 && <p style={{ color: 'var(--muted)' }}>No hay pedidos en este estado.</p>}
      {solicitudesBaja.map((s) => <FilaSolicitud key={s.id} solicitud={s} />)}
    </div>
  );
}
