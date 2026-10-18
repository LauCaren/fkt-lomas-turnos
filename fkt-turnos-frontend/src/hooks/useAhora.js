import { useEffect, useState } from 'react';

// La hora actual como estado de React, que se actualiza sola cada `cadaMs` (por defecto, cada
// minuto). Usar esto en vez de `new Date()` suelto dentro del render:
//   - el render queda "puro" (mismo resultado para el mismo estado), como pide React;
//   - lo que depende de la hora se actualiza solo: el resumen de la agenda deja de mostrar como
//     "próximo" un turno que ya pasó, y el saludo del panel cambia de fecha a la medianoche,
//     sin que nadie tenga que recargar la página.
export function useAhora(cadaMs = 60_000) {
  const [ahora, setAhora] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setAhora(new Date()), cadaMs);
    return () => clearInterval(id);
  }, [cadaMs]);
  return ahora;
}
