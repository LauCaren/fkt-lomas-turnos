import Goniometro from './Goniometro';

// Pantalla de resultado de una acción de un solo paso (check-in por QR, confirmar turno desde
// el mail): la aguja barre mientras se procesa y se planta según cómo terminó — completa si
// salió bien, a mitad de arco si hace falta algo más, al principio si falló.
const PROGRESO = { cargando: 0.5, ok: 1, aviso: 0.55, error: 0.12 };

export default function Resultado({ estado, titulo, children }) {
  return (
    <div className={`resultado resultado-${estado}`} aria-busy={estado === 'cargando'}>
      <div className="resultado-medidor">
        <Goniometro progreso={PROGRESO[estado]} compacto />
      </div>
      <h1>{titulo}</h1>
      {children && <div className="resultado-detalle">{children}</div>}
      <p className="resultado-marca">FKT Lomas · Fisio Kinesio Terapia</p>
    </div>
  );
}
