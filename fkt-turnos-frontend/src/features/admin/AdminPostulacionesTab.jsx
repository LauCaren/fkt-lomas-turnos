import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchPostulaciones, marcarPostulacionRevisada } from './adminSlice';
import { descargarArchivo } from '../../api/descargas';

function descargarCv(id, nombreArchivo) {
  return descargarArchivo(`/api/admin/postulaciones/${id}/cv`, undefined, nombreArchivo);
}

export default function AdminPostulacionesTab() {
  const dispatch = useDispatch();
  const postulaciones = useSelector((state) => state.admin.postulaciones);
  const pagina = useSelector((state) => state.admin.postulacionesPagina);
  const hayMas = useSelector((state) => state.admin.postulacionesHayMas);

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
      {hayMas && (
        <button className="btn btn-outline" style={{ alignSelf: 'center' }}
          onClick={() => dispatch(fetchPostulaciones(pagina + 1))}>
          Cargar más
        </button>
      )}
    </div>
  );
}
