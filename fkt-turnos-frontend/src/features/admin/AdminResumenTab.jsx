import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import StatCard from '../../components/ui/StatCard';
import TrendChart from '../../components/ui/TrendChart';
import { fetchResumenAdmin, fetchTendenciaAdmin } from './adminSlice';
import { formatMoneda } from '../../utils/formato';

export default function AdminResumenTab({ irACola, irAKinesiologia, irACaja }) {
  const dispatch = useDispatch();
  const { resumen, tendencia, error } = useSelector((state) => state.admin);

  useEffect(() => {
    dispatch(fetchResumenAdmin());
    dispatch(fetchTendenciaAdmin(14));
    const intervalo = setInterval(() => dispatch(fetchResumenAdmin()), 30000);
    return () => clearInterval(intervalo);
  }, [dispatch]);

  if (error) return <div className="alert alert-error" role="alert">{error}</div>;
  if (!resumen) return <p style={{ color: 'var(--muted)' }}>Cargando…</p>;

  return (
    <div>
      <p style={{ color: 'var(--muted)', marginBottom: 20 }}>Hoy, {resumen.fecha}</p>
      <div className="stagger" style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 12 }}>
        <StatCard etiqueta="Turnos reservados hoy" valor={resumen.turnosReservadosHoy} />
        <StatCard etiqueta="Turnos disponibles hoy" valor={resumen.turnosDisponiblesHoy} />
        <StatCard etiqueta="Pacientes esperando" valor={resumen.pacientesEnEspera} destacado={resumen.pacientesEnEspera > 0} onClick={irACola} />
        <StatCard etiqueta="Planes de kinesiología por validar" valor={resumen.planesKinesiologiaPendientes} destacado={resumen.planesKinesiologiaPendientes > 0} onClick={irAKinesiologia} />
        <StatCard etiqueta="Ingresos de hoy" valor={formatMoneda(resumen.ingresosHoy)} onClick={irACaja} />
        <StatCard etiqueta="Caja física" valor={formatMoneda(resumen.cajaFisica)} onClick={irACaja} />
      </div>
      <p style={{ fontSize: 12.5, color: 'var(--muted)', marginBottom: 28 }}>Se actualiza solo cada 30 segundos. Hacé clic en una tarjeta para ir directo a esa sección.</p>

      {tendencia.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ flex: '1 1 320px' }}>
            <TrendChart titulo="Turnos reservados por día (últimos 14 días)" datos={tendencia} valorDe={(d) => d.turnosReservados} />
          </div>
          <div style={{ flex: '1 1 320px' }}>
            <TrendChart titulo="Ingresos por día (últimos 14 días)" datos={tendencia} valorDe={(d) => d.ingresos} formatValor={formatMoneda} color="var(--teal-dark)" />
          </div>
        </div>
      )}
    </div>
  );
}
