import { useCallback, useRef, useState } from 'react';
import { ToastContext } from './useToast';

const DURACION_MS = 4000;
const SALIDA_MS = 200;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  const cerrar = useCallback((id) => {
    setToasts((prev) => prev.map((t) => (t.id === id ? { ...t, saliendo: true } : t)));
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, SALIDA_MS);
  }, []);

  // `accion` (opcional): { texto, onClick } — para el patrón "Turno cancelado · Deshacer" en vez
  // de un modal de confirmación aparte. Clickearla cierra el toast antes de ejecutar el callback.
  const toast = useCallback((mensaje, { tipo = 'success', accion } = {}) => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, mensaje, tipo, accion, saliendo: false }]);
    const timer = setTimeout(() => {
      cerrar(id);
      timersRef.current.delete(id);
    }, DURACION_MS);
    timersRef.current.set(id, timer);
  }, [cerrar]);

  function handleCerrarManual(id) {
    clearTimeout(timersRef.current.get(id));
    timersRef.current.delete(id);
    cerrar(id);
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {toasts.length > 0 && (
        <div className="toast-viewport" role="status" aria-live="polite">
          {toasts.map((t) => (
            <div key={t.id} className={`toast toast-${t.tipo}${t.saliendo ? ' toast-saliendo' : ''}`}>
              <span>{t.mensaje}</span>
              {t.accion && (
                <button
                  type="button"
                  className="toast-accion"
                  onClick={() => { handleCerrarManual(t.id); t.accion.onClick(); }}
                >
                  {t.accion.texto}
                </button>
              )}
              <button type="button" className="toast-cerrar" aria-label="Cerrar aviso" onClick={() => handleCerrarManual(t.id)}>
                &times;
              </button>
            </div>
          ))}
        </div>
      )}
    </ToastContext.Provider>
  );
}
