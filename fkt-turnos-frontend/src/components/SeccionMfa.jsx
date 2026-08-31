import { useState } from 'react';

export default function SeccionMfa({ perfil, mfaSetup, onIniciar, onActivar, onDesactivar }) {
  const [codigo, setCodigo] = useState('');

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
    return (
      <form onSubmit={handleDesactivar} className="card" style={{ marginTop: 20 }}>
        <p style={{ fontSize: 14, marginBottom: 12 }}>
          <span className="badge badge-disponible">Activada</span> La verificación en dos pasos está activa en tu cuenta.
        </p>
        <div className="field">
          <label>Código de tu app de autenticación (para desactivar)</label>
          <input inputMode="numeric" pattern="\d{6}" maxLength={6} required value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="123456" />
        </div>
        <button type="submit" className="btn btn-outline">Desactivar verificación en dos pasos</button>
      </form>
    );
  }

  if (mfaSetup) {
    return (
      <form onSubmit={handleActivar} className="card" style={{ marginTop: 20 }}>
        <p style={{ fontSize: 14, marginBottom: 10 }}>
          1. Abrí Google Authenticator (o cualquier app de códigos TOTP) y agregá una cuenta nueva "manual".<br />
          2. Cargá este código secreto:
        </p>
        <code style={{ display: 'block', background: 'var(--bg-soft)', padding: '10px 14px', borderRadius: 6, fontSize: 15, letterSpacing: 1, marginBottom: 14, wordBreak: 'break-all' }}>
          {mfaSetup.secreto}
        </code>
        <p style={{ fontSize: 14, marginBottom: 12 }}>3. Escribí el código de 6 dígitos que te muestra la app para confirmar:</p>
        <div className="field">
          <label>Código</label>
          <input inputMode="numeric" pattern="\d{6}" maxLength={6} required autoFocus value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="123456" />
        </div>
        <button type="submit" className="btn btn-primary">Confirmar y activar</button>
      </form>
    );
  }

  return (
    <div className="card" style={{ marginTop: 20 }}>
      <p style={{ fontSize: 14, marginBottom: 14 }}>
        <span className="badge badge-cancelado">Desactivada</span> Agregá un paso extra al iniciar sesión con una app de autenticación (Google Authenticator, Authy, etc.).
      </p>
      <button className="btn btn-outline" onClick={onIniciar}>Activar verificación en dos pasos</button>
    </div>
  );
}
