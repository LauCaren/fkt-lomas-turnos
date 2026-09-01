import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAgendaHoy } from './medicoSlice';
import MedicoTurnoCard from './MedicoTurnoCard';
import EstadoVacio from '../../components/EstadoVacio';

export default function MedicoAgendaTab() {
  const dispatch = useDispatch();
  const { agendaHoy, agendaCargando } = useSelector((state) => state.medico);

  useEffect(() => { dispatch(fetchAgendaHoy()); }, [dispatch]);

  const reservados = agendaHoy.filter((t) => t.estado === 'RESERVADO');

  if (agendaCargando) return <p style={{ color: 'var(--muted)' }}>Cargando agenda…</p>;

  if (reservados.length === 0) {
    return <EstadoVacio titulo="No tenés turnos reservados para hoy" subtitulo="Cuando un paciente reserve con vos, va a aparecer acá." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {reservados.map((t) => (
        <MedicoTurnoCard key={t.id} turno={t} mostrarBotonAtender={false} mostrarEvolucion={t.estadoAsistencia === 'ATENDIDO' && !t.tieneEvolucion} />
      ))}
    </div>
  );
}
