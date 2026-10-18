import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

// Estado de Redux de los planes de kinesiología: del lado del paciente (pedir un plan, pausarlo,
// reanudarlo, cancelarlo) y del lado del centro (listar, cargar por teléfono, validar, rechazar).
// Ver KinesiologiaService en el backend para las reglas de cada transición de estado.

export const fetchMisPlanesKinesiologia = createAsyncThunk('kinesiologia/fetchMisPlanes', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/kinesiologia/planes');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchHorariosDisponiblesKinesiologia = createAsyncThunk('kinesiologia/fetchHorariosDisponibles', async (medicoId, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/kinesiologia/horarios-disponibles', { params: medicoId ? { medicoId } : {} });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearPlanKinesiologia = createAsyncThunk('kinesiologia/crearPlan', async (formData, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/kinesiologia/planes', formData);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo crear el plan'));
  }
});

export const cancelarPlanKinesiologia = createAsyncThunk('kinesiologia/cancelarPlan', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/kinesiologia/planes/${id}/cancelar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo cancelar el plan'));
  }
});

export const pausarPlanKinesiologia = createAsyncThunk('kinesiologia/pausarPlan', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/kinesiologia/planes/${id}/pausar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo pausar el plan'));
  }
});

export const reanudarPlanKinesiologia = createAsyncThunk('kinesiologia/reanudarPlan', async ({ id, fechaInicio }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/kinesiologia/planes/${id}/reanudar`, null, { params: { fechaInicio: fechaInicio || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo reanudar el plan'));
  }
});

export const fetchPlanesKinesiologiaAdmin = createAsyncThunk('kinesiologia/fetchPlanesAdmin', async (estado, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/kinesiologia/planes', { params: { estado: estado || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearPlanKinesiologiaPorTelefono = createAsyncThunk('kinesiologia/crearPlanPorTelefono', async (formData, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/kinesiologia/planes', formData);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo crear el plan'));
  }
});

export const validarPlanKinesiologia = createAsyncThunk('kinesiologia/validarPlan', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/kinesiologia/planes/${id}/validar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo validar el plan'));
  }
});

export const rechazarPlanKinesiologia = createAsyncThunk('kinesiologia/rechazarPlan', async ({ id, motivo }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/kinesiologia/planes/${id}/rechazar`, { motivo });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo rechazar el plan'));
  }
});

export const cancelarPlanKinesiologiaAdmin = createAsyncThunk('kinesiologia/cancelarPlanAdmin', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/kinesiologia/planes/${id}/cancelar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo cancelar el plan'));
  }
});

const kinesiologiaSlice = createSlice({
  name: 'kinesiologia',
  initialState: {
    misPlanes: [],
    planesAdmin: [],
    horariosDisponibles: [],
    cargandoHorarios: false,
    error: null,
    mensajeExito: null,
  },
  reducers: {
    limpiarMensajesKinesiologia(state) {
      state.error = null;
      state.mensajeExito = null;
    },
    limpiarHorariosDisponibles(state) {
      state.horariosDisponibles = [];
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMisPlanesKinesiologia.fulfilled, (state, action) => { state.misPlanes = action.payload; })
      .addCase(fetchMisPlanesKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchHorariosDisponiblesKinesiologia.pending, (state) => { state.cargandoHorarios = true; })
      .addCase(fetchHorariosDisponiblesKinesiologia.fulfilled, (state, action) => {
        state.horariosDisponibles = action.payload;
        state.cargandoHorarios = false;
      })
      .addCase(fetchHorariosDisponiblesKinesiologia.rejected, (state, action) => {
        state.error = action.payload;
        state.cargandoHorarios = false;
      })
      .addCase(crearPlanKinesiologia.fulfilled, (state, action) => {
        state.misPlanes = [action.payload, ...state.misPlanes];
        state.mensajeExito = 'Plan enviado — un admin va a revisar la orden médica antes de habilitar los turnos';
      })
      .addCase(crearPlanKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(cancelarPlanKinesiologia.fulfilled, (state, action) => {
        state.misPlanes = state.misPlanes.map((p) => p.id === action.payload.id ? action.payload : p);
      })
      .addCase(cancelarPlanKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(pausarPlanKinesiologia.fulfilled, (state, action) => {
        state.misPlanes = state.misPlanes.map((p) => p.id === action.payload.id ? action.payload : p);
      })
      .addCase(pausarPlanKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(reanudarPlanKinesiologia.fulfilled, (state, action) => {
        state.misPlanes = state.misPlanes.map((p) => p.id === action.payload.id ? action.payload : p);
      })
      .addCase(reanudarPlanKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchPlanesKinesiologiaAdmin.fulfilled, (state, action) => { state.planesAdmin = action.payload; })
      .addCase(fetchPlanesKinesiologiaAdmin.rejected, (state, action) => { state.error = action.payload; })
      .addCase(crearPlanKinesiologiaPorTelefono.fulfilled, (state, action) => {
        state.planesAdmin = [action.payload, ...state.planesAdmin];
        state.mensajeExito = 'Plan cargado correctamente';
      })
      .addCase(crearPlanKinesiologiaPorTelefono.rejected, (state, action) => { state.error = action.payload; })
      .addCase(validarPlanKinesiologia.fulfilled, (state, action) => {
        state.planesAdmin = state.planesAdmin.map((p) => p.id === action.payload.id ? action.payload : p);
        state.mensajeExito = `Plan validado — se generaron ${action.payload.sesionesGeneradas} de ${action.payload.cantidadSesiones} sesiones`;
      })
      .addCase(validarPlanKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(rechazarPlanKinesiologia.fulfilled, (state, action) => {
        state.planesAdmin = state.planesAdmin.map((p) => p.id === action.payload.id ? action.payload : p);
        state.mensajeExito = 'Plan rechazado';
      })
      .addCase(rechazarPlanKinesiologia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(cancelarPlanKinesiologiaAdmin.fulfilled, (state, action) => {
        state.planesAdmin = state.planesAdmin.map((p) => p.id === action.payload.id ? action.payload : p);
      })
      .addCase(cancelarPlanKinesiologiaAdmin.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajesKinesiologia, limpiarHorariosDisponibles } = kinesiologiaSlice.actions;
export default kinesiologiaSlice.reducer;
