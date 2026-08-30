import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { login, limpiarMensajesAuth } from './authSlice';
import PasswordInput from '../../components/PasswordInput';

export default function LoginPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { status, error, requiereMfa } = useSelector((state) => state.auth);
  const [form, setForm] = useState({ email: '', password: '', codigoMfa: '' });

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(login(form)).unwrap().then((data) => {
      navigate(data.role === 'ADMIN' ? '/admin' : '/turnos');
    }).catch(() => {});
  }

  return (
    <div style={{ maxWidth: 420, margin: '0 auto' }}>
      <span className="eyebrow">Ingresar</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Accedé a tu cuenta</h1>
      <p style={{ marginTop: 10, color: 'var(--muted)' }}>Ingresá con tu email y contraseña para sacar o revisar tus turnos.</p>

      <form onSubmit={handleSubmit} className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" required disabled={requiereMfa} value={form.email} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <PasswordInput id="password" name="password" required disabled={requiereMfa} value={form.password} onChange={handleChange} />
        </div>
        {requiereMfa && (
          <div className="field">
            <label htmlFor="codigoMfa">Código de verificación</label>
            <input id="codigoMfa" name="codigoMfa" inputMode="numeric" pattern="\d{6}" maxLength={6} autoFocus required
              placeholder="123456" value={form.codigoMfa} onChange={handleChange} />
            <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 4 }}>Abrí tu app de autenticación y escribí el código de 6 dígitos.</p>
          </div>
        )}
        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>

      <p style={{ marginTop: 18, textAlign: 'center', color: 'var(--muted)' }}>
        <Link to="/olvide-password" onClick={() => dispatch(limpiarMensajesAuth())} style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>¿Olvidaste tu contraseña?</Link>
      </p>
      <p style={{ marginTop: 8, textAlign: 'center', color: 'var(--muted)' }}>
        ¿No tenés cuenta? <Link to="/registro" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>Creá una acá</Link>
      </p>
    </div>
  );
}
