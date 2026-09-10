import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import StatCard from '../../components/ui/StatCard';
import TrendChart from '../../components/ui/TrendChart';
import { fetchResumenCajaContador, fetchTendenciaContador } from './contadorSlice';
import EstadoVacio from '../../components/EstadoVacio';
import { formatMoneda } from '../../utils/formato';

export default function ContadorResumenTab() {
  const dispatch = useDispatch();
  const { resumen, resumenCargando, tendencia, error } = useSelector((state) => state.contador);

  useEffect(() => {
    dispatch(fetchResumenCajaContador());
    dispatch(fetchTendenciaContador(14));
  }, [dispatch]);

  if (error) return <div className="alert alert-error" role="alert">{error}</div>;
  if (resumenCargando && !resumen) {
    return (
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div className="skeleton" style={{ height: 78, minWidth: 180, flex: '1 1 180px' }} />
        <div className="skeleton" style={{ height: 78, minWidth: 180, flex: '1 1 180px' }} />
        <div className="skeleton" style={{ height: 78, minWidth: 180, flex: '1 1 180px' }} />
        <div className="skeleton" style={{ height: 78, minWidth: 180, flex: '1 1 180px' }} />
      </div>
    );
  }
  if (!resumen) return null;

  return (
    <div>
      <div className="stagger" style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 28 }}>
        <StatCard etiqueta="Caja física" valorNumerico={resumen.cajaFisica} formatValor={formatMoneda} />
        <StatCard etiqueta="Ingresos totales" valorNumerico={resumen.ingresosTotal} formatValor={formatMoneda} />
        <StatCard etiqueta="Retiros totales" valorNumerico={resumen.retirosTotal} formatValor={formatMoneda} />
        <StatCard etiqueta="Gastos totales" valorNumerico={resumen.gastosTotal} formatValor={formatMoneda} />
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

      {tendencia.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16, marginTop: 28 }}>
          <div style={{ flex: '1 1 320px' }}>
            <TrendChart titulo="Ingresos por día (últimos 14 días)" datos={tendencia} valorDe={(d) => d.ingresos} formatValor={formatMoneda} />
          </div>
          <div style={{ flex: '1 1 320px' }}>
            <TrendChart titulo="Gastos por día (últimos 14 días)" datos={tendencia} valorDe={(d) => d.gastos} formatValor={formatMoneda} color="var(--red)" />
          </div>
        </div>
      )}
    </div>
  );
}
