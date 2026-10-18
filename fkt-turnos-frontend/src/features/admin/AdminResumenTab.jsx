import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { CalendarClock, MessageSquare, FileText, UserMinus } from 'lucide-react';
import StatCard from '../../components/ui/StatCard';
import TrendChart from '../../components/ui/TrendChart';
import { fetchResumenAdmin, fetchTendenciaAdmin } from './adminSlice';
import { formatMoneda } from '../../utils/formato';

export default function AdminResumenTab({
  irACola, irAKinesiologia, irACaja, irATurnosAgendados, irAMensajes, irAPostulaciones, irASolicitudesBaja,
  irAAgenda, irABuscarPaciente,
}) {
  const dispatch = useDispatch();
  const { resumen, tendencia, error } = useSelector((state) => state.admin);
  const [ultimaActualizacion, setUltimaActualizacion] = useState(null);
  const [ahora, setAhora] = useState(() => Date.now());

  useEffect(() => {
    function cargar() {
      dispatch(fetchResumenAdmin()).unwrap().then(() => setUltimaActualizacion(Date.now())).catch(() => {});
    }
    cargar();
    dispatch(fetchTendenciaAdmin(14));
    const intervaloFetch = setInterval(() => {
      // Con la pestaña oculta no se consulta: el resumen se refresca al volver a mirarlo.
      if (!document.hidden) cargar();
    }, 30000);
    return () => clearInterval(intervaloFetch);
  }, [dispatch]);

  // Solo para el texto "actualizado hace Xs" — un reloj de 1s, no dispara ningún fetch.
  useEffect(() => {
    const intervaloReloj = setInterval(() => setAhora(Date.now()), 1000);
    return () => clearInterval(intervaloReloj);
  }, []);

  if (error) return <div className="alert alert-error" role="alert">{error}</div>;
  if (!resumen) {
    return (
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="stat-card">
            <span className="skeleton skeleton-line" style={{ width: '60%' }} />
            <span className="skeleton skeleton-line" style={{ width: '35%', height: 22 }} />
          </div>
        ))}
      </div>
    );
  }

  const segundosDesdeActualizacion = ultimaActualizacion ? Math.floor((ahora - ultimaActualizacion) / 1000) : null;
  const textoActualizacion = segundosDesdeActualizacion === null ? 'Actualizando…'
    : segundosDesdeActualizacion < 2 ? 'Actualizado ahora'
    : `Actualizado hace ${segundosDesdeActualizacion}s`;

  const itemsAtencion = [
    { id: 'sin-confirmar', label: 'Turnos de hoy sin confirmar', count: resumen.turnosSinConfirmarHoy, icon: CalendarClock, onClick: irATurnosAgendados },
    { id: 'mensajes', label: 'Mensajes sin leer', count: resumen.mensajesSinLeer, icon: MessageSquare, onClick: irAMensajes },
    { id: 'postulaciones', label: 'Postulaciones sin revisar', count: resumen.postulacionesSinRevisar, icon: FileText, onClick: irAPostulaciones },
    { id: 'bajas', label: 'Pedidos de baja pendientes', count: resumen.solicitudesBajaPendientes, icon: UserMinus, onClick: irASolicitudesBaja },
  ].filter((item) => item.count > 0);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12, marginBottom: 20 }}>
        <p style={{ color: 'var(--muted)' }}>Hoy, {resumen.fecha}</p>
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" className="btn btn-outline btn-sm" onClick={irAAgenda}>Nuevo turno</button>
          <button type="button" className="btn btn-outline btn-sm" onClick={irABuscarPaciente}>Buscar paciente</button>
        </div>
      </div>
      <div className="stagger" style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 12 }}>
        <StatCard etiqueta="Turnos reservados hoy" valor={resumen.turnosReservadosHoy} />
        <StatCard etiqueta="Turnos disponibles hoy" valor={resumen.turnosDisponiblesHoy} />
        <StatCard etiqueta="Llegaron hoy" valor={resumen.turnosLlegaronHoy} onClick={irAAgenda} />
        <StatCard etiqueta="Ausentes hoy" valor={resumen.turnosAusentesHoy} destacado={resumen.turnosAusentesHoy > 0} onClick={irAAgenda} />
        <StatCard etiqueta="Pacientes esperando" valor={resumen.pacientesEnEspera} destacado={resumen.pacientesEnEspera > 0} onClick={irACola} />
        <StatCard etiqueta="Planes de kinesiología por validar" valor={resumen.planesKinesiologiaPendientes} destacado={resumen.planesKinesiologiaPendientes > 0} onClick={irAKinesiologia} />
        <StatCard etiqueta="Ingresos de hoy" valorNumerico={resumen.ingresosHoy} formatValor={formatMoneda} onClick={irACaja} />
        <StatCard etiqueta="Caja física" valorNumerico={resumen.cajaFisica} formatValor={formatMoneda} onClick={irACaja} />
      </div>
      <p style={{ fontSize: 13.5, color: 'var(--muted)', marginBottom: 28 }}>{textoActualizacion} · se actualiza solo cada 30 segundos. Hacé clic en una tarjeta para ir directo a esa sección.</p>

      {itemsAtencion.length > 0 && (
        <div className="card" style={{ marginBottom: 28 }}>
          <h2 style={{ fontSize: 15, marginBottom: 12 }}>Requiere atención</h2>
          <div className="attention-list stagger">
            {itemsAtencion.map((item) => {
              const Icon = item.icon;
              return (
                <button key={item.id} type="button" className="attention-item" onClick={item.onClick}>
                  <Icon aria-hidden="true" />
                  <span className="attention-item-label">{item.label}</span>
                  <span className="badge badge-pendiente">{item.count}</span>
                </button>
              );
            })}
          </div>
        </div>
      )}

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
