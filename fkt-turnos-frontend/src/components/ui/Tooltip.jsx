// Envuelve un único elemento hijo (ej. un botón solo-ícono) y le agrega una burbuja de
// texto visible al pasar el mouse o enfocar con teclado. El hijo mantiene su propio
// aria-label — esto es una pista visual adicional, no un reemplazo de accesibilidad.
export default function Tooltip({ texto, children }) {
  return (
    <span className="tooltip-wrap">
      {children}
      <span className="tooltip-bubble" role="tooltip">{texto}</span>
    </span>
  );
}
