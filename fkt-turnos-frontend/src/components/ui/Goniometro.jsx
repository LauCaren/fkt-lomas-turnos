// Arco graduado con aguja — la pieza de identidad de la app ("cada grado de movimiento"):
// el progreso se lee como una medición de goniómetro, no como una barra. Puramente visual
// (aria-hidden): quien lo usa tiene que exponer el mismo dato en texto (ver PasosReserva).

const CX = 160;
const CY = 164;
const R = 136;

function punto(grados, radio) {
  const rad = (grados * Math.PI) / 180;
  return [CX + radio * Math.cos(rad), CY - radio * Math.sin(rad)];
}

// Graduaciones cada 5° (de 180° a 0°), más largas cada 30°, como la escala de un goniómetro real.
const GRADUACIONES = Array.from({ length: 37 }, (_, i) => i * 5);

/**
 * @param progreso fracción 0..1 del recorrido (0 = aguja a la izquierda, 1 = a la derecha)
 * @param paradas fracciones 0..1 donde va una marca de paso (los pasos de la reserva)
 * @param compacto versión con menos graduaciones y trazo más grueso, para usos chicos (login)
 */
export default function Goniometro({ progreso = 0, paradas = [], compacto = false, className = '' }) {
  const p = Math.min(1, Math.max(0, progreso));
  const angulo = 180 - p * 180;
  const [x0, y0] = punto(180, R);
  const [x1, y1] = punto(0, R);

  return (
    <svg
      className={`gonio${compacto ? ' gonio-compacto' : ''} ${className}`}
      viewBox="0 0 320 180"
      aria-hidden="true"
      focusable="false"
    >
      <path className="gonio-base" d={`M ${x0} ${y0} A ${R} ${R} 0 0 1 ${x1} ${y1}`} />
      <path
        className="gonio-avance"
        d={`M ${x0} ${y0} A ${R} ${R} 0 0 1 ${x1} ${y1}`}
        pathLength="1"
        style={{ strokeDashoffset: 1 - p }}
      />
      {GRADUACIONES.filter((g) => !compacto || g % 15 === 0).map((g) => {
        const larga = compacto || g % 30 === 0;
        const [ax, ay] = punto(g, R - 7);
        const [bx, by] = punto(g, R - (larga ? 19 : 13));
        const fraccion = (180 - g) / 180;
        return (
          <line
            key={g}
            x1={ax} y1={ay} x2={bx} y2={by}
            className={`gonio-tick${larga ? ' gonio-tick-larga' : ''}${fraccion <= p + 0.001 ? ' gonio-tick-medida' : ''}`}
          />
        );
      })}
      {paradas.map((f, i) => {
        const [px, py] = punto(180 - f * 180, R);
        const alcanzada = f <= p + 0.001;
        return (
          <circle
            key={i}
            cx={px} cy={py}
            r={alcanzada ? 7 : 6}
            className={`gonio-parada${alcanzada ? ' gonio-parada-alcanzada' : ''}`}
          />
        );
      })}
      <g className="gonio-aguja" style={{ transform: `rotate(${180 - angulo}deg)` }}>
        <line x1={CX} y1={CY} x2={CX - (R - 26)} y2={CY} className="gonio-aguja-linea" />
        <circle cx={CX - (R - 26)} cy={CY} r="3.5" className="gonio-aguja-punta" />
      </g>
      <circle cx={CX} cy={CY} r="10" className="gonio-eje" />
      <circle cx={CX} cy={CY} r="3.5" className="gonio-eje-centro" />
    </svg>
  );
}
