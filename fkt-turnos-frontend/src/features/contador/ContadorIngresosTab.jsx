import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchIngresosContador } from './contadorSlice';
import EstadoVacio from '../../components/EstadoVacio';
import { formatMoneda } from '../../utils/formato';

export default function ContadorIngresosTab() {
  const dispatch = useDispatch();
  const { ingresos, ingresosCargando, error } = useSelector((state) => state.contador);

  useEffect(() => { dispatch(fetchIngresosContador()); }, [dispatch]);

  if (error) return <div className="alert alert-error" role="alert">{error}</div>;
  if (ingresosCargando && ingresos.length === 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="skeleton" style={{ height: 52 }} />
        <div className="skeleton" style={{ height: 52 }} />
        <div className="skeleton" style={{ height: 52 }} />
      </div>
    );
  }

  if (ingresos.length === 0) {
    return <EstadoVacio titulo="Sin ingresos cargados" subtitulo="Los cobros a pacientes que registre la secretaria van a aparecer acá." />;
  }

  return (
    <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {ingresos.map((i) => (
        <div key={i.id} className="card" style={{ padding: '12px 18px', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8, opacity: i.anulado ? 0.55 : 1 }}>
          <span>
            {i.medicoNombre} · {i.fecha}
            {i.anulado && <span style={{ color: 'var(--muted)', fontSize: 13.5 }}> (anulado{i.motivoAnulacion ? `: ${i.motivoAnulacion}` : ''})</span>}
          </span>
          <span style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <span style={{ fontSize: 13.5, color: 'var(--muted)' }}>{i.metodoPago}</span>
            <strong>{formatMoneda(i.monto)}</strong>
          </span>
        </div>
      ))}
    </div>
  );
}
