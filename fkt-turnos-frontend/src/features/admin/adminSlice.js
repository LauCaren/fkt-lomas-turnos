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

export const fetchAgenda = createAsyncThunk('admin/fetchAgenda', async ({ fecha, desde, hasta, medicoId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/turnos', {
      params: { desde: desde || fecha, hasta: hasta || fecha, medicoId: medicoId || undefined },
    });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchFeriados = createAsyncThunk('admin/fetchFeriados', async ({ desde, hasta }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/feriados', { params: { desde, hasta } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const reservarParaPaciente = createAsyncThunk('admin/reservarParaPaciente', async ({ turnoId, payload }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/admin/turnos/${turnoId}/reservar-para-paciente`, payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo reservar el turno'));
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

export const iniciarMfa = createAsyncThunk('admin/iniciarMfa', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/mfa/iniciar');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo iniciar la configuración'));
  }
});

export const activarMfa = createAsyncThunk('admin/activarMfa', async (codigo, { rejectWithValue }) => {
  try {
    await axiosClient.post('/api/admin/mfa/activar', { codigo });
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'El código no es correcto'));
  }
});

export const desactivarMfa = createAsyncThunk('admin/desactivarMfa', async (codigo, { rejectWithValue }) => {
  try {
    await axiosClient.post('/api/admin/mfa/desactivar', { codigo });
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'El código no es correcto'));
  }
});

export const fetchRecordatorios = createAsyncThunk('admin/fetchRecordatorios', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/recordatorios');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const reenviarRecordatorio = createAsyncThunk('admin/reenviarRecordatorio', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/admin/recordatorios/${id}/reenviar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo reenviar el recordatorio'));
  }
});

export const fetchConfiguracionRecordatorios = createAsyncThunk('admin/fetchConfiguracionRecordatorios', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/recordatorios/configuracion');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const actualizarConfiguracionRecordatorios = createAsyncThunk('admin/actualizarConfiguracionRecordatorios', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put('/api/admin/recordatorios/configuracion', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo guardar la configuración'));
  }
});

export const buscarPacientes = createAsyncThunk('admin/buscarPacientes', async (buscar, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/recordatorios/pacientes', { params: { buscar } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const actualizarCanalPreferido = createAsyncThunk('admin/actualizarCanalPreferido', async ({ pacienteId, canal }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/recordatorios/pacientes/${pacienteId}/canal-preferido`, { canal });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo actualizar el canal preferido'));
  }
});

export const enviarMensajeManual = createAsyncThunk('admin/enviarMensajeManual', async (payload, { rejectWithValue }) => {
  try {
    await axiosClient.post('/api/admin/recordatorios/mensajes-manuales', payload);
    return true;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo enviar el mensaje'));
  }
});

const adminSlice = createSlice({
  name: 'admin',
  initialState: {
    medicos: [],
    reglasPorMedico: {},
    agenda: [],
    agendaCargando: false,
    feriados: [],
    mfaSetup: null,
    mensajesContacto: [],
    postulaciones: [],
    recordatorios: [],
    configuracionRecordatorios: null,
    resultadosBusquedaPacientes: [],
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
      .addCase(fetchAgenda.pending, (state) => { state.agendaCargando = true; })
      .addCase(fetchAgenda.fulfilled, (state, action) => { state.agenda = action.payload; state.agendaCargando = false; })
      .addCase(fetchAgenda.rejected, (state, action) => { state.error = action.payload; state.agendaCargando = false; })
      .addCase(reservarParaPaciente.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.id ? action.payload : t);
        state.mensajeExito = 'Turno reservado correctamente';
      })
      .addCase(reservarParaPaciente.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchFeriados.fulfilled, (state, action) => { state.feriados = action.payload; })
      .addCase(fetchFeriados.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchMensajesContacto.fulfilled, (state, action) => { state.mensajesContacto = action.payload; })
      .addCase(marcarMensajeLeido.fulfilled, (state, action) => {
        state.mensajesContacto = state.mensajesContacto.map((m) => m.id === action.payload.id ? action.payload : m);
      })
      .addCase(fetchPostulaciones.fulfilled, (state, action) => { state.postulaciones = action.payload; })
      .addCase(marcarPostulacionRevisada.fulfilled, (state, action) => {
        state.postulaciones = state.postulaciones.map((p) => p.id === action.payload.id ? action.payload : p);
      })
      .addCase(cambiarPasswordAdmin.fulfilled, (state) => { state.mensajeExito = 'Contraseña actualizada correctamente'; })
      .addCase(cambiarPasswordAdmin.rejected, (state, action) => { state.error = action.payload; })
      .addCase(iniciarMfa.fulfilled, (state, action) => { state.mfaSetup = action.payload; })
      .addCase(iniciarMfa.rejected, (state, action) => { state.error = action.payload; })
      .addCase(activarMfa.fulfilled, (state) => { state.mfaSetup = null; state.mensajeExito = 'Verificación en dos pasos activada'; })
      .addCase(activarMfa.rejected, (state, action) => { state.error = action.payload; })
      .addCase(desactivarMfa.fulfilled, (state) => { state.mensajeExito = 'Verificación en dos pasos desactivada'; })
      .addCase(desactivarMfa.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchRecordatorios.fulfilled, (state, action) => { state.recordatorios = action.payload; })
      .addCase(fetchRecordatorios.rejected, (state, action) => { state.error = action.payload; })
      .addCase(reenviarRecordatorio.fulfilled, (state, action) => {
        state.recordatorios = state.recordatorios.map((r) => r.id === action.payload.id ? action.payload : r);
        state.mensajeExito = action.payload.estado === 'ENVIADO' ? 'Recordatorio reenviado correctamente' : 'Se reintentó el envío, pero volvió a fallar';
      })
      .addCase(reenviarRecordatorio.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchConfiguracionRecordatorios.fulfilled, (state, action) => { state.configuracionRecordatorios = action.payload; })
      .addCase(fetchConfiguracionRecordatorios.rejected, (state, action) => { state.error = action.payload; })
      .addCase(actualizarConfiguracionRecordatorios.fulfilled, (state, action) => {
        state.configuracionRecordatorios = action.payload;
        state.mensajeExito = 'Configuración de recordatorios guardada';
      })
      .addCase(actualizarConfiguracionRecordatorios.rejected, (state, action) => { state.error = action.payload; })
      .addCase(buscarPacientes.fulfilled, (state, action) => { state.resultadosBusquedaPacientes = action.payload; })
      .addCase(buscarPacientes.rejected, (state, action) => { state.error = action.payload; })
      .addCase(actualizarCanalPreferido.fulfilled, (state, action) => {
        state.resultadosBusquedaPacientes = state.resultadosBusquedaPacientes.map((p) => p.id === action.payload.id ? action.payload : p);
        state.mensajeExito = 'Canal preferido actualizado';
      })
      .addCase(actualizarCanalPreferido.rejected, (state, action) => { state.error = action.payload; })
      .addCase(enviarMensajeManual.fulfilled, (state) => { state.mensajeExito = 'Mensaje enviado correctamente'; })
      .addCase(enviarMensajeManual.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajes } = adminSlice.actions;
export default adminSlice.reducer;
