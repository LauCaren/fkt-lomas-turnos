import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { crearResenaTurno } from './turnosSlice';

const ESTRELLAS = [1, 2, 3, 4, 5];

/** Solo aparece para un turno propio ya ATENDIDO sin reseña todavía (ver TurnoDto.tieneResena) — el backend vuelve a validar todo esto igual, esto es solo la UI. */
export default function DejarResenaTurno({ turnoId }) {
  const dispatch = useDispatch();
  const resenaEnCurso = useSelector((state) => state.turnos.resenaEnCurso);
  const error = useSelector((state) => state.turnos.resenaErrorPorTurno[turnoId]);
  const [abierto, setAbierto] = useState(false);
  const [enviada, setEnviada] = useState(false);
  const [puntaje, setPuntaje] = useState(0);
  const [hover, setHover] = useState(0);
  const [comentario, setComentario] = useState('');

  if (enviada) {
    return <p style={{ fontSize: 13, color: 'var(--teal-dark)', marginTop: 10 }}>¡Gracias por tu reseña! La vamos a publicar apenas la revisemos.</p>;
  }

  if (!abierto) {
    return (
      <button type="button" className="btn btn-outline" style={{ marginTop: 10, padding: '6px 14px', fontSize: 13 }} onClick={() => setAbierto(true)}>
        Dejar una reseña
      </button>
    );
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(crearResenaTurno({ turnoId, puntaje, comentario: comentario.trim() || null }))
      .unwrap()
      .then(() => setEnviada(true))
      .catch(() => {});
  }

  return (
    <form onSubmit={handleSubmit} className="card" style={{ marginTop: 10, padding: 14 }}>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 10, fontSize: 13 }}>{error}</div>}
      <div style={{ display: 'flex', gap: 4 }} role="radiogroup" aria-label="Puntaje">
        {ESTRELLAS.map((n) => (
          <button
            key={n}
            type="button"
            aria-label={`${n} de 5 estrellas`}
            aria-pressed={puntaje === n}
            onClick={() => setPuntaje(n)}
            onMouseEnter={() => setHover(n)}
            onMouseLeave={() => setHover(0)}
            className="estrella-btn"
            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 2, fontSize: 24, lineHeight: 1, color: (hover || puntaje) >= n ? 'var(--amber, #B4740E)' : 'var(--line)' }}
          >
            ★
          </button>
        ))}
      </div>
      <textarea
        rows={2}
        placeholder="Contanos cómo fue tu experiencia (opcional)"
        maxLength={500}
        value={comentario}
        onChange={(e) => setComentario(e.target.value)}
        style={{ marginTop: 10, width: '100%' }}
      />
      <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
        <button type="submit" className="btn btn-primary" style={{ padding: '6px 14px', fontSize: 13 }} disabled={puntaje === 0 || resenaEnCurso === turnoId}>
          {resenaEnCurso === turnoId ? 'Enviando…' : 'Enviar reseña'}
        </button>
        <button type="button" className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setAbierto(false)}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
