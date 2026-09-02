import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchColaEsperaMedico } from './medicoSlice';
import MedicoTurnoCard from './MedicoTurnoCard';
import EstadoVacio from '../../components/EstadoVacio';

export default function MedicoColaEsperaTab() {
  const dispatch = useDispatch();
  const { colaEspera, colaEsperaCargando, turnoParaEvolucion } = useSelector((state) => state.medico);

  useEffect(() => {
    dispatch(fetchColaEsperaMedico());
    const intervalo = setInterval(() => dispatch(fetchColaEsperaMedico()), 10000);
    return () => clearInterval(intervalo);
  }, [dispatch]);

  if (colaEsperaCargando && colaEspera.length === 0 && !turnoParaEvolucion) return <p style={{ color: 'var(--muted)' }}>Cargando cola de espera…</p>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {turnoParaEvolucion && (
        <div>
          <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 6 }}>Acabás de atender a este paciente — cargá la evolución:</p>
          <MedicoTurnoCard turno={turnoParaEvolucion} mostrarBotonAtender={false} mostrarEvolucion />
        </div>
      )}
      {colaEspera.length === 0 && !turnoParaEvolucion && (
        <EstadoVacio titulo="No hay pacientes esperando en este momento" subtitulo="La lista se actualiza sola cada 10 segundos." />
      )}
      {colaEspera.map((t) => <MedicoTurnoCard key={t.id} turno={t} mostrarBotonAtender mostrarEvolucion={false} />)}
    </div>
  );
}
