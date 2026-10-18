import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchArrepentimientos, resolverArrepentimiento } from './adminSlice';
import EstadoVacio from '../../components/EstadoVacio';
import { formatFechaHora } from '../../utils/formato';

// Bandeja del "Botón de arrepentimiento" (Ley 24.240 art. 34, Res. SCI 424/2020). Cada pedido
// trae un código que la persona ya recibió; el centro lo gestiona a mano (cancelar el turno y,
// si pagó online, devolver el dinero por Mercado Pago) y lo marca como resuelto con una nota
// que queda como constancia.

const ESTADOS = [
  { value: 'PENDIENTE', label: 'Pendientes' },
  { value: 'RESUELTA', label: 'Resueltos' },
  { value: '', label: 'Todos' },
];

function FilaArrepentimiento({ solicitud }) {
  const dispatch = useDispatch();
  const [resolviendo, setResolviendo] = useState(false);
  const [nota, setNota] = useState('');
  const pendiente = solicitud.estado === 'PENDIENTE';

  function confirmar(e) {
    e.preventDefault();
    dispatch(resolverArrepentimiento({ id: solicitud.id, notaResolucion: nota.trim() }));
    setResolviendo(false);
  }

  return (
    <article className="card pedido-legal">
      <header className="pedido-legal-cabecera">
        <div>
          <h3 className="pedido-legal-codigo">{solicitud.codigo}</h3>
          <p className="pedido-legal-meta">Recibido el {formatFechaHora(solicitud.createdAt)}</p>
        </div>
        <span className={`badge ${pendiente ? 'badge-pendiente' : 'badge-disponible'}`}>
          {pendiente ? 'Pendiente' : 'Resuelto'}
        </span>
      </header>

      <dl className="pedido-legal-datos">
        <div><dt>Nombre</dt><dd>{solicitud.nombreCompleto}</dd></div>
        {solicitud.dni && <div><dt>DNI</dt><dd>{solicitud.dni}</dd></div>}
        <div><dt>Email</dt><dd><a href={`mailto:${solicitud.email}`}>{solicitud.email}</a></dd></div>
        {solicitud.telefono && <div><dt>Teléfono</dt><dd><a href={`tel:${solicitud.telefono}`}>{solicitud.telefono}</a></dd></div>}
      </dl>
      <p className="pedido-legal-detalle">{solicitud.detalleOperacion}</p>

      {pendiente && !resolviendo && (
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setResolviendo(true)}>
          Marcar como resuelto
        </button>
      )}
      {pendiente && resolviendo && (
        <form className="pedido-legal-resolver" onSubmit={confirmar}>
          <div className="field">
            <label htmlFor={`nota-${solicitud.id}`}>¿Cómo se resolvió?</label>
            <input id={`nota-${solicitud.id}`} required maxLength={1000} value={nota} onChange={(e) => setNota(e.target.value)}
              placeholder="Ej.: turno cancelado y pago devuelto por Mercado Pago el 12/10" />
          </div>
          <div className="pedido-legal-acciones">
            <button type="submit" className="btn btn-primary btn-sm" disabled={!nota.trim()}>Confirmar</button>
            <button type="button" className="btn btn-outline btn-sm" onClick={() => setResolviendo(false)}>Cancelar</button>
          </div>
        </form>
      )}
      {!pendiente && (
        <p className="pedido-legal-meta">
          Resuelto{solicitud.resueltaPorNombre ? ` por ${solicitud.resueltaPorNombre}` : ''} el {formatFechaHora(solicitud.resueltaAt)}: {solicitud.notaResolucion}
        </p>
      )}
    </article>
  );
}

export default function AdminArrepentimientosTab() {
  const dispatch = useDispatch();
  const { arrepentimientos, error, mensajeExito } = useSelector((state) => state.admin);
  const [estado, setEstado] = useState('PENDIENTE');

  useEffect(() => {
    dispatch(fetchArrepentimientos(estado));
  }, [dispatch, estado]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status">{mensajeExito}</div>}

      <p className="tab-intro">
        Pedidos del botón de arrepentimiento: personas que quieren cancelar un turno o pago hecho online
        dentro de los 10 días (Ley 24.240). Ya recibieron su código por email. Cancelá el turno, devolvé el
        pago si corresponde y marcá el pedido como resuelto contando cómo.
      </p>

      <div className="filter-bar">
        <div className="field filtro-estrecho">
          <label htmlFor="arrepentimientosFiltroEstado">Estado</label>
          <select id="arrepentimientosFiltroEstado" value={estado} onChange={(e) => setEstado(e.target.value)}>
            {ESTADOS.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
          </select>
        </div>
      </div>

      {arrepentimientos.length === 0 && <EstadoVacio titulo="Sin pedidos" subtitulo="No hay pedidos en este estado." />}
      <div className="stagger">
        {arrepentimientos.map((s) => <FilaArrepentimiento key={s.id} solicitud={s} />)}
      </div>
    </div>
  );
}
