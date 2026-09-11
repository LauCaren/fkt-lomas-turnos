import { createContext, useContext } from 'react';

export const ConfirmContext = createContext(null);

/**
 * `const confirmar = useConfirm(); const ok = await confirmar({ titulo, detalle, peligroso });`
 * — `detalle` acepta JSX (para mostrar el turno/paciente concretos, no solo texto genérico).
 * Ver ConfirmDialog.jsx (ConfirmProvider, ya montado en App.jsx) para la implementación.
 */
export function useConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) {
    throw new Error('useConfirm necesita <ConfirmProvider> en un componente ancestro (ya está montado en App.jsx)');
  }
  return ctx;
}
