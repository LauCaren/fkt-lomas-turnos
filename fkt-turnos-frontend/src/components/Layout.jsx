import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../features/auth/authSlice';
import { SITE_URL, isElectron } from '../config';
import InstalarAppBanner from './InstalarAppBanner';

function Marca() {
  return (
    <>
      <img src="/img/fkt-logo-mark-transparent.png" alt="" aria-hidden="true" style={{ width: 34, height: 34, objectFit: 'contain', flexShrink: 0 }} />
      <span style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.15 }}>
        <span style={{ fontFamily: "'Playfair Display', serif", fontWeight: 700, fontSize: 20 }}>
          FKT Lomas<span style={{ color: 'var(--teal)' }}> · Turnos</span>
        </span>
        <span style={{ fontFamily: "'Caveat', cursive", fontWeight: 600, fontSize: 14, color: 'var(--muted)' }}>Fisio Kinesio Terapia</span>
      </span>
    </>
  );
}

export default function Layout({ children }) {
  const usuario = useSelector((state) => state.auth.usuario);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  function handleLogout() {
    dispatch(logout());
    navigate('/login');
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={{ background: '#fff', borderBottom: '1px solid var(--line)' }}>
        <div className="wrap" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 0', flexWrap: 'wrap', gap: 12 }}>
          {isElectron ? (
            // La app de escritorio es solo para los paneles internos (admin/médico/contador) —
            // no tiene que poder salir nunca al sitio informativo público.
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, whiteSpace: 'nowrap' }}>
              <Marca />
            </div>
          ) : (
            <a href={SITE_URL} style={{ display: 'flex', alignItems: 'center', gap: 10, whiteSpace: 'nowrap' }}>
              <Marca />
            </a>
          )}
          {usuario && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
              {usuario.role === 'PACIENTE' && (
                <>
                  <Link to="/turnos" style={{ fontSize: 14, fontWeight: 600 }}>Sacar turno</Link>
                  <Link to="/turnos/kinesiologia" style={{ fontSize: 14, fontWeight: 600 }}>Kinesiología</Link>
                  <Link to="/turnos/mis-turnos" style={{ fontSize: 14, fontWeight: 600 }}>Mis turnos</Link>
                  <Link to="/turnos/mi-cuenta" style={{ fontSize: 14, fontWeight: 600 }}>Mi cuenta</Link>
                </>
              )}
              {usuario.role === 'ADMIN' && (
                <Link to="/admin" style={{ fontSize: 14, fontWeight: 600 }}>Panel admin</Link>
              )}
              {usuario.role === 'MEDICO' && (
                <Link to="/medico" style={{ fontSize: 14, fontWeight: 600 }}>Panel médico</Link>
              )}
              {usuario.role === 'CONTADOR' && (
                <Link to="/contador" style={{ fontSize: 14, fontWeight: 600 }}>Panel contador</Link>
              )}
              <span style={{ fontSize: 14, color: 'var(--muted)' }}>{usuario.nombreCompleto}</span>
              <button className="btn btn-outline" onClick={handleLogout}>Salir</button>
            </div>
          )}
        </div>
      </header>
      <main style={{ flex: 1, padding: '40px 0' }}>
        <div className="wrap">
          {usuario?.role === 'PACIENTE' && <InstalarAppBanner />}
          {children}
        </div>
      </main>
    </div>
  );
}
