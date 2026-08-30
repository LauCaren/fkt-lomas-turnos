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

const cuentaSlice = createSlice({
  name: 'cuenta',
  initialState: {
    perfil: null,
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
      .addCase(cambiarPasswordCuenta.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajesCuenta } = cuentaSlice.actions;
export default cuentaSlice.reducer;
