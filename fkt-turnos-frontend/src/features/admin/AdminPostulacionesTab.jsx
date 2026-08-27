import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import axiosClient from '../../api/axiosClient';
import { fetchPostulaciones, marcarPostulacionRevisada } from './adminSlice';

async function descargarCv(id, nombreArchivo) {
  const response = await axiosClient.get(`/api/admin/postulaciones/${id}/cv`, { responseType: 'blob' });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', nombreArchivo);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export default function AdminPostulacionesTab() {
  const dispatch = useDispatch();
  const postulaciones = useSelector((state) => state.admin.postulaciones);

  useEffect(() => {
    dispatch(fetchPostulaciones());
  }, [dispatch]);

  if (postulaciones.length === 0) {
    return <p style={{ color: 'var(--muted)' }}>Todavía no llegaron postulaciones.</p>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {postulaciones.map((p) => (
        <div key={p.id} className="card" style={{ opacity: p.revisado ? 0.65 : 1 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div style={{ fontWeight: 600 }}>{p.nombreCompleto}</div>
            <div style={{ fontSize: 12, color: 'var(--muted)' }}>{new Date(p.createdAt).toLocaleString('es-AR')}</div>
          </div>
          <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>{p.email} · {p.telefono}</div>
          {p.mensaje && <p style={{ marginTop: 10 }}>{p.mensaje}</p>}
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }}
              onClick={() => descargarCv(p.id, p.cvNombreOriginal)}>Descargar CV</button>
            {!p.revisado && (
              <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }}
                onClick={() => dispatch(marcarPostulacionRevisada(p.id))}>Marcar como revisado</button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
