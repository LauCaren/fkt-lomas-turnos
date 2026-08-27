import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

export const fetchMedicosAdmin = createAsyncThunk('admin/fetchMedicos', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/medicos');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearEspecialidad = createAsyncThunk('admin/crearEspecialidad', async (nombre, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/especialidades', { nombre });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearMedico = createAsyncThunk('admin/crearMedico', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/medicos', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const actualizarMedico = createAsyncThunk('admin/actualizarMedico', async ({ id, payload }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/medicos/${id}`, payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchReglas = createAsyncThunk('admin/fetchReglas', async (medicoId, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get(`/api/admin/medicos/${medicoId}/reglas-disponibilidad`);
    return { medicoId, reglas: data };
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearRegla = createAsyncThunk('admin/crearRegla', async ({ medicoId, payload }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/admin/medicos/${medicoId}/reglas-disponibilidad`, payload);
    return { medicoId, regla: data };
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const eliminarRegla = createAsyncThunk('admin/eliminarRegla', async ({ medicoId, reglaId }, { rejectWithValue }) => {
  try {
    await axiosClient.delete(`/api/admin/medicos/reglas-disponibilidad/${reglaId}`);
    return { medicoId, reglaId };
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const generarTurnos = createAsyncThunk('admin/generarTurnos', async ({ medicoId, desde, hasta }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/admin/medicos/${medicoId}/generar-turnos`, { desde, hasta });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchMensajesContacto = createAsyncThunk('admin/fetchMensajes', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/mensajes-contacto');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const marcarMensajeLeido = createAsyncThunk('admin/marcarMensajeLeido', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.patch(`/api/admin/mensajes-contacto/${id}/leido`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchPostulaciones = createAsyncThunk('admin/fetchPostulaciones', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/postulaciones');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const marcarPostulacionRevisada = createAsyncThunk('admin/marcarPostulacionRevisada', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.patch(`/api/admin/postulaciones/${id}/revisado`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const cambiarPasswordAdmin = createAsyncThunk('admin/cambiarPassword', async (payload, { rejectWithValue }) => {
  try {
    await axiosClient.put('/api/admin/password', payload);
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo cambiar la contraseña'));
  }
});

const adminSlice = createSlice({
  name: 'admin',
  initialState: {
    medicos: [],
    reglasPorMedico: {},
    mensajesContacto: [],
    postulaciones: [],
    error: null,
    mensajeExito: null,
  },
  reducers: {
    limpiarMensajes(state) {
      state.error = null;
      state.mensajeExito = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMedicosAdmin.fulfilled, (state, action) => { state.medicos = action.payload; })
      .addCase(crearMedico.fulfilled, (state, action) => { state.medicos.push(action.payload); })
      .addCase(crearMedico.rejected, (state, action) => { state.error = action.payload; })
      .addCase(actualizarMedico.fulfilled, (state, action) => {
        state.medicos = state.medicos.map((m) => m.id === action.payload.id ? action.payload : m);
      })
      .addCase(crearEspecialidad.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchReglas.fulfilled, (state, action) => {
        state.reglasPorMedico[action.payload.medicoId] = action.payload.reglas;
      })
      .addCase(crearRegla.fulfilled, (state, action) => {
        const lista = state.reglasPorMedico[action.payload.medicoId] || [];
        state.reglasPorMedico[action.payload.medicoId] = [...lista, action.payload.regla];
      })
      .addCase(crearRegla.rejected, (state, action) => { state.error = action.payload; })
      .addCase(eliminarRegla.fulfilled, (state, action) => {
        const lista = state.reglasPorMedico[action.payload.medicoId] || [];
        state.reglasPorMedico[action.payload.medicoId] = lista.filter((r) => r.id !== action.payload.reglaId);
      })
      .addCase(generarTurnos.fulfilled, (state, action) => {
        state.mensajeExito = `Se generaron ${action.payload.turnosGenerados} turnos nuevos`;
      })
      .addCase(generarTurnos.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchMensajesContacto.fulfilled, (state, action) => { state.mensajesContacto = action.payload; })
      .addCase(marcarMensajeLeido.fulfilled, (state, action) => {
        state.mensajesContacto = state.mensajesContacto.map((m) => m.id === action.payload.id ? action.payload : m);
      })
      .addCase(fetchPostulaciones.fulfilled, (state, action) => { state.postulaciones = action.payload; })
      .addCase(marcarPostulacionRevisada.fulfilled, (state, action) => {
        state.postulaciones = state.postulaciones.map((p) => p.id === action.payload.id ? action.payload : p);
      })
      .addCase(cambiarPasswordAdmin.fulfilled, (state) => { state.mensajeExito = 'Contraseña actualizada correctamente'; })
      .addCase(cambiarPasswordAdmin.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajes } = adminSlice.actions;
export default adminSlice.reducer;
