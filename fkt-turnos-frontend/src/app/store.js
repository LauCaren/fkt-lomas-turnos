import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../features/auth/authSlice';
import turnosReducer from '../features/turnos/turnosSlice';
import adminReducer from '../features/admin/adminSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    turnos: turnosReducer,
    admin: adminReducer,
  },
});
