package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.dto.PostulacionDto;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.PostulacionRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostulacionService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("application/pdf");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final PostulacionRepository postulacionRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    public PostulacionDto recibir(String nombreCompleto, String email, String telefono, String mensaje, MultipartFile cv) {
        validarArchivo(cv);

        try {
            String nombreArchivo = UUID.randomUUID() + ".pdf";
            String clave = fileStorageService.guardar("cv", nombreArchivo, cv);

            Postulacion postulacion = new Postulacion();
            postulacion.setNombreCompleto(nombreCompleto);
            postulacion.setEmail(email);
            postulacion.setTelefono(telefono);
            postulacion.setMensaje(mensaje);
            postulacion.setCvPath(clave);
            postulacion.setCvNombreOriginal(StringUtils.cleanPath(cv.getOriginalFilename() != null ? cv.getOriginalFilename() : nombreArchivo));
            postulacion.setCvContentType(cv.getContentType());
            postulacion.setCvSize(cv.getSize());

            postulacionRepository.save(postulacion);
            emailService.notificarNuevaPostulacion(postulacion);
            return PostulacionDto.de(postulacion);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el CV", e);
        }
    }

    private void validarArchivo(MultipartFile cv) {
        if (cv == null || cv.isEmpty()) {
            throw new ArchivoInvalidoException("Adjuntá tu CV en formato PDF");
        }
        if (!TIPOS_PERMITIDOS.contains(cv.getContentType())) {
            throw new ArchivoInvalidoException("El CV debe ser un archivo PDF");
        }
        if (cv.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoInvalidoException("El CV no puede superar los 5MB");
        }
        // El Content-Type de arriba lo declara el navegador — se puede falsificar. Se verifica
        // además que el archivo empiece con la firma real de un PDF ("%PDF-").
        try (var in = cv.getInputStream()) {
            byte[] firma = in.readNBytes(5);
            if (!new String(firma, StandardCharsets.US_ASCII).equals("%PDF-")) {
                throw new ArchivoInvalidoException("El archivo no es un PDF válido");
            }
        } catch (IOException e) {
            throw new ArchivoInvalidoException("El archivo no es un PDF válido");
        }
    }

    public List<PostulacionDto> listar() {
        return postulacionRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(PostulacionDto::de).toList();
    }

    public PostulacionDto marcarRevisado(Long id) {
        Postulacion postulacion = obtenerEntidad(id);
        postulacion.setRevisado(true);
        return PostulacionDto.de(postulacionRepository.save(postulacion));
    }

    public Postulacion obtenerEntidad(Long id) {
        return postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación no encontrada"));
    }

    public Resource descargarCv(Long id) {
        Postulacion postulacion = obtenerEntidad(id);
        return fileStorageService.cargar(postulacion.getCvPath())
                .orElseThrow(() -> new RecursoNoEncontradoException("El archivo del CV ya no está disponible"));
    }
}
