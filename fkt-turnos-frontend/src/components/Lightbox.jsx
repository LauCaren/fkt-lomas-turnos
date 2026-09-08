import { useEffect, useRef } from 'react';

export default function Lightbox({ src, alt, onClose }) {
  const cerrarRef = useRef(null);
  const disparadorRef = useRef(null);

  useEffect(() => {
    if (!src) return undefined;
    function handleKeyDown(e) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [src, onClose]);

  useEffect(() => {
    if (!src) return undefined;
    // Sin esto, al abrir el lightbox el foco de teclado se queda perdido en el elemento
    // que lo disparó (o en el body) mientras la pantalla se llena visualmente con el
    // overlay — y al cerrar, el foco tiene que volver ahí, no perderse en el body.
    disparadorRef.current = document.activeElement;
    cerrarRef.current?.focus();
    return () => {
      if (disparadorRef.current instanceof HTMLElement) {
        disparadorRef.current.focus();
      }
    };
  }, [src]);

  if (!src) return null;

  return (
    <div className="lightbox open" role="dialog" aria-modal="true" onClick={onClose}>
      <button ref={cerrarRef} type="button" className="lightbox-cerrar" aria-label="Cerrar" onClick={onClose}>&times;</button>
      <img src={src} alt={alt || ''} onClick={(e) => e.stopPropagation()} />
    </div>
  );
}
