import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchGastosContador } from './contadorSlice';
import EstadoVacio from '../../components/EstadoVacio';

function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

export default function ContadorGastosTab() {
  const dispatch = useDispatch();
  const { gastos, gastosCargando, error } = useSelector((state) => state.contador);

  useEffect(() => { dispatch(fetchGastosContador()); }, [dispatch]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (gastosCargando && gastos.length === 0) return <p style={{ color: 'var(--muted)' }}>Cargando gastos…</p>;

  if (gastos.length === 0) {
    return <EstadoVacio titulo="Sin gastos cargados" subtitulo="Los gastos operativos del consultorio van a aparecer acá." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {gastos.map((g) => (
        <div key={g.id} className="card" style={{ padding: '12px 18px', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8, opacity: g.anulado ? 0.55 : 1 }}>
          <span>
            {g.categoriaNombre} · {g.fecha}
            {g.descripcion && <span style={{ color: 'var(--muted)' }}> — {g.descripcion}</span>}
            {g.anulado && <span style={{ color: 'var(--muted)', fontSize: 12.5 }}> (anulado{g.motivoAnulacion ? `: ${g.motivoAnulacion}` : ''})</span>}
          </span>
          <strong>{formatMoneda(g.monto)}</strong>
        </div>
      ))}
    </div>
  );
}
