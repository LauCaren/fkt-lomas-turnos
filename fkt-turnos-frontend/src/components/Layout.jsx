import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../features/auth/authSlice';

const SITE_URL = import.meta.env.VITE_SITE_URL || 'https://fktlomas.com';

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
          <a href={SITE_URL} style={{ fontFamily: "'Space Grotesk', sans-serif", fontWeight: 700, fontSize: 20, whiteSpace: 'nowrap' }}>
            FKT Lomas<span style={{ color: 'var(--orange)' }}> · Turnos</span>
          </a>
          {usuario && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
              {usuario.role === 'PACIENTE' && (
                <>
                  <Link to="/turnos" style={{ fontSize: 14, fontWeight: 600 }}>Sacar turno</Link>
                  <Link to="/turnos/mis-turnos" style={{ fontSize: 14, fontWeight: 600 }}>Mis turnos</Link>
                </>
              )}
              {usuario.role === 'ADMIN' && (
                <Link to="/admin" style={{ fontSize: 14, fontWeight: 600 }}>Panel admin</Link>
              )}
              <span style={{ fontSize: 14, color: 'var(--muted)' }}>{usuario.nombreCompleto}</span>
              <button className="btn btn-outline" onClick={handleLogout}>Salir</button>
            </div>
          )}
        </div>
      </header>
      <main style={{ flex: 1, padding: '40px 0' }}>
        <div className="wrap">{children}</div>
      </main>
    </div>
  );
}
