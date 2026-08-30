package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Especialidad;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.dto.MedicoDto;
import com.fktlomas.turnos.dto.admin.ActualizarMedicoRequest;
import com.fktlomas.turnos.dto.admin.CrearMedicoRequest;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private static final Map<String, String> EXTENSIONES_PERMITIDAS = Map.of("image/jpeg", ".jpg", "image/png", ".png");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final MedicoRepository medicoRepository;
    private final EspecialidadService especialidadService;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @Transactional(readOnly = true)
    public List<MedicoDto> listar(Long especialidadId) {
        List<Medico> medicos = especialidadId != null
                ? medicoRepository.findByEspecialidadIdAndActivoTrue(especialidadId)
                : medicoRepository.findByActivoTrue();
        return medicos.stream().map(MedicoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicoDto> listarTodosAdmin() {
        return medicoRepository.findAll().stream().map(MedicoDto::de).toList();
    }

    public Medico obtenerEntidad(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
    }

    public MedicoDto crear(CrearMedicoRequest request) {
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        Medico medico = new Medico();
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        medico.setAnosExperiencia(request.anosExperiencia());
        return MedicoDto.de(medicoRepository.save(medico));
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
    public MedicoDto actualizar(Long id, ActualizarMedicoRequest request) {
        Medico medico = obtenerEntidad(id);
        Especialidad especialidad = especialidadService.obtenerEntidad(request.especialidadId());
        medico.setNombreCompleto(request.nombreCompleto());
        medico.setEspecialidad(especialidad);
        medico.setMatricula(request.matricula());
        medico.setActivo(request.activo());
        medico.setDuracionTurnoMinutos(request.duracionTurnoMinutos());
        medico.setAnosExperiencia(request.anosExperiencia());
        return MedicoDto.de(medicoRepository.save(medico));
    }

    @Transactional
    public MedicoDto subirFoto(Long id, MultipartFile foto) {
        Medico medico = obtenerEntidad(id);
        String rutaAnterior = medico.getFotoPath();
        String destino = guardarImagen(foto, "foto");
        medico.setFotoPath(destino);
        medico.setFotoContentType(foto.getContentType());
        Medico guardado = medicoRepository.save(medico);
        borrarSiExiste(rutaAnterior);
        return MedicoDto.de(guardado);
    }

    @Transactional
    public MedicoDto subirTitulo(Long id, MultipartFile imagen) {
        Medico medico = obtenerEntidad(id);
        String rutaAnterior = medico.getTituloImagenPath();
        String destino = guardarImagen(imagen, "titulo");
        medico.setTituloImagenPath(destino);
        medico.setTituloImagenContentType(imagen.getContentType());
        Medico guardado = medicoRepository.save(medico);
        borrarSiExiste(rutaAnterior);
        return MedicoDto.de(guardado);
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
        if (rutaGuardada == null) {
            throw new RecursoNoEncontradoException(mensajeSiFalta);
        }
        Path path = Path.of(rutaGuardada);
        if (!Files.exists(path)) {
            throw new RecursoNoEncontradoException(mensajeSiFalta);
        }
        return new FileSystemResource(path);
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
        try {
            Path directorio = Path.of(uploadsDir, "medicos", subcarpeta);
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(UUID.randomUUID() + extension);
            archivo.transferTo(destino);
            return destino.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    private void borrarSiExiste(String ruta) {
        if (ruta == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(ruta));
        } catch (IOException ignored) {
            // No es crítico: queda un archivo huérfano en disco, no afecta el funcionamiento.
        }
    }
}
