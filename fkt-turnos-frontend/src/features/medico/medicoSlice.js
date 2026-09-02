import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

export const fetchAgendaHoy = createAsyncThunk('medico/fetchAgendaHoy', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/medico/agenda-hoy');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchColaEsperaMedico = createAsyncThunk('medico/fetchColaEspera', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/medico/cola-espera');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const marcarAtendido = createAsyncThunk('medico/marcarAtendido', async (turnoId, { rejectWithValue }) => {
  try {
    await axiosClient.put(`/api/medico/turnos/${turnoId}/atendido`);
    return turnoId;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo marcar como atendido'));
  }
});

export const crearEvolucion = createAsyncThunk('medico/crearEvolucion', async ({ turnoId, texto }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/medico/turnos/${turnoId}/evolucion`, { texto });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo guardar la evolución'));
  }
});

export const fetchEvolucionesDePaciente = createAsyncThunk('medico/fetchEvoluciones', async (pacienteId, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get(`/api/medico/pacientes/${pacienteId}/evoluciones`);
    return { pacienteId, evoluciones: data };
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchSaldoMedico = createAsyncThunk('medico/fetchSaldo', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/medico/caja/saldo');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

const medicoSlice = createSlice({
  name: 'medico',
  initialState: {
    agendaHoy: [],
    agendaCargando: false,
    colaEspera: [],
    colaEsperaCargando: false,
    // Turno recién marcado "atendido" desde la cola de espera, a la espera de su evolución.
    // Se guarda aparte (no adentro de colaEspera) porque esa lista se pisa entera cada 10s con
    // el polling del backend (que solo devuelve turnos LLEGO) — si dejáramos el turno atendido
    // ahí, el próximo poll podía hacerlo desaparecer de la pantalla a mitad de que el médico
    // esté escribiendo la nota, antes de guardarla.
    turnoParaEvolucion: null,
    evolucionesPorPaciente: {},
    saldo: null,
    error: null,
    mensajeExito: null,
  },
  reducers: {
    limpiarMensajesMedico(state) {
      state.error = null;
      state.mensajeExito = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAgendaHoy.pending, (state) => { state.agendaCargando = true; })
      .addCase(fetchAgendaHoy.fulfilled, (state, action) => { state.agendaHoy = action.payload; state.agendaCargando = false; })
      .addCase(fetchAgendaHoy.rejected, (state, action) => { state.error = action.payload; state.agendaCargando = false; })
      .addCase(fetchColaEsperaMedico.pending, (state) => { state.colaEsperaCargando = true; })
      .addCase(fetchColaEsperaMedico.fulfilled, (state, action) => { state.colaEspera = action.payload; state.colaEsperaCargando = false; })
      .addCase(fetchColaEsperaMedico.rejected, (state, action) => { state.error = action.payload; state.colaEsperaCargando = false; })
      .addCase(marcarAtendido.fulfilled, (state, action) => {
        const turno = state.colaEspera.find((t) => t.id === action.payload);
        state.colaEspera = state.colaEspera.filter((t) => t.id !== action.payload);
        state.agendaHoy = state.agendaHoy.map((t) => t.id === action.payload ? { ...t, estadoAsistencia: 'ATENDIDO' } : t);
        if (turno) state.turnoParaEvolucion = { ...turno, estadoAsistencia: 'ATENDIDO' };
        state.mensajeExito = 'Paciente marcado como atendido';
      })
      .addCase(marcarAtendido.rejected, (state, action) => { state.error = action.payload; })
      .addCase(crearEvolucion.fulfilled, (state, action) => {
        state.agendaHoy = state.agendaHoy.map((t) => t.id === action.payload.turnoId ? { ...t, tieneEvolucion: true } : t);
        if (state.turnoParaEvolucion?.id === action.payload.turnoId) state.turnoParaEvolucion = null;
        state.mensajeExito = 'Evolución guardada';
      })
      .addCase(crearEvolucion.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchEvolucionesDePaciente.fulfilled, (state, action) => {
        state.evolucionesPorPaciente[action.payload.pacienteId] = action.payload.evoluciones;
      })
      .addCase(fetchEvolucionesDePaciente.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchSaldoMedico.fulfilled, (state, action) => { state.saldo = action.payload; })
      .addCase(fetchSaldoMedico.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajesMedico } = medicoSlice.actions;
export default medicoSlice.reducer;
