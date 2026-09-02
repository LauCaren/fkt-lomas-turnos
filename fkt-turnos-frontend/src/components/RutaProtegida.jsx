import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

export function RutaPaciente({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'PACIENTE') return <Navigate to="/" replace />;
  return children;
}

export function RutaAdmin({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
}

export function RutaSecretaria({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'SECRETARIA') return <Navigate to="/" replace />;
  return children;
}

export function RutaMedico({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'MEDICO') return <Navigate to="/" replace />;
  return children;
}

export function RutaContador({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'CONTADOR') return <Navigate to="/" replace />;
  return children;
}
