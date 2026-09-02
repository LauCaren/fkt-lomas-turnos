import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import StatCard from '../../components/ui/StatCard';
import { fetchResumenCajaContador } from './contadorSlice';
import EstadoVacio from '../../components/EstadoVacio';
import { formatMoneda } from '../../utils/formato';

export default function ContadorResumenTab() {
  const dispatch = useDispatch();
  const { resumen, resumenCargando, error } = useSelector((state) => state.contador);

  useEffect(() => { dispatch(fetchResumenCajaContador()); }, [dispatch]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (resumenCargando && !resumen) return <p style={{ color: 'var(--muted)' }}>Cargando…</p>;
  if (!resumen) return null;

  return (
    <div>
      <div className="stagger" style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 28 }}>
        <StatCard etiqueta="Caja física" valor={formatMoneda(resumen.cajaFisica)} />
        <StatCard etiqueta="Ingresos totales" valor={formatMoneda(resumen.ingresosTotal)} />
        <StatCard etiqueta="Retiros totales" valor={formatMoneda(resumen.retirosTotal)} />
        <StatCard etiqueta="Gastos totales" valor={formatMoneda(resumen.gastosTotal)} />
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 12 }}>Saldo a favor por médico</h3>
      {resumen.saldosPorMedico.length === 0 ? (
        <EstadoVacio titulo="Todavía no hay movimientos" subtitulo="Cuando se cargue un ingreso o retiro, va a aparecer acá el saldo de cada médico." />
      ) : (
        <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {resumen.saldosPorMedico.map((s) => (
            <div key={s.medicoId} className="card" style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8, padding: '12px 18px' }}>
              <span>{s.medicoNombre}</span>
              <span style={{ display: 'flex', gap: 16, fontSize: 13.5, flexWrap: 'wrap' }}>
                <span style={{ color: 'var(--muted)' }}>Cobrado {formatMoneda(s.ingresosTotal)}</span>
                <span style={{ color: 'var(--muted)' }}>Retirado {formatMoneda(s.retirosTotal)}</span>
                <strong>Saldo {formatMoneda(s.saldo)}</strong>
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
