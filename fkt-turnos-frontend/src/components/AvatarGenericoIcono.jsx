// Silueta genérica de persona — el mismo dibujo para los 8 avatares, cambia solo el color de
// fondo que le pone UsuarioAvatar (ver constants/avataresGenericos.js). currentColor para que
// el color lo controle quien lo usa vía CSS `color`.
export default function AvatarGenericoIcono({ style }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" style={style} aria-hidden="true">
      <circle cx="12" cy="8.2" r="4" fill="currentColor" />
      <path d="M3.5 20.5c0-4.42 3.8-7.5 8.5-7.5s8.5 3.08 8.5 7.5" fill="currentColor" />
    </svg>
  );
}
