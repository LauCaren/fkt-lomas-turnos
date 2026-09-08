import { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';
import { APP_URL, isElectron } from '../../config';

// El QR es fijo (siempre apunta a /checkin, no a un turno puntual) — se imprime una sola vez
// y queda pegado en la pared. Cada paciente que lo escanea se identifica solo (login) y el
// backend busca su propio turno de hoy.
const URL_CHECKIN = APP_URL ? `${APP_URL}/checkin` : null;

export default function SalaEsperaQr() {
  const canvasRef = useRef(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!canvasRef.current || !URL_CHECKIN) return;
    QRCode.toCanvas(canvasRef.current, URL_CHECKIN, { width: 220, margin: 1 }, (err) => {
      if (err) setError('No se pudo generar el código QR.');
    });
  }, []);

  if (!URL_CHECKIN) {
    return (
      <div className="card alert alert-error" style={{ marginBottom: 24 }}>
        {isElectron
          ? 'Para generar el cartel QR desde la app de escritorio hace falta configurar VITE_APP_URL (la URL pública de la app de turnos) al compilarla.'
          : 'No se pudo determinar la URL pública de la app para generar el cartel QR.'}
      </div>
    );
  }

  return (
    <div className="card" style={{ marginBottom: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <strong>Cartel QR para la sala de espera</strong>
          <p style={{ color: 'var(--muted)', fontSize: 13.5, marginTop: 4 }}>
            Imprimilo y pegalo en la sala de espera. El paciente lo escanea al llegar y su turno de hoy
            se marca "Llegó" automáticamente (salvo que tenga copago de IOMA, ahí le pide que pase por el mostrador).
          </p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={() => window.print()}>Imprimir cartel</button>
      </div>

      {error && <div className="alert alert-error" role="alert" style={{ marginTop: 12 }}>{error}</div>}

      <div id="cartel-qr-imprimir" style={{ textAlign: 'center', marginTop: 20, padding: 24, border: '1px solid var(--line)', borderRadius: 'var(--radius-m)' }}>
        <div className="eyebrow">FKT Lomas</div>
        <h2 style={{ marginTop: 8, fontSize: 22 }}>Escaneá al llegar</h2>
        <p style={{ color: 'var(--muted)', marginTop: 6, maxWidth: 320, marginInline: 'auto' }}>
          Apuntá la cámara de tu celular a este código para avisarnos que llegaste a tu turno.
        </p>
        <canvas ref={canvasRef} style={{ marginTop: 16 }} />
        <p style={{ color: 'var(--muted)', fontSize: 12, marginTop: 12 }}>
          ¿Tenés copago de IOMA o no podés escanear? Avisale directamente a la secretaria en el mostrador.
        </p>
      </div>
    </div>
  );
}
