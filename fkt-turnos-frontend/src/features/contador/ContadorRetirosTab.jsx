import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchRetirosContador } from './contadorSlice';
import EstadoVacio from '../../components/EstadoVacio';

function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

export default function ContadorRetirosTab() {
  const dispatch = useDispatch();
  const { retiros, retirosCargando, error } = useSelector((state) => state.contador);

  useEffect(() => { dispatch(fetchRetirosContador()); }, [dispatch]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (retirosCargando && retiros.length === 0) return <p style={{ color: 'var(--muted)' }}>Cargando retiros…</p>;

  if (retiros.length === 0) {
    return <EstadoVacio titulo="Sin retiros cargados" subtitulo="Cuando un médico retire su saldo acumulado, va a aparecer acá." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {retiros.map((r) => (
        <div key={r.id} className="card" style={{ padding: '12px 18px', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8, opacity: r.anulado ? 0.55 : 1 }}>
          <span>
            {r.medicoNombre} · {new Date(r.fechaHora).toLocaleDateString('es-AR')}
            {r.anulado && <span style={{ color: 'var(--muted)', fontSize: 12.5 }}> (anulado{r.motivoAnulacion ? `: ${r.motivoAnulacion}` : ''})</span>}
          </span>
          <span style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <span style={{ fontSize: 12.5, color: 'var(--muted)' }}>Entregó {r.entregadoPorNombre}</span>
            <strong>{formatMoneda(r.monto)}</strong>
          </span>
        </div>
      ))}
    </div>
  );
}
