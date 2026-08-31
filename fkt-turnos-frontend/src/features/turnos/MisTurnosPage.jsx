import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { fetchMisTurnos, cancelarTurno } from './turnosSlice';
import AgregarCalendario from '../../components/AgregarCalendario';

const BADGE_CLASS = {
  DISPONIBLE: 'badge-disponible',
  RESERVADO: 'badge-reservado',
  CANCELADO: 'badge-cancelado',
};

const BADGE_LABEL = {
  DISPONIBLE: 'Disponible',
  RESERVADO: 'Reservado',
  CANCELADO: 'Cancelado',
};

export default function MisTurnosPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { misTurnos } = useSelector((state) => state.turnos);

  useEffect(() => {
    dispatch(fetchMisTurnos());
  }, [dispatch]);

  function esFuturo(turno) {
    return new Date(`${turno.fecha}T${turno.horaInicio}`) >= new Date();
  }

  function handleCancelar(id) {
    if (window.confirm('¿Cancelar este turno? El horario quedará disponible para otro paciente.')) {
      dispatch(cancelarTurno(id));
    }
  }

  function handleReprogramar(turno) {
    navigate('/turnos', { state: { reprogramar: turno } });
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <span className="eyebrow">Mis turnos</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Tus turnos reservados</h1>

      {misTurnos.length === 0 && (
        <div className="card" style={{ marginTop: 28, textAlign: 'center' }}>
          <p style={{ color: 'var(--muted)' }}>Todavía no tenés turnos reservados.</p>
          <Link to="/turnos" className="btn btn-primary" style={{ marginTop: 16 }}>Sacar un turno</Link>
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 28 }}>
        {misTurnos.map((t) => (
          <div key={t.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <div>
              <div style={{ fontWeight: 600 }}>{t.especialidadNombre} con {t.medicoNombre}</div>
              <div style={{ color: 'var(--muted)', fontSize: 14, marginTop: 4 }}>
                {t.fecha} · {t.horaInicio.slice(0, 5)} a {t.horaFin.slice(0, 5)}
              </div>
              {t.motivoConsulta && <div style={{ color: 'var(--muted)', fontSize: 13, marginTop: 4 }}>Motivo: {t.motivoConsulta}</div>}
              {t.estado === 'RESERVADO' && esFuturo(t) && (
                <div style={{ marginTop: 10 }}>
                  <AgregarCalendario turno={t} />
                </div>
              )}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
              <span className={`badge ${BADGE_CLASS[t.estado]}`}>{BADGE_LABEL[t.estado]}</span>
              {t.estado === 'RESERVADO' && esFuturo(t) && (
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => handleReprogramar(t)}>
                    Reprogramar
                  </button>
                  <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => handleCancelar(t.id)}>
                    Cancelar
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
