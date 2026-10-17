import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Star } from 'lucide-react';
import { crearResenaTurno } from './turnosSlice';

const ESTRELLAS = [1, 2, 3, 4, 5];
const TEXTO_PUNTAJE = ['', 'Muy mala', 'Mala', 'Regular', 'Buena', 'Excelente'];

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
    return <p className="resena-gracias">¡Gracias por tu reseña! La publicamos apenas la revisemos.</p>;
  }

  if (!abierto) {
    return (
      <button type="button" className="btn btn-outline resena-abrir" onClick={() => setAbierto(true)}>
        <Star size={17} aria-hidden="true" /> Contanos cómo te fue
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

  const mostrado = hover || puntaje;

  return (
    <form onSubmit={handleSubmit} className="resena">
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      <div className="resena-estrellas" role="radiogroup" aria-label="Puntaje">
        {ESTRELLAS.map((n) => (
          <button
            key={n}
            type="button"
            role="radio"
            aria-label={`${n} de 5 estrellas`}
            aria-checked={puntaje === n}
            onClick={() => setPuntaje(n)}
            onMouseEnter={() => setHover(n)}
            onMouseLeave={() => setHover(0)}
            className={`estrella-btn${mostrado >= n ? ' encendida' : ''}`}
          >
            <Star size={30} aria-hidden="true" />
          </button>
        ))}
        <span className="resena-texto" aria-live="polite">{TEXTO_PUNTAJE[mostrado]}</span>
      </div>
      <div className="field" style={{ margin: '12px 0 0' }}>
        <label htmlFor={`resena-${turnoId}`} className="sr-only">Comentario</label>
        <textarea
          id={`resena-${turnoId}`}
          rows={2}
          placeholder="Contanos cómo fue tu experiencia (opcional)"
          maxLength={500}
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
        />
      </div>
      <div className="resena-acciones">
        <button type="submit" className="btn btn-primary" disabled={puntaje === 0 || resenaEnCurso === turnoId}>
          {resenaEnCurso === turnoId ? 'Enviando…' : 'Enviar reseña'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => setAbierto(false)}>
          Ahora no
        </button>
      </div>
    </form>
  );
}
