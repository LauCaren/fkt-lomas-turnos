import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Moon, Sun } from 'lucide-react';
import { logout } from '../features/auth/authSlice';
import { cargarFotoInfo } from '../features/perfil/perfilSlice';
import { SITE_URL, isElectron } from '../config';
import { useTema } from '../hooks/useTema';
import InstalarAppBanner from './InstalarAppBanner';
import UsuarioAvatar from './UsuarioAvatar';
import SelectorFotoPerfil from './SelectorFotoPerfil';
import NotificacionesBell from './NotificacionesBell';
import PieLegal from './PieLegal';

function Marca() {
  return (
    <>
      <img src="/img/fkt-logo-mark-transparent.png" alt="" aria-hidden="true" className="app-marca-logo" />
      <span className="app-marca-texto">
        <span className="app-marca-nombre">
          FKT Lomas<span> · Turnos</span>
        </span>
        <span className="app-marca-bajada">Fisio Kinesio Terapia</span>
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
  const { tema, alternar: alternarTema } = useTema();
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
        <div className="wrap app-header-inner" style={esPanelInterno ? { '--maxw': '1240px' } : undefined}>
          {isElectron ? (
            // La app de escritorio es solo para los paneles internos (admin/médico/contador) —
            // no tiene que poder salir nunca al sitio informativo público.
            <div className="app-marca">
              <Marca />
            </div>
          ) : (
            <a href={SITE_URL} className="app-marca">
              <Marca />
            </a>
          )}
          {usuario && (
            <nav className="app-nav-links" aria-label="Secciones">
              {usuario.role === 'PACIENTE' && (
                <>
                  <NavLink to="/turnos" end>Sacar turno</NavLink>
                  <NavLink to="/turnos/kinesiologia">Kinesiología</NavLink>
                  <NavLink to="/turnos/mis-turnos">Mis turnos</NavLink>
                  <NavLink to="/turnos/mi-cuenta">Mi cuenta</NavLink>
                </>
              )}
              {usuario.role === 'ADMIN' && <NavLink to="/admin">Panel admin</NavLink>}
              {usuario.role === 'MEDICO' && <NavLink to="/medico">Panel médico</NavLink>}
              {usuario.role === 'CONTADOR' && <NavLink to="/contador">Panel contador</NavLink>}
            </nav>
          )}
          <div className="app-acciones">
            <button
              type="button"
              onClick={alternarTema}
              aria-label={tema === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
              title={tema === 'dark' ? 'Tema claro' : 'Tema oscuro'}
              className="btn-icon-tema"
            >
              {tema === 'dark' ? <Sun size={19} /> : <Moon size={19} />}
            </button>
            {usuario && (
              <>
                <NotificacionesBell />
                <button
                  type="button"
                  onClick={() => setSelectorFotoAbierto(true)}
                  aria-label="Tu foto de perfil"
                  className="app-usuario"
                >
                  <UsuarioAvatar
                    fotoUrl="/api/usuarios/me/foto"
                    tieneFoto={!!fotoInfo?.tieneFoto}
                    avatarGenerico={fotoInfo?.avatarGenerico}
                    nombreCompleto={usuario.nombreCompleto}
                    size={34}
                    version={fotoVersion}
                  />
                  <span className="app-usuario-nombre">{usuario.nombreCompleto}</span>
                </button>
                <button className="btn btn-outline app-salir" onClick={handleLogout}>Salir</button>
              </>
            )}
          </div>
        </div>
      </header>
      <main style={{ flex: 1, padding: '40px 0 56px' }}>
        <div className="wrap" style={esPanelInterno ? { '--maxw': '1240px' } : undefined}>
          {usuario?.role === 'PACIENTE' && <InstalarAppBanner />}
          <div key={location.pathname} className="page-fade">
            {children}
          </div>
        </div>
      </main>
      {!esPanelInterno && <PieLegal />}
      {selectorFotoAbierto && <SelectorFotoPerfil onCerrar={() => setSelectorFotoAbierto(false)} />}
    </div>
  );
}
