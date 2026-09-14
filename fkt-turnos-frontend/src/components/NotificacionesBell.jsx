import { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Bell } from 'lucide-react';
import { fetchNotificaciones, fetchCantidadNoLeidas, marcarNotificacionLeida, marcarTodasLeidas } from '../features/notificaciones/notificacionesSlice';

const INTERVALO_POLLING_MS = 60000;

const ETIQUETA_TIPO = {
  TURNO_CANCELADO_STAFF: 'Turno cancelado',
  TURNO_REPROGRAMADO: 'Turno reprogramado',
  TURNO_ASIGNADO_LISTA_ESPERA: 'Turno asignado',
};

function hace(fechaISO) {
  const segundos = Math.max(0, Math.floor((Date.now() - new Date(fechaISO).getTime()) / 1000));
  if (segundos < 60) return 'recién';
  const minutos = Math.floor(segundos / 60);
  if (minutos < 60) return `hace ${minutos}m`;
  const horas = Math.floor(minutos / 60);
  if (horas < 24) return `hace ${horas}h`;
  const dias = Math.floor(horas / 24);
  return `hace ${dias}d`;
}

/**
 * Campanita del header (Layout.jsx, cualquier rol logueado) — avisa dentro de la app cuando
 * secretaría/admin cancela, reprograma o asigna un turno, sin depender de que el paciente
 * revise el email o WhatsApp (que además pueden fallar en silencio, ver CanalFallbackService).
 * El número se actualiza solo con polling liviano; la lista completa recién se pide al abrir el
 * panel, para no traerla en cada carga de página si nadie la va a mirar.
 */
export default function NotificacionesBell() {
  const dispatch = useDispatch();
  const { items, cantidadNoLeidas } = useSelector((state) => state.notificaciones);
  const [abierto, setAbierto] = useState(false);
  const contenedorRef = useRef(null);

  useEffect(() => {
    dispatch(fetchCantidadNoLeidas());
    const intervalo = setInterval(() => dispatch(fetchCantidadNoLeidas()), INTERVALO_POLLING_MS);
    return () => clearInterval(intervalo);
  }, [dispatch]);

  useEffect(() => {
    if (abierto) dispatch(fetchNotificaciones());
  }, [abierto, dispatch]);

  useEffect(() => {
    if (!abierto) return undefined;
    function handleClickFuera(e) {
      if (contenedorRef.current && !contenedorRef.current.contains(e.target)) setAbierto(false);
    }
    document.addEventListener('mousedown', handleClickFuera);
    return () => document.removeEventListener('mousedown', handleClickFuera);
  }, [abierto]);

  function handleClickNotificacion(n) {
    if (!n.leida) dispatch(marcarNotificacionLeida(n.id));
  }

  return (
    <div ref={contenedorRef} style={{ position: 'relative' }}>
      <button
        type="button"
        onClick={() => setAbierto((v) => !v)}
        aria-label={cantidadNoLeidas > 0 ? `Notificaciones (${cantidadNoLeidas} sin leer)` : 'Notificaciones'}
        aria-expanded={abierto}
        className="btn-icon-tema"
        style={{ position: 'relative' }}
      >
        <Bell size={18} />
        {cantidadNoLeidas > 0 && (
          <span
            aria-hidden="true"
            style={{
              position: 'absolute', top: -2, right: -2, minWidth: 16, height: 16, padding: '0 3px',
              borderRadius: 100, background: 'var(--red)', color: '#fff', fontSize: 10, fontWeight: 700,
              display: 'flex', alignItems: 'center', justifyContent: 'center', lineHeight: 1,
            }}
          >
            {cantidadNoLeidas > 9 ? '9+' : cantidadNoLeidas}
          </span>
        )}
      </button>

      {abierto && (
        <div
          className="card"
          role="menu"
          style={{
            position: 'absolute', top: 'calc(100% + 8px)', right: 0, width: 340, maxHeight: 420,
            overflowY: 'auto', padding: 0, zIndex: 200, boxShadow: 'var(--shadow-lg)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 16px', borderBottom: '1px solid var(--line)' }}>
            <span style={{ fontWeight: 600, fontSize: 14.5 }}>Notificaciones</span>
            {cantidadNoLeidas > 0 && (
              <button type="button" onClick={() => dispatch(marcarTodasLeidas())} style={{ fontSize: 12.5, color: 'var(--teal-dark)', fontWeight: 600, background: 'none', border: 'none', cursor: 'pointer' }}>
                Marcar todas como leídas
              </button>
            )}
          </div>
          {items.length === 0 && (
            <p style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--muted)', fontSize: 13.5 }}>
              No tenés notificaciones todavía.
            </p>
          )}
          {items.map((n) => (
            <button
              key={n.id}
              type="button"
              onClick={() => handleClickNotificacion(n)}
              style={{
                display: 'block', width: '100%', textAlign: 'left', padding: '12px 16px', border: 'none',
                borderBottom: '1px solid var(--line)', cursor: n.leida ? 'default' : 'pointer',
                background: n.leida ? 'transparent' : 'var(--teal-tint)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                {!n.leida && <span aria-hidden="true" style={{ width: 6, height: 6, borderRadius: 100, background: 'var(--teal)', flexShrink: 0 }} />}
                <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: '.02em' }}>
                  {ETIQUETA_TIPO[n.tipo] || 'Aviso'}
                </span>
              </div>
              <p style={{ fontSize: 13.5, color: 'var(--ink)', lineHeight: 1.4 }}>{n.mensaje}</p>
              <span style={{ fontSize: 11.5, color: 'var(--muted)' }}>{hace(n.createdAt)}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
