import { useEffect } from 'react';

export default function Lightbox({ src, alt, onClose }) {
  useEffect(() => {
    if (!src) return undefined;
    function handleKeyDown(e) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [src, onClose]);

  if (!src) return null;

  return (
    <div className="lightbox open" role="dialog" aria-modal="true" onClick={onClose}>
      <button type="button" className="lightbox-cerrar" aria-label="Cerrar" onClick={onClose}>&times;</button>
      <img src={src} alt={alt || ''} onClick={(e) => e.stopPropagation()} />
    </div>
  );
}
