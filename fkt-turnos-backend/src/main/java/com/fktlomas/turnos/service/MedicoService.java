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
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private static final Map<String, String> EXTENSIONES_PERMITIDAS = Map.of("image/jpeg", ".jpg", "image/png", ".png");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadService especialidadService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

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

    public MedicoAdminDto crear(CrearMedicoRequest request) {
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        Medico medico = new Medico();
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        medico.setAnosExperiencia(request.anosExperiencia());
        return MedicoAdminDto.de(medicoRepository.save(medico));
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
    public MedicoAdminDto actualizar(Long id, ActualizarMedicoRequest request) {
        Medico medico = obtenerEntidad(id);
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setActivo(request.activo());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        medico.setAnosExperiencia(request.anosExperiencia());
        medico.setCobraCopagoIoma(request.cobraCopagoIoma());
        medico.setTarifaConsulta(request.tarifaConsulta());
        return MedicoAdminDto.de(medicoRepository.save(medico));
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
        String destino = guardarImagen(foto, "foto");
        medico.setFotoPath(destino);
        medico.setFotoContentType(foto.getContentType());
        Medico guardado = medicoRepository.save(medico);
        borrarSiExiste(rutaAnterior);
        return MedicoAdminDto.de(guardado);
    }

    @Transactional
    public MedicoAdminDto subirTitulo(Long id, MultipartFile imagen) {
        Medico medico = obtenerEntidad(id);
        String rutaAnterior = medico.getTituloImagenPath();
        String destino = guardarImagen(imagen, "titulo");
        medico.setTituloImagenPath(destino);
        medico.setTituloImagenContentType(imagen.getContentType());
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

    private String guardarImagen(MultipartFile archivo, String subcarpeta) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Adjuntá una imagen");
        }
        String extension = EXTENSIONES_PERMITIDAS.get(archivo.getContentType());
        if (extension == null) {
            throw new ArchivoInvalidoException("La imagen debe ser JPG o PNG");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoInvalidoException("La imagen no puede superar los 5MB");
        }
        // El Content-Type de arriba lo declara el navegador — se puede falsificar. Acá se
        // verifica que el archivo sea realmente una imagen decodificable, no cualquier cosa
        // subida con una extensión/mimetype falso.
        try {
            if (ImageIO.read(archivo.getInputStream()) == null) {
                throw new ArchivoInvalidoException("El archivo no es una imagen válida");
            }
        } catch (IOException e) {
            throw new ArchivoInvalidoException("El archivo no es una imagen válida");
        }
        try {
            return fileStorageService.guardar("medicos/" + subcarpeta, UUID.randomUUID() + extension, archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    private void borrarSiExiste(String ruta) {
        fileStorageService.borrar(ruta);
    }
}
