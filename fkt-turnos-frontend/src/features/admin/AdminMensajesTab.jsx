import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMensajesContacto, marcarMensajeLeido } from './adminSlice';

export default function AdminMensajesTab() {
  const dispatch = useDispatch();
  const mensajes = useSelector((state) => state.admin.mensajesContacto);

  useEffect(() => {
    dispatch(fetchMensajesContacto());
  }, [dispatch]);

  if (mensajes.length === 0) {
    return <p style={{ color: 'var(--muted)' }}>Todavía no llegaron consultas por alquiler de consultorios.</p>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
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
    </div>
  );
}
