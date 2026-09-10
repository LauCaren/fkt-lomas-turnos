import { useEffect, useRef, useState } from 'react';

// Anima desde el último valor mostrado hacia el nuevo (no siempre desde 0 — si el resumen se
// refresca en caliente, un stat que pasa de 3 a 4 sube un paso, no arranca de cero de nuevo).
// Respeta prefers-reduced-motion saltando directo al valor final, sin pasos intermedios.
function useCountUp(valorNumerico) {
  const [mostrado, setMostrado] = useState(valorNumerico ?? 0);
  const desdeRef = useRef(valorNumerico ?? 0);

  useEffect(() => {
    if (valorNumerico == null || Number.isNaN(valorNumerico)) return undefined;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setMostrado(valorNumerico);
      desdeRef.current = valorNumerico;
      return undefined;
    }
    const desde = desdeRef.current;
    const hasta = valorNumerico;
    if (desde === hasta) return undefined;

    const duracionMs = 700;
    const inicio = performance.now();
    let frame;
    function tick(ahora) {
      const t = Math.min((ahora - inicio) / duracionMs, 1);
      const suavizado = 1 - (1 - t) ** 3; // ease-out cúbico
      setMostrado(desde + (hasta - desde) * suavizado);
      if (t < 1) {
        frame = requestAnimationFrame(tick);
      } else {
        desdeRef.current = hasta;
      }
    }
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [valorNumerico]);

  return mostrado;
}

/**
 * `valor` sigue aceptando cualquier cosa (string ya formateado, número, lo que sea) para no
 * romper a nadie que ya lo usa así. Para el conteo animado hace falta el número crudo: si
 * `valor` ya es un number, se usa directo; si es un string formateado (ej. moneda), pasar
 * `valorNumerico` (el número de verdad) + `formatValor` (la función que lo formatea) — ver
 * AdminResumenTab/ContadorResumenTab para el caso de moneda.
 */
export default function StatCard({ etiqueta, valor, valorNumerico, formatValor, destacado, onClick }) {
  const numero = valorNumerico ?? (typeof valor === 'number' ? valor : null);
  const animado = useCountUp(numero);
  const formatear = formatValor || ((n) => Math.round(n).toLocaleString('es-AR'));
  const contenido = numero != null ? formatear(animado) : valor;

  const Comp = onClick ? 'button' : 'div';
  return (
    <Comp
      type={onClick ? 'button' : undefined}
      onClick={onClick}
      className={`stat-card${onClick ? ' stat-card-btn' : ''}`}
    >
      <div className="stat-card-label">{etiqueta}</div>
      <div className={`stat-card-value${destacado ? ' destacado' : ''}`}>{contenido}</div>
    </Comp>
  );
}
