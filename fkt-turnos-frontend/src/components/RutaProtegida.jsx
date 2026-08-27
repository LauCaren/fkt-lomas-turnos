import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

export function RutaPaciente({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'PACIENTE') return <Navigate to="/admin" replace />;
  return children;
}

export function RutaAdmin({ children }) {
  const { token, usuario } = useSelector((state) => state.auth);
  if (!token || !usuario) return <Navigate to="/login" replace />;
  if (usuario.role !== 'ADMIN') return <Navigate to="/turnos" replace />;
  return children;
}
