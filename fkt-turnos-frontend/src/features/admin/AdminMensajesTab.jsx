import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMensajesContacto, marcarMensajeLeido } from './adminSlice';
import EstadoVacio from '../../components/EstadoVacio';

export default function AdminMensajesTab() {
  const dispatch = useDispatch();
  const mensajes = useSelector((state) => state.admin.mensajesContacto);
  const pagina = useSelector((state) => state.admin.mensajesContactoPagina);
  const hayMas = useSelector((state) => state.admin.mensajesContactoHayMas);

  useEffect(() => {
    dispatch(fetchMensajesContacto());
  }, [dispatch]);

  if (mensajes.length === 0) {
    return <EstadoVacio titulo="Sin consultas" subtitulo="Todavía no llegaron consultas por alquiler de consultorios." />;
  }

  return (
    <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {mensajes.map((m) => (
        <div key={m.id} className="card" style={{ opacity: m.leido ? 0.65 : 1 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div style={{ fontWeight: 600 }}>{m.nombre}</div>
            <div style={{ fontSize: 12, color: 'var(--muted)' }}>{new Date(m.createdAt).toLocaleString('es-AR')}</div>
          </div>
          <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>{m.email} · {m.telefono}</div>
          <p style={{ marginTop: 10 }}>{m.mensaje}</p>
          {!m.leido && (
            <button className="btn btn-outline" style={{ marginTop: 12, padding: '6px 14px', fontSize: 13 }}
              onClick={() => dispatch(marcarMensajeLeido(m.id))}>Marcar como leído</button>
          )}
        </div>
      ))}
      {hayMas && (
        <button className="btn btn-outline" style={{ alignSelf: 'center' }}
          onClick={() => dispatch(fetchMensajesContacto(pagina + 1))}>
          Cargar más
        </button>
      )}
    </div>
  );
}
