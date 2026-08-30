import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { login } from './authSlice';

export default function LoginPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { status, error } = useSelector((state) => state.auth);
  const [form, setForm] = useState({ email: '', password: '' });

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
          <input id="email" name="email" type="email" required value={form.email} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <input id="password" name="password" type="password" required value={form.password} onChange={handleChange} />
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>

      <p style={{ marginTop: 18, textAlign: 'center', color: 'var(--muted)' }}>
        ¿No tenés cuenta? <Link to="/registro" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>Creá una acá</Link>
      </p>
    </div>
  );
}
