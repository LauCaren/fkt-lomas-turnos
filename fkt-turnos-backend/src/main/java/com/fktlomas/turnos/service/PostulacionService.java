package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Postulacion;
import com.fktlomas.turnos.dto.PostulacionDto;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.PostulacionRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
        // TIPOS_PERMITIDOS es un Set.of(): a diferencia de un HashSet, su contains(null) tira
        // NullPointerException en vez de devolver false — y el Content-Type puede venir null
        // (un cliente puede no mandarlo). El chequeo de null antes evita un 500 sin sentido en
        // vez del error de validación esperado.
        if (cv.getContentType() == null || !TIPOS_PERMITIDOS.contains(cv.getContentType())) {
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

    // Paginado (no findAll() completo): igual que ContactoService, esta bandeja crece con
    // los años y no tiene por qué traerse entera en cada carga del panel.
    public Page<PostulacionDto> listar(Pageable pageable) {
        Pageable ordenado = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        return postulacionRepository.findAll(ordenado).map(PostulacionDto::de);
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
