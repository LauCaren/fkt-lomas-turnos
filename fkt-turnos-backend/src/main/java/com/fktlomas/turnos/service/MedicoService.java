package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.MedicoDto;
import com.fktlomas.turnos.dto.admin.ActualizarMedicoRequest;
import com.fktlomas.turnos.dto.admin.CrearMedicoRequest;
import com.fktlomas.turnos.dto.admin.CredencialesMedicoRequest;
import com.fktlomas.turnos.dto.admin.MedicoAdminDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import com.fktlomas.turnos.storage.ImagenSegura;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private static final String AUDITORIA_TIPO_MEDICO = "MEDICO";

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadService especialidadService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final PasswordFiltradaService passwordFiltradaService;

    @Transactional(readOnly = true)
    public List<MedicoDto> listar(Long especialidadId) {
        List<Medico> medicos = especialidadId != null
                ? medicoRepository.findByEspecialidadIdAndActivoTrue(especialidadId)
                : medicoRepository.findByActivoTrue();
        return medicos.stream().map(MedicoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicoAdminDto> listarTodosAdmin() {
        return medicoRepository.findAll().stream().map(MedicoAdminDto::de).toList();
    }

    public Medico obtenerEntidad(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
    }

    public MedicoAdminDto crear(CrearMedicoRequest request, Usuario actor) {
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        Medico medico = new Medico();
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        medico.setAnosExperiencia(request.anosExperiencia());
        MedicoAdminDto creado = MedicoAdminDto.de(medicoRepository.save(medico));
        auditoriaService.registrar(actor, AUDITORIA_TIPO_MEDICO, creado.id(), "CREAR", null, creado);
        return creado;
    }

    /**
     * @Transactional acá es necesario, no cosmético: obtenerEntidad(id) + save(medico) son
     * dos transacciones cortas separadas si no. Entre una y otra, medico queda "detached" y
     * medicoRepository.save(...) hace un merge que reengancha medico.especialidad como un
     * proxy lazy nuevo en el nuevo contexto de persistencia — y esa transacción del save ya
     * cerró para cuando MedicoDto.de() intenta leerlo, así que explota con
     * LazyInitializationException. Con @Transactional todo el método comparte una sola sesión.
     */
    @Transactional
    public MedicoAdminDto actualizar(Long id, ActualizarMedicoRequest request, Usuario actor) {
        Medico medico = obtenerEntidad(id);
        MedicoAdminDto antes = MedicoAdminDto.de(medico);
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setActivo(request.activo());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        medico.setAnosExperiencia(request.anosExperiencia());
        medico.setCobraCopagoIoma(request.cobraCopagoIoma());
        medico.setTarifaConsulta(request.tarifaConsulta());
        MedicoAdminDto actualizado = MedicoAdminDto.de(medicoRepository.save(medico));
        auditoriaService.registrar(actor, AUDITORIA_TIPO_MEDICO, id, "ACTUALIZAR", antes, actualizado);
        return actualizado;
    }

    /**
     * Crea (o reemplaza) las credenciales de login del médico. Siempre las da de alta el
     * admin desde el panel — el médico no se autoregistra.
     */
    @Transactional
    public MedicoAdminDto crearOActualizarCredenciales(Long id, CredencialesMedicoRequest request) {
        Medico medico = obtenerEntidad(id);
        Usuario usuario = medico.getUsuario();
        String emailNormalizado = request.email().trim().toLowerCase();

        if (usuario == null) {
            if (usuarioRepository.existsByEmail(emailNormalizado)) {
                throw new ConflictoDeDatosException("Ya existe una cuenta con ese email");
            }
            usuario = new Usuario();
            usuario.setRole(Role.MEDICO);
            usuario.setAceptaPoliticaPrivacidad(true);
        } else if (!usuario.getEmail().equalsIgnoreCase(emailNormalizado) && usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ConflictoDeDatosException("Ya existe una cuenta con ese email");
        }
        passwordFiltradaService.verificar(request.password());

        usuario.setNombreCompleto(medico.getNombreCompleto());
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuarioRepository.save(usuario);
        medico.setUsuario(usuario);
        return MedicoAdminDto.de(medicoRepository.save(medico));
    }

    @Transactional
    public MedicoAdminDto subirFoto(Long id, MultipartFile foto) {
        Medico medico = obtenerEntidad(id);
        String rutaAnterior = medico.getFotoPath();
        ImagenGuardada guardada = guardarImagen(foto, "foto");
        medico.setFotoPath(guardada.ruta());
        medico.setFotoContentType(guardada.contentType());
        Medico guardado = medicoRepository.save(medico);
        borrarSiExiste(rutaAnterior);
        return MedicoAdminDto.de(guardado);
    }

    @Transactional
    public MedicoAdminDto subirTitulo(Long id, MultipartFile imagen) {
        Medico medico = obtenerEntidad(id);
        String rutaAnterior = medico.getTituloImagenPath();
        ImagenGuardada guardada = guardarImagen(imagen, "titulo");
        medico.setTituloImagenPath(guardada.ruta());
        medico.setTituloImagenContentType(guardada.contentType());
        Medico guardado = medicoRepository.save(medico);
        borrarSiExiste(rutaAnterior);
        return MedicoAdminDto.de(guardado);
    }

    public Resource obtenerFoto(Long id) {
        return recursoDeArchivo(obtenerEntidad(id).getFotoPath(), "Este médico todavía no tiene una foto cargada");
    }

    public String obtenerFotoContentType(Long id) {
        return obtenerEntidad(id).getFotoContentType();
    }

    public Resource obtenerTitulo(Long id) {
        return recursoDeArchivo(obtenerEntidad(id).getTituloImagenPath(), "Este médico todavía no tiene el título cargado");
    }

    public String obtenerTituloContentType(Long id) {
        return obtenerEntidad(id).getTituloImagenContentType();
    }

    private Resource recursoDeArchivo(String rutaGuardada, String mensajeSiFalta) {
        return fileStorageService.cargar(rutaGuardada)
                .orElseThrow(() -> new RecursoNoEncontradoException(mensajeSiFalta));
    }

    private ImagenGuardada guardarImagen(MultipartFile archivo, String subcarpeta) {
        // Validación + re-codificación (sin EXIF/GPS, con tope de dimensiones): ver ImagenSegura.
        ImagenSegura.Imagen imagen = ImagenSegura.procesar(archivo);
        try {
            String ruta = fileStorageService.guardar("medicos/" + subcarpeta, UUID.randomUUID() + imagen.extension(),
                    imagen.contenido(), imagen.contentType());
            return new ImagenGuardada(ruta, imagen.contentType());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    /** Dónde quedó la imagen y con qué Content-Type se guardó (el que se sirve después). */
    private record ImagenGuardada(String ruta, String contentType) {
    }

    private void borrarSiExiste(String ruta) {
        fileStorageService.borrar(ruta);
    }
}
