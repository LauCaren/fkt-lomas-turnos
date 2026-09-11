import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { revocarMisSesiones } from '../features/cuenta/cuentaSlice';
import { logout } from '../features/auth/authSlice';
import { useConfirm } from './ui/useConfirm';

/**
 * "Cerrar todas mis sesiones" — invalida cualquier token emitido hasta ahora, incluida la
 * sesión actual (por eso, al confirmar, se desloguea localmente y se manda al login). Útil si
 * se sospecha que alguien más tiene acceso al token (dispositivo compartido, robo, etc.).
 * Reutilizado por paciente, admin/secretaria y médico/contador — el endpoint es el mismo para
 * cualquier rol logueado (ver cuentaSlice.revocarMisSesiones).
 */
export default function SeccionRevocarSesiones() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const confirmar = useConfirm();

  async function handleClick() {
    const ok = await confirmar({
      titulo: 'Cerrar todas mis sesiones',
      detalle: 'Esto va a cerrar tu sesión actual también — vas a tener que volver a ingresar.',
      confirmarTexto: 'Cerrar sesiones',
      peligroso: true,
    });
    if (!ok) return;
    dispatch(revocarMisSesiones()).unwrap().then(() => {
      dispatch(logout());
      navigate('/login', { replace: true });
    }).catch(() => {});
  }

  return (
    <div className="card">
      <p style={{ fontSize: 14, marginBottom: 12 }}>
        Cierra cualquier sesión abierta con tu cuenta en otros dispositivos o navegadores (y también esta).
        Usalo si compartiste tu contraseña sin querer o dejaste una sesión abierta en un equipo que no es tuyo.
      </p>
      <button type="button" className="btn btn-outline" onClick={handleClick}>Cerrar todas mis sesiones</button>
    </div>
  );
}
