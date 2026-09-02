import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchResumenAdmin } from './adminSlice';
import { formatMoneda } from '../../utils/formato';

function Tarjeta({ etiqueta, valor, destacado }) {
  return (
    <div className="card" style={{ minWidth: 180, flex: '1 1 180px' }}>
      <div style={{ fontSize: 13, color: 'var(--muted)' }}>{etiqueta}</div>
      <div style={{ fontSize: 26, fontWeight: 700, color: destacado ? 'var(--teal-dark)' : 'inherit', marginTop: 4 }}>{valor}</div>
    </div>
  );
}

export default function AdminResumenTab({ irACola, irAKinesiologia, irACaja }) {
  const dispatch = useDispatch();
  const { resumen, error } = useSelector((state) => state.admin);

  useEffect(() => {
    dispatch(fetchResumenAdmin());
    const intervalo = setInterval(() => dispatch(fetchResumenAdmin()), 30000);
    return () => clearInterval(intervalo);
  }, [dispatch]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!resumen) return <p style={{ color: 'var(--muted)' }}>Cargando…</p>;

  return (
    <div>
      <p style={{ color: 'var(--muted)', marginBottom: 20 }}>Hoy, {resumen.fecha}</p>
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 12 }}>
        <Tarjeta etiqueta="Turnos reservados hoy" valor={resumen.turnosReservadosHoy} />
        <Tarjeta etiqueta="Turnos disponibles hoy" valor={resumen.turnosDisponiblesHoy} />
        <button type="button" onClick={irACola} style={{ all: 'unset', cursor: 'pointer', flex: '1 1 180px' }}>
          <Tarjeta etiqueta="Pacientes esperando" valor={resumen.pacientesEnEspera} destacado={resumen.pacientesEnEspera > 0} />
        </button>
        <button type="button" onClick={irAKinesiologia} style={{ all: 'unset', cursor: 'pointer', flex: '1 1 180px' }}>
          <Tarjeta etiqueta="Planes de kinesiología por validar" valor={resumen.planesKinesiologiaPendientes} destacado={resumen.planesKinesiologiaPendientes > 0} />
        </button>
        <button type="button" onClick={irACaja} style={{ all: 'unset', cursor: 'pointer', flex: '1 1 180px' }}>
          <Tarjeta etiqueta="Ingresos de hoy" valor={formatMoneda(resumen.ingresosHoy)} />
        </button>
        <button type="button" onClick={irACaja} style={{ all: 'unset', cursor: 'pointer', flex: '1 1 180px' }}>
          <Tarjeta etiqueta="Caja física" valor={formatMoneda(resumen.cajaFisica)} />
        </button>
      </div>
      <p style={{ fontSize: 12.5, color: 'var(--muted)' }}>Se actualiza solo cada 30 segundos. Hacé clic en una tarjeta para ir directo a esa sección.</p>
    </div>
  );
}
