import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

export const fetchEspecialidades = createAsyncThunk(
  'turnos/fetchEspecialidades',
  async (_, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.get('/api/especialidades');
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error));
    }
  }
);

export const fetchMedicos = createAsyncThunk(
  'turnos/fetchMedicos',
  async (especialidadId, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.get('/api/medicos', { params: { especialidadId } });
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error));
    }
  }
);

export const fetchTurnosDisponibles = createAsyncThunk(
  'turnos/fetchTurnosDisponibles',
  async ({ medicoId, fecha }, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.get(`/api/medicos/${medicoId}/turnos-disponibles`, { params: { fecha } });
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error));
    }
  }
);

export const reservarTurno = createAsyncThunk(
  'turnos/reservarTurno',
  async ({ turnoId, motivoConsulta }, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post(`/api/turnos/${turnoId}/reservar`, { motivoConsulta });
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error, 'Ese turno ya no está disponible'));
    }
  }
);

export const fetchMisTurnos = createAsyncThunk(
  'turnos/fetchMisTurnos',
  async (_, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.get('/api/turnos/mios');
      return data;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error));
    }
  }
);

export const cancelarTurno = createAsyncThunk(
  'turnos/cancelarTurno',
  async (turnoId, { rejectWithValue }) => {
    try {
      await axiosClient.delete(`/api/turnos/${turnoId}`);
      return turnoId;
    } catch (error) {
      return rejectWithValue(mensajeDeError(error));
    }
  }
);

const turnosSlice = createSlice({
  name: 'turnos',
  initialState: {
    especialidades: [],
    medicos: [],
    disponibles: [],
    misTurnos: [],
    status: 'idle',
    error: null,
    reservaExitosa: null,
  },
  reducers: {
    limpiarReserva(state) {
      state.reservaExitosa = null;
      state.error = null;
    },
    limpiarDisponibles(state) {
      state.disponibles = [];
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchEspecialidades.fulfilled, (state, action) => { state.especialidades = action.payload; })
      .addCase(fetchMedicos.fulfilled, (state, action) => { state.medicos = action.payload; })
      .addCase(fetchTurnosDisponibles.pending, (state) => { state.status = 'loading'; })
      .addCase(fetchTurnosDisponibles.fulfilled, (state, action) => {
        state.status = 'idle';
        state.disponibles = action.payload;
      })
      .addCase(fetchTurnosDisponibles.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload;
      })
      .addCase(reservarTurno.pending, (state) => { state.status = 'loading'; state.error = null; })
      .addCase(reservarTurno.fulfilled, (state, action) => {
        state.status = 'idle';
        state.reservaExitosa = action.payload;
        state.disponibles = state.disponibles.filter((t) => t.id !== action.payload.id);
      })
      .addCase(reservarTurno.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload;
      })
      .addCase(fetchMisTurnos.fulfilled, (state, action) => { state.misTurnos = action.payload; })
      .addCase(cancelarTurno.fulfilled, (state, action) => {
        state.misTurnos = state.misTurnos.map((t) => t.id === action.payload ? { ...t, estado: 'CANCELADO' } : t);
      });
  },
});

export const { limpiarReserva, limpiarDisponibles } = turnosSlice.actions;
export default turnosSlice.reducer;
