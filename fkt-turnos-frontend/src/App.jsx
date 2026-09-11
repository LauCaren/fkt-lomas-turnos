import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useSelector } from 'react-redux';
import Layout from './components/Layout';
import { ConfirmProvider } from './components/ui/ConfirmDialog';
import { ToastProvider } from './components/ui/ToastProvider';
import { RutaPaciente, RutaAdmin, RutaSecretaria, RutaMedico, RutaContador } from './components/RutaProtegida';
import LoginPage from './features/auth/LoginPage';
import RegisterPage from './features/auth/RegisterPage';
import OlvidePasswordPage from './features/auth/OlvidePasswordPage';
import RestablecerPasswordPage from './features/auth/RestablecerPasswordPage';
import BookingWizard from './features/turnos/BookingWizard';
import MisTurnosPage from './features/turnos/MisTurnosPage';
import MiCuentaPage from './features/cuenta/MiCuentaPage';
import ConfirmarTurnoPage from './features/turnos/ConfirmarTurnoPage';
import CheckInPage from './features/turnos/CheckInPage';
import { RUTA_POR_ROL } from './config';

// Cada panel de staff (admin/secretaria/médico/contador) y el módulo de kinesiología son
// pantallas grandes que un paciente reservando un turno nunca visita — separarlos en su
// propio chunk evita que ese paciente baje ese JS en el primer load.
const AdminDashboardPage = lazy(() => import('./features/admin/AdminDashboardPage'));
const SecretariaDashboardPage = lazy(() => import('./features/secretaria/SecretariaDashboardPage'));
const MedicoDashboardPage = lazy(() => import('./features/medico/MedicoDashboardPage'));
const ContadorDashboardPage = lazy(() => import('./features/contador/ContadorDashboardPage'));
const KinesiologiaPage = lazy(() => import('./features/kinesiologia/KinesiologiaPage'));

function InicioRedirect() {
  const usuario = useSelector((state) => state.auth.usuario);
  if (!usuario) return <Navigate to="/login" replace />;
  return <Navigate to={RUTA_POR_ROL[usuario.role] || '/turnos'} replace />;
}

function CargandoPagina() {
  return (
    <div style={{ textAlign: 'center', padding: '80px 0', color: 'var(--muted)' }}>
      Cargando…
    </div>
  );
}

export default function App() {
  return (
    <ToastProvider>
      <ConfirmProvider>
        <Layout>
          <Suspense fallback={<CargandoPagina />}>
            <Routes>
              <Route path="/" element={<InicioRedirect />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/registro" element={<RegisterPage />} />
              <Route path="/olvide-password" element={<OlvidePasswordPage />} />
              <Route path="/restablecer-password" element={<RestablecerPasswordPage />} />
              <Route path="/confirmar-turno" element={<ConfirmarTurnoPage />} />
              <Route path="/turnos" element={<RutaPaciente><BookingWizard /></RutaPaciente>} />
              <Route path="/turnos/kinesiologia" element={<RutaPaciente><KinesiologiaPage /></RutaPaciente>} />
              <Route path="/turnos/mis-turnos" element={<RutaPaciente><MisTurnosPage /></RutaPaciente>} />
              <Route path="/turnos/mi-cuenta" element={<RutaPaciente><MiCuentaPage /></RutaPaciente>} />
              <Route path="/checkin" element={<RutaPaciente><CheckInPage /></RutaPaciente>} />
              <Route path="/admin" element={<RutaAdmin><AdminDashboardPage /></RutaAdmin>} />
              <Route path="/secretaria" element={<RutaSecretaria><SecretariaDashboardPage /></RutaSecretaria>} />
              <Route path="/medico" element={<RutaMedico><MedicoDashboardPage /></RutaMedico>} />
              <Route path="/contador" element={<RutaContador><ContadorDashboardPage /></RutaContador>} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </Layout>
      </ConfirmProvider>
    </ToastProvider>
  );
}
