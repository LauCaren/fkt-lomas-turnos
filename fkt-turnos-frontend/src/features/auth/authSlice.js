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
  requiereMfa: false,
  mensajeExito: null,
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
  async ({ email, password, codigoMfa }, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post('/api/auth/login', { email, password, codigoMfa });
      return data;
    } catch (error) {
      return rejectWithValue({
        mensaje: mensajeDeError(error, 'Email o contraseña incorrectos'),
        requiereMfa: !!error.response?.data?.requiereMfa,
      });
    }
  }
);

export const olvidePassword = createAsyncThunk(
  'auth/olvidePassword',
  async (email, { rejectWithValue }) => {
    try {
      await axiosClient.post('/api/auth/olvide-password', { email });
      return true;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error, 'No se pudo procesar el pedido'));
    }
  }
);

export const restablecerPassword = createAsyncThunk(
  'auth/restablecerPassword',
  async ({ token, passwordNueva }, { rejectWithValue }) => {
    try {
      await axiosClient.post('/api/auth/restablecer-password', { token, passwordNueva });
      return true;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error, 'No se pudo restablecer la contraseña'));
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
    actualizarUsuarioLocal(state, action) {
      state.usuario = { ...state.usuario, ...action.payload };
      localStorage.setItem('fkt_user', JSON.stringify(state.usuario));
    },
    limpiarMensajesAuth(state) {
      state.error = null;
      state.mensajeExito = null;
      state.requiereMfa = false;
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
        state.requiereMfa = false;
        guardarSesion(action.payload);
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload.mensaje;
        state.requiereMfa = action.payload.requiereMfa;
      })
      .addCase(olvidePassword.pending, (state) => { state.status = 'loading'; state.error = null; state.mensajeExito = null; })
      .addCase(olvidePassword.fulfilled, (state) => {
        state.status = 'idle';
        state.mensajeExito = 'Si el email existe en nuestro sistema, te enviamos un link para restablecer tu contraseña.';
      })
      .addCase(olvidePassword.rejected, (state, action) => { state.status = 'idle'; state.error = action.payload; })
      .addCase(restablecerPassword.pending, (state) => { state.status = 'loading'; state.error = null; state.mensajeExito = null; })
      .addCase(restablecerPassword.fulfilled, (state) => {
        state.status = 'idle';
        state.mensajeExito = 'Contraseña actualizada. Ya podés ingresar con tu nueva contraseña.';
      })
      .addCase(restablecerPassword.rejected, (state, action) => { state.status = 'idle'; state.error = action.payload; });
  },
});

export const { logout, actualizarUsuarioLocal, limpiarMensajesAuth } = authSlice.actions;
export default authSlice.reducer;
