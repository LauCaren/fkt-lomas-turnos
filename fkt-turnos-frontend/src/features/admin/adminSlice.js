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

export const subirFotoMedico = createAsyncThunk('admin/subirFotoMedico', async ({ medicoId, file }, { rejectWithValue }) => {
  try {
    const formData = new FormData();
    formData.append('foto', file);
    const { data } = await axiosClient.post(`/api/admin/medicos/${medicoId}/foto`, formData);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo subir la foto'));
  }
});

export const subirTituloMedico = createAsyncThunk('admin/subirTituloMedico', async ({ medicoId, file }, { rejectWithValue }) => {
  try {
    const formData = new FormData();
    formData.append('imagen', file);
    const { data } = await axiosClient.post(`/api/admin/medicos/${medicoId}/titulo`, formData);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo subir la imagen del título'));
  }
});

export const crearOActualizarCredencialesMedico = createAsyncThunk('admin/crearOActualizarCredencialesMedico', async ({ medicoId, payload }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/medicos/${medicoId}/credenciales`, payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudieron guardar las credenciales'));
  }
});

export const fetchResumenAdmin = createAsyncThunk('admin/fetchResumen', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/resumen');
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

export const fetchAgenda = createAsyncThunk('admin/fetchAgenda', async ({ fecha, desde, hasta, medicoId, especialidadId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/turnos', {
      params: { desde: desde || fecha, hasta: hasta || fecha, medicoId: medicoId || undefined, especialidadId: especialidadId || undefined },
    });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchTurnosOcupados = createAsyncThunk('admin/fetchTurnosOcupados', async ({ desde, hasta, medicoId, especialidadId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/turnos/buscar', {
      params: { desde: desde || undefined, hasta: hasta || undefined, medicoId: medicoId || undefined, especialidadId: especialidadId || undefined, estado: 'RESERVADO', size: 200 },
    });
    // El endpoint devuelve una Page de Spring Data (content, totalElements, etc.), no un array plano.
    return data.content;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchColaEsperaAdmin = createAsyncThunk('admin/fetchColaEspera', async ({ fecha, medicoId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/turnos/cola-espera', { params: { fecha, medicoId: medicoId || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const actualizarAsistencia = createAsyncThunk('admin/actualizarAsistencia', async ({ turnoId, estado }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/turnos/${turnoId}/asistencia`, { estado });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo actualizar la asistencia'));
  }
});

export const deshacerTurno = createAsyncThunk('admin/deshacerTurno', async (turnoId, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/turnos/${turnoId}/deshacer`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo deshacer'));
  }
});

export const fetchListaEsperaCompatibles = createAsyncThunk('admin/fetchListaEsperaCompatibles', async ({ medicoId, fecha }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/lista-espera', { params: { medicoId, fecha } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const asignarTurnoAPaciente = createAsyncThunk('admin/asignarTurnoAPaciente', async ({ turnoId, pacienteId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/admin/turnos/${turnoId}/asignar-paciente`, { pacienteId });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo asignar el turno'));
  }
});

export const enviarRecordatorioAhora = createAsyncThunk('admin/enviarRecordatorioAhora', async (turnoId, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post(`/api/admin/turnos/${turnoId}/recordatorio/enviar`);
    return { turnoId, recordatorio: data };
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo enviar el recordatorio'));
  }
});

export const cancelarTurnoStaff = createAsyncThunk('admin/cancelarTurnoStaff', async (turnoId, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/turnos/${turnoId}/cancelar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo cancelar el turno'));
  }
});

// El backend cancela cada turno en su propia transacción y devuelve {cancelados, errores} — uno
// que ya no esté RESERVADO (alguien lo canceló mientras tanto) no aborta el resto. No devuelve
// el TurnoAdminDto completo de cada uno (a diferencia de cancelarTurnoStaff), así que quien
// llama a esto tiene que volver a pedir la agenda para reflejar el resultado — ver
// AdminAgendaTab.jsx, que ya tiene el fetchAgenda con los parámetros de rango a mano.
export const cancelarTurnosLote = createAsyncThunk('admin/cancelarTurnosLote', async (turnoIds, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put('/api/admin/turnos/cancelar-lote', { turnoIds });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudieron cancelar los turnos'));
  }
});

export const reprogramarTurno = createAsyncThunk('admin/reprogramarTurno', async ({ turnoOrigenId, turnoDestinoId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/turnos/${turnoOrigenId}/reprogramar`, { turnoDestinoId });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo reprogramar el turno'));
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

// page=0 (default) trae la primera tanda y reemplaza la lista — usarlo también para "refrescar".
// Pasar un page > 0 (ver AdminMensajesTab) agrega esa tanda al final en vez de reemplazar.
export const fetchMensajesContacto = createAsyncThunk('admin/fetchMensajes', async (page = 0, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/mensajes-contacto', { params: { page, size: 20 } });
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

// Misma lógica de paginado que fetchMensajesContacto (ver comentario arriba).
export const fetchPostulaciones = createAsyncThunk('admin/fetchPostulaciones', async (page = 0, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/postulaciones', { params: { page, size: 20 } });
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

export const fetchPacientesEnRango = createAsyncThunk('admin/fetchPacientesEnRango', async ({ desde, hasta, medicoId }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/recordatorios/pacientes-en-rango', { params: { desde, hasta, medicoId: medicoId || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const enviarMensajeMasivo = createAsyncThunk('admin/enviarMensajeMasivo', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/recordatorios/mensaje-masivo', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo enviar el aviso'));
  }
});

export const fetchTendenciaAdmin = createAsyncThunk('admin/fetchTendencia', async (dias, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/resumen/tendencia', { params: { dias } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchSecretarias = createAsyncThunk('admin/fetchSecretarias', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/secretarias');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearSecretaria = createAsyncThunk('admin/crearSecretaria', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/secretarias', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo crear la cuenta'));
  }
});

export const activarCuentaStaff = createAsyncThunk('admin/activarCuentaStaff', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/usuarios/${id}/activar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo activar la cuenta'));
  }
});

export const desactivarCuentaStaff = createAsyncThunk('admin/desactivarCuentaStaff', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/usuarios/${id}/desactivar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo desactivar la cuenta'));
  }
});

export const revocarSesionesCuentaStaff = createAsyncThunk('admin/revocarSesionesCuentaStaff', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/usuarios/${id}/revocar-sesiones`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudieron revocar las sesiones'));
  }
});

export const fetchSolicitudesBaja = createAsyncThunk('admin/fetchSolicitudesBaja', async (estado, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/solicitudes-baja', { params: { estado: estado || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const aprobarSolicitudBaja = createAsyncThunk('admin/aprobarSolicitudBaja', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/solicitudes-baja/${id}/aprobar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo aprobar el pedido'));
  }
});

export const rechazarSolicitudBaja = createAsyncThunk('admin/rechazarSolicitudBaja', async ({ id, motivo }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/solicitudes-baja/${id}/rechazar`, { motivo });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo rechazar el pedido'));
  }
});

export const fetchResenas = createAsyncThunk('admin/fetchResenas', async (estado, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/resenas', { params: { estado: estado || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const aprobarResena = createAsyncThunk('admin/aprobarResena', async (id, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/resenas/${id}/aprobar`);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo aprobar la reseña'));
  }
});

export const rechazarResena = createAsyncThunk('admin/rechazarResena', async ({ id, motivo }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/resenas/${id}/rechazar`, { motivo });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo rechazar la reseña'));
  }
});

export const fetchAuditoria = createAsyncThunk('admin/fetchAuditoria', async ({ tipoEntidad, desde, hasta, page } = {}, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/auditoria', {
      params: { tipoEntidad: tipoEntidad || undefined, desde: desde || undefined, hasta: hasta || undefined, page: page || 0 },
    });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

const adminSlice = createSlice({
  name: 'admin',
  initialState: {
    resumen: null,
    medicos: [],
    reglasPorMedico: {},
    agenda: [],
    agendaCargando: false,
    turnosOcupados: [],
    turnosOcupadosCargando: false,
    colaEspera: [],
    colaEsperaCargando: false,
    feriados: [],
    mensajesContacto: [],
    mensajesContactoPagina: 0,
    mensajesContactoHayMas: false,
    postulaciones: [],
    postulacionesPagina: 0,
    postulacionesHayMas: false,
    recordatorios: [],
    configuracionRecordatorios: null,
    resultadosBusquedaPacientes: [],
    pacientesEnRango: [],
    pacientesEnRangoCargando: false,
    tendencia: [],
    secretarias: [],
    solicitudesBaja: [],
    resenas: [],
    auditoria: { content: [], totalPages: 0, number: 0 },
    error: null,
    mensajeExito: null,
    dniPrellenado: null,
    objetivoReprogramar: null,
    pacienteAAbrir: null,
  },
  reducers: {
    limpiarMensajes(state) {
      state.error = null;
      state.mensajeExito = null;
    },
    // "Nuevo turno" desde la ficha de un paciente (AdminBusquedaPacienteTab): deja el DNI acá
    // para que la Agenda lo tome al abrir el formulario de reserva por teléfono, en vez de que
    // la secretaria tenga que volver a escribirlo — se limpia apenas se usa una vez.
    setDniPrellenado(state, action) {
      state.dniPrellenado = action.payload;
    },
    // "Reprogramar" desde la ficha: la Agenda no tiene cargado el turno puntual del paciente
    // (solo lo que esté en el rango/médico visible), así que en vez de intentar abrir el panel
    // de detalle de memoria, se preselecciona médico + fecha y se guía a la secretaria con un
    // aviso a que busque ese turno puntual y toque "Reprogramar" ahí.
    setObjetivoReprogramar(state, action) {
      state.objetivoReprogramar = action.payload;
    },
    // Resultado de paciente elegido en la búsqueda global (Ctrl+K): id de paciente a abrir de
    // una en la pestaña "Buscar paciente" en vez de forzar a repetir la búsqueda ahí.
    setPacienteAAbrir(state, action) {
      state.pacienteAAbrir = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchResumenAdmin.fulfilled, (state, action) => { state.resumen = action.payload; })
      .addCase(fetchResumenAdmin.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchMedicosAdmin.fulfilled, (state, action) => { state.medicos = action.payload; })
      .addCase(crearMedico.fulfilled, (state, action) => { state.medicos.push(action.payload); })
      .addCase(crearMedico.rejected, (state, action) => { state.error = action.payload; })
      .addCase(actualizarMedico.fulfilled, (state, action) => {
        state.medicos = state.medicos.map((m) => m.id === action.payload.id ? action.payload : m);
      })
      .addCase(crearOActualizarCredencialesMedico.fulfilled, (state, action) => {
        state.medicos = state.medicos.map((m) => m.id === action.payload.id ? action.payload : m);
        state.mensajeExito = 'Credenciales del médico guardadas';
      })
      .addCase(crearOActualizarCredencialesMedico.rejected, (state, action) => { state.error = action.payload; })
      .addCase(subirFotoMedico.fulfilled, (state, action) => {
        state.medicos = state.medicos.map((m) => m.id === action.payload.id ? action.payload : m);
        state.mensajeExito = 'Foto actualizada';
      })
      .addCase(subirFotoMedico.rejected, (state, action) => { state.error = action.payload; })
      .addCase(subirTituloMedico.fulfilled, (state, action) => {
        state.medicos = state.medicos.map((m) => m.id === action.payload.id ? action.payload : m);
        state.mensajeExito = 'Imagen del título actualizada';
      })
      .addCase(subirTituloMedico.rejected, (state, action) => { state.error = action.payload; })
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
      .addCase(fetchTurnosOcupados.pending, (state) => { state.turnosOcupadosCargando = true; })
      .addCase(fetchTurnosOcupados.fulfilled, (state, action) => { state.turnosOcupados = action.payload; state.turnosOcupadosCargando = false; })
      .addCase(fetchTurnosOcupados.rejected, (state, action) => { state.error = action.payload; state.turnosOcupadosCargando = false; })
      .addCase(fetchColaEsperaAdmin.pending, (state) => { state.colaEsperaCargando = true; })
      .addCase(fetchColaEsperaAdmin.fulfilled, (state, action) => { state.colaEspera = action.payload; state.colaEsperaCargando = false; })
      .addCase(fetchColaEsperaAdmin.rejected, (state, action) => { state.error = action.payload; state.colaEsperaCargando = false; })
      .addCase(actualizarAsistencia.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.id ? action.payload : t);
        state.colaEspera = action.payload.estadoAsistencia === 'LLEGO'
          ? state.colaEspera
          : state.colaEspera.filter((t) => t.id !== action.payload.id);
      })
      .addCase(actualizarAsistencia.rejected, (state, action) => { state.error = action.payload; })
      .addCase(deshacerTurno.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.id ? action.payload : t);
        state.mensajeExito = 'Deshecho correctamente';
      })
      .addCase(deshacerTurno.rejected, (state, action) => { state.error = action.payload; })
      .addCase(reservarParaPaciente.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.id ? action.payload : t);
        state.mensajeExito = 'Turno reservado correctamente';
      })
      .addCase(reservarParaPaciente.rejected, (state, action) => { state.error = action.payload; })
      .addCase(reprogramarTurno.fulfilled, (state, action) => {
        const { origen, destino } = action.payload;
        state.agenda = state.agenda.map((t) => {
          if (t.id === origen.id) return origen;
          if (t.id === destino.id) return destino;
          return t;
        });
      })
      .addCase(reprogramarTurno.rejected, (state, action) => { state.error = action.payload; })
      .addCase(cancelarTurnoStaff.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.id ? action.payload : t);
      })
      .addCase(cancelarTurnoStaff.rejected, (state, action) => { state.error = action.payload; })
      .addCase(asignarTurnoAPaciente.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.id ? action.payload : t);
      })
      .addCase(asignarTurnoAPaciente.rejected, (state, action) => { state.error = action.payload; })
      .addCase(enviarRecordatorioAhora.fulfilled, (state, action) => {
        state.agenda = state.agenda.map((t) => t.id === action.payload.turnoId
          ? { ...t, estadoRecordatorio: action.payload.recordatorio.estado }
          : t);
      })
      .addCase(enviarRecordatorioAhora.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchFeriados.fulfilled, (state, action) => { state.feriados = action.payload; })
      .addCase(fetchFeriados.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchMensajesContacto.fulfilled, (state, action) => {
        const pagina = action.payload;
        state.mensajesContacto = pagina.number === 0 ? pagina.content : [...state.mensajesContacto, ...pagina.content];
        state.mensajesContactoPagina = pagina.number;
        state.mensajesContactoHayMas = !pagina.last;
      })
      .addCase(marcarMensajeLeido.fulfilled, (state, action) => {
        state.mensajesContacto = state.mensajesContacto.map((m) => m.id === action.payload.id ? action.payload : m);
      })
      .addCase(fetchPostulaciones.fulfilled, (state, action) => {
        const pagina = action.payload;
        state.postulaciones = pagina.number === 0 ? pagina.content : [...state.postulaciones, ...pagina.content];
        state.postulacionesPagina = pagina.number;
        state.postulacionesHayMas = !pagina.last;
      })
      .addCase(marcarPostulacionRevisada.fulfilled, (state, action) => {
        state.postulaciones = state.postulaciones.map((p) => p.id === action.payload.id ? action.payload : p);
      })
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
      .addCase(enviarMensajeManual.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchPacientesEnRango.pending, (state) => { state.pacientesEnRangoCargando = true; })
      .addCase(fetchPacientesEnRango.fulfilled, (state, action) => { state.pacientesEnRango = action.payload; state.pacientesEnRangoCargando = false; })
      .addCase(fetchPacientesEnRango.rejected, (state, action) => { state.error = action.payload; state.pacientesEnRangoCargando = false; })
      .addCase(enviarMensajeMasivo.fulfilled, (state, action) => {
        state.mensajeExito = `Aviso enviado a ${action.payload.enviados} paciente${action.payload.enviados === 1 ? '' : 's'}`;
        state.pacientesEnRango = [];
      })
      .addCase(enviarMensajeMasivo.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchTendenciaAdmin.fulfilled, (state, action) => { state.tendencia = action.payload; })
      .addCase(fetchTendenciaAdmin.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchSecretarias.fulfilled, (state, action) => { state.secretarias = action.payload; })
      .addCase(fetchSecretarias.rejected, (state, action) => { state.error = action.payload; })
      .addCase(crearSecretaria.fulfilled, (state, action) => {
        state.secretarias.push(action.payload);
        state.mensajeExito = 'Cuenta de secretaria creada';
      })
      .addCase(crearSecretaria.rejected, (state, action) => { state.error = action.payload; })
      .addCase(activarCuentaStaff.fulfilled, (state, action) => {
        state.secretarias = state.secretarias.map((s) => s.id === action.payload.id ? action.payload : s);
        state.mensajeExito = 'Cuenta activada';
      })
      .addCase(activarCuentaStaff.rejected, (state, action) => { state.error = action.payload; })
      .addCase(desactivarCuentaStaff.fulfilled, (state, action) => {
        state.secretarias = state.secretarias.map((s) => s.id === action.payload.id ? action.payload : s);
        state.mensajeExito = 'Cuenta desactivada — sus sesiones activas quedaron cerradas';
      })
      .addCase(desactivarCuentaStaff.rejected, (state, action) => { state.error = action.payload; })
      .addCase(revocarSesionesCuentaStaff.fulfilled, (state) => {
        state.mensajeExito = 'Se cerraron todas las sesiones activas de esa cuenta';
      })
      .addCase(revocarSesionesCuentaStaff.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchAuditoria.fulfilled, (state, action) => { state.auditoria = action.payload; })
      .addCase(fetchAuditoria.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchSolicitudesBaja.fulfilled, (state, action) => { state.solicitudesBaja = action.payload; })
      .addCase(fetchSolicitudesBaja.rejected, (state, action) => { state.error = action.payload; })
      .addCase(aprobarSolicitudBaja.fulfilled, (state, action) => {
        state.solicitudesBaja = state.solicitudesBaja.map((s) => s.id === action.payload.id ? action.payload : s);
        state.mensajeExito = 'Pedido aprobado — la cuenta quedó anonimizada';
      })
      .addCase(aprobarSolicitudBaja.rejected, (state, action) => { state.error = action.payload; })
      .addCase(rechazarSolicitudBaja.fulfilled, (state, action) => {
        state.solicitudesBaja = state.solicitudesBaja.map((s) => s.id === action.payload.id ? action.payload : s);
        state.mensajeExito = 'Pedido rechazado';
      })
      .addCase(rechazarSolicitudBaja.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchResenas.fulfilled, (state, action) => { state.resenas = action.payload; })
      .addCase(fetchResenas.rejected, (state, action) => { state.error = action.payload; })
      .addCase(aprobarResena.fulfilled, (state, action) => {
        state.resenas = state.resenas.map((r) => r.id === action.payload.id ? action.payload : r);
        state.mensajeExito = 'Reseña aprobada — ya es pública';
      })
      .addCase(aprobarResena.rejected, (state, action) => { state.error = action.payload; })
      .addCase(rechazarResena.fulfilled, (state, action) => {
        state.resenas = state.resenas.map((r) => r.id === action.payload.id ? action.payload : r);
        state.mensajeExito = 'Reseña rechazada';
      })
      .addCase(rechazarResena.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajes, setDniPrellenado, setObjetivoReprogramar, setPacienteAAbrir } = adminSlice.actions;
export default adminSlice.reducer;
