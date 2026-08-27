import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

function usuarioGuardado() {
  try {
    const raw = localStorage.getItem('fkt_user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

const initialState = {
  token: localStorage.getItem('fkt_token') || null,
  usuario: usuarioGuardado(),
  status: 'idle',
  error: null,
};

function guardarSesion(data) {
  localStorage.setItem('fkt_token', data.token);
  localStorage.setItem('fkt_user', JSON.stringify({
    nombreCompleto: data.nombreCompleto,
    email: data.email,
    role: data.role,
  }));
}

export const registrarPaciente = createAsyncThunk(
  'auth/registrar',
  async (datos, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post('/api/auth/registro', datos);
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error, 'No pudimos crear tu cuenta'));
    }
  }
);

export const login = createAsyncThunk(
  'auth/login',
  async ({ email, password }, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post('/api/auth/login', { email, password });
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error, 'Email o contraseña incorrectos'));
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      state.token = null;
      state.usuario = null;
      localStorage.removeItem('fkt_token');
      localStorage.removeItem('fkt_user');
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(registrarPaciente.pending, (state) => { state.status = 'loading'; state.error = null; })
      .addCase(registrarPaciente.fulfilled, (state, action) => {
        state.status = 'idle';
        state.token = action.payload.token;
        state.usuario = { nombreCompleto: action.payload.nombreCompleto, email: action.payload.email, role: action.payload.role };
        guardarSesion(action.payload);
      })
      .addCase(registrarPaciente.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload;
      })
      .addCase(login.pending, (state) => { state.status = 'loading'; state.error = null; })
      .addCase(login.fulfilled, (state, action) => {
        state.status = 'idle';
        state.token = action.payload.token;
        state.usuario = { nombreCompleto: action.payload.nombreCompleto, email: action.payload.email, role: action.payload.role };
        guardarSesion(action.payload);
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload;
      });
  },
});

export const { logout } = authSlice.actions;
export default authSlice.reducer;
