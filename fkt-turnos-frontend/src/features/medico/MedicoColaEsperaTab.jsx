import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchColaEsperaMedico } from './medicoSlice';
import MedicoTurnoCard from './MedicoTurnoCard';
import EstadoVacio from '../../components/EstadoVacio';

export default function MedicoColaEsperaTab() {
  const dispatch = useDispatch();
  const { colaEspera, colaEsperaCargando } = useSelector((state) => state.medico);

  useEffect(() => {
    dispatch(fetchColaEsperaMedico());
    const intervalo = setInterval(() => dispatch(fetchColaEsperaMedico()), 10000);
    return () => clearInterval(intervalo);
  }, [dispatch]);

  if (colaEsperaCargando && colaEspera.length === 0) return <p style={{ color: 'var(--muted)' }}>Cargando cola de espera…</p>;

  if (colaEspera.length === 0) {
    return <EstadoVacio titulo="No hay pacientes esperando en este momento" subtitulo="La lista se actualiza sola cada 10 segundos." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {colaEspera.map((t) => <MedicoTurnoCard key={t.id} turno={t} mostrarBotonAtender mostrarEvolucion={false} />)}
    </div>
  );
}
