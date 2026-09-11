import { useEffect, useRef } from 'react';

/**
 * Overlay + card genéricos, mount/unmount-based (el padre controla si existe con un
 * condicional, no con una prop "open") — mismo patrón de foco que ya usa Lightbox.jsx:
 * guarda qué tenía el foco antes de abrir, lo mueve adentro del modal, y lo devuelve al
 * cerrar. `initialFocusRef` deja elegir a qué elemento enfocar primero (por default, la
 * card misma); útil para no dejar el foco por defecto en la acción "confirmar" de un
 * ConfirmDialog destructivo.
 */
export default function Modal({ children, onCerrar, labelledBy, cerrarAlClickearFuera = true, initialFocusRef, className = '' }) {
  const disparadorRef = useRef(null);
  const cardRef = useRef(null);

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape') onCerrar();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onCerrar]);

  useEffect(() => {
    disparadorRef.current = document.activeElement;
    (initialFocusRef?.current || cardRef.current)?.focus();
    return () => {
      if (disparadorRef.current instanceof HTMLElement) {
        disparadorRef.current.focus();
      }
    };
  }, [initialFocusRef]);

  return (
    <div className="modal-overlay" role="presentation" onClick={cerrarAlClickearFuera ? onCerrar : undefined}>
      <div
        ref={cardRef}
        className={`modal-card${className ? ` ${className}` : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
}
