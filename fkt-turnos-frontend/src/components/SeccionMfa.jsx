import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { ROLES_MFA_OBLIGATORIO } from '../config';

export default function SeccionMfa({ perfil, mfaSetup, onIniciar, onActivar, onDesactivar }) {
  const [codigo, setCodigo] = useState('');
  const [qrDataUrl, setQrDataUrl] = useState(null);

  // El QR se genera en el navegador a partir del otpAuthUri que ya manda el backend —
  // nunca sale de acá (ni el secreto ni el QR pegan a ningún servicio de terceros para
  // "convertir texto en imagen", como suele hacerse con las APIs públicas de QR).
  useEffect(() => {
    if (!mfaSetup?.otpAuthUri) { setQrDataUrl(null); return; }
    let cancelado = false;
    QRCode.toDataURL(mfaSetup.otpAuthUri, { width: 220, margin: 1 })
      .then((url) => { if (!cancelado) setQrDataUrl(url); })
      .catch(() => { if (!cancelado) setQrDataUrl(null); });
    return () => { cancelado = true; };
  }, [mfaSetup?.otpAuthUri]);

  function handleActivar(e) {
    e.preventDefault();
    onActivar(codigo).then(() => setCodigo('')).catch(() => {});
  }

  function handleDesactivar(e) {
    e.preventDefault();
    onDesactivar(codigo).then(() => setCodigo('')).catch(() => {});
  }

  if (!perfil) return null;

  if (perfil.mfaHabilitado) {
    const esObligatoria = ROLES_MFA_OBLIGATORIO.includes(perfil.role);
    return (
      <div className="card">
        <p style={{ fontSize: 14, marginBottom: esObligatoria ? 0 : 12 }}>
          <span className="badge badge-disponible">Activada</span> La verificación en dos pasos está activa en tu cuenta.
        </p>
        {esObligatoria ? (
          <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 8 }}>
            Es obligatoria para tu rol y no se puede desactivar.
          </p>
        ) : (
          <form onSubmit={handleDesactivar}>
            <div className="field" style={{ marginTop: 12 }}>
              <label htmlFor="mfaCodigoDesactivar">Código de tu app de autenticación (para desactivar)</label>
              <input id="mfaCodigoDesactivar" inputMode="numeric" pattern="\d{6}" maxLength={6} required value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="123456" />
            </div>
            <button type="submit" className="btn btn-outline">Desactivar verificación en dos pasos</button>
          </form>
        )}
      </div>
    );
  }

  if (mfaSetup) {
    return (
      <form onSubmit={handleActivar} className="card">
        <p style={{ fontSize: 14, marginBottom: 14 }}>
          1. Abrí Google Authenticator (o cualquier app de códigos TOTP) y escaneá este código QR:
        </p>
        {qrDataUrl && (
          <img
            src={qrDataUrl}
            alt="Código QR para configurar la verificación en dos pasos"
            width={220}
            height={220}
            style={{ display: 'block', margin: '0 auto 16px', border: '1px solid var(--line)', borderRadius: 8, padding: 8, background: '#fff' }}
          />
        )}
        <details style={{ marginBottom: 16 }}>
          <summary style={{ fontSize: 13.5, color: 'var(--muted)', cursor: 'pointer' }}>¿No podés escanear? Cargalo a mano</summary>
          <code style={{ display: 'block', background: 'var(--bg-soft)', padding: '10px 14px', borderRadius: 6, fontSize: 15, letterSpacing: 1, marginTop: 10, wordBreak: 'break-all' }}>
            {mfaSetup.secreto}
          </code>
        </details>
        <p style={{ fontSize: 14, marginBottom: 12 }}>2. Escribí el código de 6 dígitos que te muestra la app para confirmar:</p>
        <div className="field">
          <label htmlFor="mfaCodigoActivar">Código</label>
          <input id="mfaCodigoActivar" inputMode="numeric" pattern="\d{6}" maxLength={6} required autoFocus value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="123456" />
        </div>
        <button type="submit" className="btn btn-primary">Confirmar y activar</button>
      </form>
    );
  }

  return (
    <div className="card">
      <p style={{ fontSize: 14, marginBottom: 14 }}>
        <span className="badge badge-cancelado">Desactivada</span> Agregá un paso extra al iniciar sesión con una app de autenticación (Google Authenticator, Authy, etc.).
      </p>
      <button className="btn btn-outline" onClick={onIniciar}>Activar verificación en dos pasos</button>
    </div>
  );
}
