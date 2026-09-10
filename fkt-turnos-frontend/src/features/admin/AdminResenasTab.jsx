import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchResenas, aprobarResena, rechazarResena } from './adminSlice';
import EstadoVacio from '../../components/EstadoVacio';

const ESTADOS = [
  { value: 'PENDIENTE', label: 'Pendientes' },
  { value: '', label: 'Todas' },
  { value: 'APROBADA', label: 'Aprobadas' },
  { value: 'RECHAZADA', label: 'Rechazadas' },
];

const BADGE_ESTADO = { PENDIENTE: 'badge-pendiente', APROBADA: 'badge-disponible', RECHAZADA: 'badge-reservado' };

function Estrellas({ puntaje }) {
  return <span aria-label={`${puntaje} de 5 estrellas`} style={{ color: 'var(--amber)', letterSpacing: 1 }}>{'★'.repeat(puntaje)}{'☆'.repeat(5 - puntaje)}</span>;
}

function FilaResena({ resena }) {
  const dispatch = useDispatch();
  const [rechazando, setRechazando] = useState(false);
  const [motivo, setMotivo] = useState('');

  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <div style={{ fontWeight: 600 }}>{resena.pacienteNombre} — {resena.medicoNombre}</div>
          <div style={{ marginTop: 4 }}><Estrellas puntaje={resena.puntaje} /></div>
          {resena.comentario && <div style={{ fontSize: 13.5, marginTop: 6 }}>{resena.comentario}</div>}
        </div>
        <span className={`badge ${BADGE_ESTADO[resena.estado] || 'badge-cancelado'}`}>{resena.estado}</span>
      </div>

      {resena.estado === 'PENDIENTE' && (
        <div style={{ marginTop: 12 }}>
          {!rechazando ? (
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-primary" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => dispatch(aprobarResena(resena.id))}>
                Aprobar
              </button>
              <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setRechazando(true)}>
                Rechazar
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input placeholder="Motivo del rechazo" value={motivo} onChange={(e) => setMotivo(e.target.value)} style={{ flex: 1, padding: '6px 10px', fontSize: 13 }} />
              <button className="btn btn-primary" style={{ padding: '6px 14px', fontSize: 13 }} disabled={!motivo.trim()}
                onClick={() => { dispatch(rechazarResena({ id: resena.id, motivo: motivo.trim() })); setRechazando(false); }}>
                Confirmar rechazo
              </button>
              <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setRechazando(false)}>Cancelar</button>
            </div>
          )}
        </div>
      )}
      {resena.motivoRechazo && <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 8 }}>Motivo de rechazo: {resena.motivoRechazo}</div>}
    </div>
  );
}

export default function AdminResenasTab() {
  const dispatch = useDispatch();
  const { resenas, error, mensajeExito } = useSelector((state) => state.admin);
  const [estado, setEstado] = useState('PENDIENTE');

  useEffect(() => {
    dispatch(fetchResenas(estado));
  }, [dispatch, estado]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginBottom: 14 }}>{mensajeExito}</div>}

      <p style={{ fontSize: 13.5, color: 'var(--muted)', marginBottom: 16, maxWidth: 640 }}>
        Reseñas que dejaron pacientes sobre un turno que ya tuvieron atendido. Solo las aprobadas se
        muestran en el sitio y en la ficha del médico — nada se publica automáticamente.
      </p>

      <div className="filter-bar">
        <div className="field" style={{ maxWidth: 220 }}>
          <label htmlFor="resenasFiltroEstado">Estado</label>
          <select id="resenasFiltroEstado" value={estado} onChange={(e) => setEstado(e.target.value)}>
            {ESTADOS.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
          </select>
        </div>
      </div>

      {resenas.length === 0 && <EstadoVacio titulo="Sin reseñas" subtitulo="No hay reseñas en este estado." />}
      <div className="stagger">
        {resenas.map((r) => <FilaResena key={r.id} resena={r} />)}
      </div>
    </div>
  );
}
