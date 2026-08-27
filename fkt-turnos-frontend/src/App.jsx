import { Navigate, Route, Routes } from 'react-router-dom';
import { useSelector } from 'react-redux';
import Layout from './components/Layout';
import { RutaPaciente, RutaAdmin } from './components/RutaProtegida';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import BookingWizard from './features/turnos/BookingWizard';
import MisTurnosPage from './features/turnos/MisTurnosPage';
import AdminDashboardPage from './features/admin/AdminDashboardPage';

function InicioRedirect() {
  const usuario = useSelector((state) => state.auth.usuario);
  if (!usuario) return <Navigate to="/login" replace />;
  return <Navigate to={usuario.role === 'ADMIN' ? '/admin' : '/turnos'} replace />;
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<InicioRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />
        <Route path="/turnos" element={<RutaPaciente><BookingWizard /></RutaPaciente>} />
        <Route path="/turnos/mis-turnos" element={<RutaPaciente><MisTurnosPage /></RutaPaciente>} />
        <Route path="/admin" element={<RutaAdmin><AdminDashboardPage /></RutaAdmin>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
