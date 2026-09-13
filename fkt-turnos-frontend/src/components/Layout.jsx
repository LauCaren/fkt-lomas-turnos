import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../features/auth/authSlice';
import { cargarFotoInfo } from '../features/perfil/perfilSlice';
import { SITE_URL, isElectron } from '../config';
import InstalarAppBanner from './InstalarAppBanner';
import UsuarioAvatar from './UsuarioAvatar';
import SelectorFotoPerfil from './SelectorFotoPerfil';

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

const ROLES_PANEL_INTERNO = ['ADMIN', 'SECRETARIA', 'CONTADOR', 'MEDICO'];

export default function Layout({ children }) {
  const usuario = useSelector((state) => state.auth.usuario);
  const fotoInfo = useSelector((state) => state.perfil.fotoInfo);
  const fotoVersion = useSelector((state) => state.perfil.fotoVersion);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [selectorFotoAbierto, setSelectorFotoAbierto] = useState(false);
  // Los paneles internos usan un layout de sidebar + contenido (ver SidebarNav) que necesita
  // más aire que las páginas del paciente — angostarlos al mismo --maxw de siempre apretaba
  // demasiado la columna de contenido junto a la barra lateral.
  const esPanelInterno = usuario && ROLES_PANEL_INTERNO.includes(usuario.role);

  // Cualquier rol puede tener una foto de perfil (ver SelectorFotoPerfil) — se carga acá, una
  // sola vez por sesión mientras haya alguien logueado, para que el avatar del header ya
  // aparezca listo en la primera pantalla que se visita, no recién al abrir "mi cuenta".
  useEffect(() => {
    if (usuario) dispatch(cargarFotoInfo());
  }, [usuario, dispatch]);

  function handleLogout() {
    dispatch(logout());
    navigate('/login');
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header className="app-header">
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
              <div className="app-nav-links">
                {usuario.role === 'PACIENTE' && (
                  <>
                    <Link to="/turnos">Sacar turno</Link>
                    <Link to="/turnos/kinesiologia">Kinesiología</Link>
                    <Link to="/turnos/mis-turnos">Mis turnos</Link>
                    <Link to="/turnos/mi-cuenta">Mi cuenta</Link>
                  </>
                )}
                {usuario.role === 'ADMIN' && <Link to="/admin">Panel admin</Link>}
                {usuario.role === 'MEDICO' && <Link to="/medico">Panel médico</Link>}
                {usuario.role === 'CONTADOR' && <Link to="/contador">Panel contador</Link>}
              </div>
              <button
                type="button"
                onClick={() => setSelectorFotoAbierto(true)}
                aria-label="Tu foto de perfil"
                style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'none', border: 'none', cursor: 'pointer', padding: 2, borderRadius: 100 }}
              >
                <UsuarioAvatar
                  fotoUrl="/api/usuarios/me/foto"
                  tieneFoto={!!fotoInfo?.tieneFoto}
                  avatarGenerico={fotoInfo?.avatarGenerico}
                  nombreCompleto={usuario.nombreCompleto}
                  size={30}
                  version={fotoVersion}
                />
                <span style={{ fontSize: 14, color: 'var(--muted)' }}>{usuario.nombreCompleto}</span>
              </button>
              <button className="btn btn-outline" onClick={handleLogout}>Salir</button>
            </div>
          )}
        </div>
      </header>
      <main style={{ flex: 1, padding: '40px 0' }}>
        <div className="wrap" style={esPanelInterno ? { '--maxw': '1240px' } : undefined}>
          {usuario?.role === 'PACIENTE' && <InstalarAppBanner />}
          <div key={location.pathname} className="page-fade">
            {children}
          </div>
        </div>
      </main>
      {selectorFotoAbierto && <SelectorFotoPerfil onCerrar={() => setSelectorFotoAbierto(false)} />}
    </div>
  );
}
