import { Navigate, Route, Routes } from 'react-router-dom';
import { useSelector } from 'react-redux';
import Layout from './components/Layout';
import { RutaPaciente, RutaAdmin, RutaMedico, RutaContador } from './components/RutaProtegida';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import OlvidePasswordPage from './features/auth/OlvidePasswordPage';
import RestablecerPasswordPage from './features/auth/RestablecerPasswordPage';
import BookingWizard from './features/turnos/BookingWizard';
import MisTurnosPage from './features/turnos/MisTurnosPage';
import AdminDashboardPage from './features/admin/AdminDashboardPage';
import MiCuentaPage from './features/cuenta/MiCuentaPage';
import MedicoDashboardPage from './features/medico/MedicoDashboardPage';
import ContadorDashboardPage from './features/contador/ContadorDashboardPage';
import KinesiologiaPage from './features/kinesiologia/KinesiologiaPage';

const RUTA_POR_ROL = { ADMIN: '/admin', MEDICO: '/medico', CONTADOR: '/contador' };

function InicioRedirect() {
  const usuario = useSelector((state) => state.auth.usuario);
  if (!usuario) return <Navigate to="/login" replace />;
  return <Navigate to={RUTA_POR_ROL[usuario.role] || '/turnos'} replace />;
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<InicioRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />
        <Route path="/olvide-password" element={<OlvidePasswordPage />} />
        <Route path="/restablecer-password" element={<RestablecerPasswordPage />} />
        <Route path="/turnos" element={<RutaPaciente><BookingWizard /></RutaPaciente>} />
        <Route path="/turnos/kinesiologia" element={<RutaPaciente><KinesiologiaPage /></RutaPaciente>} />
        <Route path="/turnos/mis-turnos" element={<RutaPaciente><MisTurnosPage /></RutaPaciente>} />
        <Route path="/turnos/mi-cuenta" element={<RutaPaciente><MiCuentaPage /></RutaPaciente>} />
        <Route path="/admin" element={<RutaAdmin><AdminDashboardPage /></RutaAdmin>} />
        <Route path="/medico" element={<RutaMedico><MedicoDashboardPage /></RutaMedico>} />
        <Route path="/contador" element={<RutaContador><ContadorDashboardPage /></RutaContador>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
