import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

export const fetchMiPerfil = createAsyncThunk('cuenta/fetchPerfil', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/mi-cuenta');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const actualizarPerfil = createAsyncThunk('cuenta/actualizarPerfil', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put('/api/mi-cuenta', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudieron guardar los cambios'));
  }
});

export const cambiarPasswordCuenta = createAsyncThunk('cuenta/cambiarPassword', async (payload, { rejectWithValue }) => {
  try {
    await axiosClient.put('/api/mi-cuenta/password', payload);
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo cambiar la contraseña'));
  }
});

export const iniciarMfaCuenta = createAsyncThunk('cuenta/iniciarMfa', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/mi-cuenta/mfa/iniciar');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const activarMfaCuenta = createAsyncThunk('cuenta/activarMfa', async (codigo, { rejectWithValue }) => {
  try {
    await axiosClient.post('/api/mi-cuenta/mfa/activar', { codigo });
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const desactivarMfaCuenta = createAsyncThunk('cuenta/desactivarMfa', async (codigo, { rejectWithValue }) => {
  try {
    await axiosClient.post('/api/mi-cuenta/mfa/desactivar', { codigo });
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

// Disponible para cualquier rol logueado (paciente, admin, secretaria, médico, contador) —
// el endpoint no distingue por rol, ver UsuarioController. Cierra también la sesión actual
// (fuerza a loguearse de nuevo), no solo las "otras" — el componente que la usa debe
// desloguear localmente después de que esto resuelva.
export const revocarMisSesiones = createAsyncThunk('cuenta/revocarMisSesiones', async (_, { rejectWithValue }) => {
  try {
    await axiosClient.put('/api/mi-cuenta/revocar-sesiones');
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudieron cerrar las sesiones'));
  }
});

export const fetchMisEvoluciones = createAsyncThunk('cuenta/fetchMisEvoluciones', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/mi-cuenta/evoluciones');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const solicitarBajaCuenta = createAsyncThunk('cuenta/solicitarBaja', async (motivo, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/mi-cuenta/solicitar-baja', { motivo: motivo || undefined });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo registrar el pedido de baja'));
  }
});

export const fetchMisPagos = createAsyncThunk('cuenta/fetchPagos', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/mi-cuenta/pagos');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

const cuentaSlice = createSlice({
  name: 'cuenta',
  initialState: {
    perfil: null,
    mfaSetup: null,
    misEvoluciones: [],
    solicitudBaja: null,
    pagos: [],
    error: null,
    mensajeExito: null,
  },
  reducers: {
    limpiarMensajesCuenta(state) {
      state.error = null;
      state.mensajeExito = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMiPerfil.fulfilled, (state, action) => { state.perfil = action.payload; })
      .addCase(fetchMiPerfil.rejected, (state, action) => { state.error = action.payload; })
      .addCase(actualizarPerfil.fulfilled, (state, action) => {
        state.perfil = action.payload;
        state.mensajeExito = 'Datos actualizados correctamente';
      })
      .addCase(actualizarPerfil.rejected, (state, action) => { state.error = action.payload; })
      .addCase(cambiarPasswordCuenta.fulfilled, (state) => { state.mensajeExito = 'Contraseña actualizada correctamente'; })
      .addCase(cambiarPasswordCuenta.rejected, (state, action) => { state.error = action.payload; })
      .addCase(iniciarMfaCuenta.fulfilled, (state, action) => { state.mfaSetup = action.payload; })
      .addCase(iniciarMfaCuenta.rejected, (state, action) => { state.error = action.payload; })
      .addCase(activarMfaCuenta.fulfilled, (state) => { state.mfaSetup = null; state.mensajeExito = 'Verificación en dos pasos activada'; })
      .addCase(activarMfaCuenta.rejected, (state, action) => { state.error = action.payload; })
      .addCase(desactivarMfaCuenta.fulfilled, (state) => { state.mensajeExito = 'Verificación en dos pasos desactivada'; })
      .addCase(desactivarMfaCuenta.rejected, (state, action) => { state.error = action.payload; })
      .addCase(revocarMisSesiones.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchMisEvoluciones.fulfilled, (state, action) => { state.misEvoluciones = action.payload; })
      .addCase(fetchMisEvoluciones.rejected, (state, action) => { state.error = action.payload; })
      .addCase(solicitarBajaCuenta.fulfilled, (state, action) => {
        state.solicitudBaja = action.payload;
        state.mensajeExito = 'Registramos tu pedido de baja — el centro lo va a revisar y confirmar por email.';
      })
      .addCase(solicitarBajaCuenta.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchMisPagos.fulfilled, (state, action) => { state.pagos = action.payload; })
      .addCase(fetchMisPagos.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajesCuenta } = cuentaSlice.actions;
export default cuentaSlice.reducer;
