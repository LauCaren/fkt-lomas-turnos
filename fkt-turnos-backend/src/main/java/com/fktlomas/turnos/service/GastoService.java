package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.CategoriaGasto;
import com.fktlomas.turnos.domain.Gasto;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.caja.CategoriaGastoDto;
import com.fktlomas.turnos.dto.caja.CrearCategoriaGastoRequest;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.CategoriaGastoRepository;
import com.fktlomas.turnos.repository.GastoRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GastoService {

    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final GastoRepository gastoRepository;
    private final CategoriaGastoRepository categoriaGastoRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<CategoriaGastoDto> listarCategorias() {
        return categoriaGastoRepository.findAllByOrderByNombreAsc().stream().map(CategoriaGastoDto::de).toList();
    }

    @Transactional
    public CategoriaGastoDto crearCategoria(CrearCategoriaGastoRequest request) {
        if (categoriaGastoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictoDeDatosException("Ya existe una categoría con ese nombre");
        }
        CategoriaGasto categoria = new CategoriaGasto();
        categoria.setNombre(request.nombre());
        return CategoriaGastoDto.de(categoriaGastoRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<GastoDto> listar(Long categoriaId, LocalDate desde, LocalDate hasta) {
        return gastoRepository.buscar(categoriaId, desde, hasta).stream().map(GastoDto::de).toList();
    }

    @Transactional
    public GastoDto registrar(Long categoriaId, BigDecimal monto, LocalDate fecha, String descripcion,
                               MultipartFile comprobante, Usuario registradoPor) {
        CategoriaGasto categoria = categoriaGastoRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));

        Gasto gasto = new Gasto();
        gasto.setCategoria(categoria);
        gasto.setMonto(monto);
        gasto.setFecha(fecha != null ? fecha : LocalDate.now());
        gasto.setDescripcion(descripcion);
        gasto.setRegistradoPor(registradoPor);

        if (comprobante != null && !comprobante.isEmpty()) {
            TipoArchivo tipo = tipoValidado(comprobante);
            try {
                gasto.setComprobantePath(fileStorageService.guardar("gastos/comprobantes", UUID.randomUUID() + tipo.extension(), comprobante));
                gasto.setComprobanteContentType(tipo.contentType());
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo guardar el comprobante", e);
            }
        }

        return GastoDto.de(gastoRepository.save(gasto));
    }

    @Transactional
    public GastoDto anular(Long id, String motivo) {
        Gasto gasto = gastoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Gasto no encontrado"));
        if (gasto.isAnulado()) {
            throw new ConflictoDeDatosException("Este gasto ya está anulado");
        }
        gasto.setAnulado(true);
        gasto.setMotivoAnulacion(motivo);
        return GastoDto.de(gastoRepository.save(gasto));
    }

    public Resource descargarComprobante(Long id) {
        Gasto gasto = gastoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Gasto no encontrado"));
        if (gasto.getComprobantePath() == null) {
            throw new RecursoNoEncontradoException("Este gasto no tiene comprobante adjunto");
        }
        return fileStorageService.cargar(gasto.getComprobantePath())
                .orElseThrow(() -> new RecursoNoEncontradoException("El archivo ya no está disponible"));
    }

    public String contentTypeComprobante(Long id) {
        return gastoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Gasto no encontrado")).getComprobanteContentType();
    }

    private record TipoArchivo(String extension, String contentType) {
    }

    /**
     * El Content-Type del multipart lo declara el cliente y se puede falsificar — nunca se
     * persiste ni se sirve de vuelta. Se deriva la extensión Y el content-type real a partir
     * de la firma binaria del archivo, la única fuente confiable (evita que un comprobante
     * subido con Content-Type "text/html" pero magic bytes de imagen se sirva después como
     * HTML y ejecute script en el panel de quien lo abre).
     */
    private TipoArchivo tipoValidado(MultipartFile archivo) {
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoInvalidoException("El comprobante no puede superar los 5MB");
        }
        byte[] cabecera;
        try (var in = archivo.getInputStream()) {
            cabecera = in.readNBytes(8);
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo");
        }
        if (cabecera.length >= 5 && new String(cabecera, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) return new TipoArchivo(".pdf", "application/pdf");
        if (cabecera.length >= 2 && (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xFF) == 0xD8) return new TipoArchivo(".jpg", "image/jpeg");
        if (cabecera.length >= 8 && (cabecera[0] & 0xFF) == 0x89 && cabecera[1] == 'P' && cabecera[2] == 'N' && cabecera[3] == 'G') return new TipoArchivo(".png", "image/png");
        throw new ArchivoInvalidoException("El comprobante debe ser una imagen (JPG/PNG) o un PDF");
    }
}
