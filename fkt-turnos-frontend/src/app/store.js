import { combineReducers, configureStore } from '@reduxjs/toolkit';
import authReducer, { logout } from '../features/auth/authSlice';
import turnosReducer from '../features/turnos/turnosSlice';
import adminReducer from '../features/admin/adminSlice';
import cuentaReducer from '../features/cuenta/cuentaSlice';
import medicoReducer from '../features/medico/medicoSlice';
import cajaReducer from '../features/caja/cajaSlice';
import kinesiologiaReducer from '../features/kinesiologia/kinesiologiaSlice';
import contadorReducer from '../features/contador/contadorSlice';

const appReducer = combineReducers({
  auth: authReducer,
  turnos: turnosReducer,
  admin: adminReducer,
  cuenta: cuentaReducer,
  medico: medicoReducer,
  caja: cajaReducer,
  kinesiologia: kinesiologiaReducer,
  contador: contadorReducer,
});

// Al cerrar sesión se resetea TODO el store (no solo auth), para que en una
// computadora compartida (p. ej. la recepción) la siguiente persona que entre
// no vea datos cacheados de la sesión anterior.
function rootReducer(state, action) {
  if (action.type === logout.type) {
    state = undefined;
  }
  return appReducer(state, action);
}

export const store = configureStore({
  reducer: rootReducer,
});
