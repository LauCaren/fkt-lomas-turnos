package com.fktlomas.turnos.storage;

import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Validación y "limpieza" de las imágenes que sube la gente (foto de perfil de cualquier
 * usuario, foto y título de los médicos). Un solo lugar para las dos reglas que antes estaban
 * copiadas en UsuarioFotoService y MedicoService.
 *
 * <p>Qué hace, en orden:
 * <ol>
 *   <li>Solo JPG o PNG (según el Content-Type declarado) y hasta 5 MB.</li>
 *   <li>Lee las dimensiones <b>antes</b> de decodificar: un PNG de pocos KB puede declarar
 *       50.000 × 50.000 píxeles y, al decodificarlo, pedir gigas de memoria y tirar abajo el
 *       backend (una "bomba de descompresión"). Más de {@value #LADO_MAXIMO} px por lado se
 *       rechaza.</li>
 *   <li>Decodifica y <b>vuelve a codificar</b> la imagen. Se guardan solo los píxeles, no el
 *       archivo original: así se descartan los metadatos EXIF (las fotos de celular suelen
 *       traer la ubicación GPS exacta de dónde se sacaron, y la foto de perfil la ve el resto
 *       del staff) y cualquier contenido extra escondido en el archivo (un "políglota" que es
 *       imagen válida y a la vez otra cosa).</li>
 * </ol>
 */
public final class ImagenSegura {

    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;
    static final int LADO_MAXIMO = 6000;

    /** Content-Type aceptado → formato de ImageIO y extensión con la que se guarda. */
    private static final Map<String, String[]> FORMATOS = Map.of(
            "image/jpeg", new String[]{"jpg", ".jpg"},
            "image/png", new String[]{"png", ".png"});

    /** Imagen ya validada y re-codificada, lista para pasarle a {@link FileStorageService}. */
    public record Imagen(byte[] contenido, String contentType, String extension) {
    }

    private ImagenSegura() {
    }

    public static Imagen procesar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Adjuntá una imagen");
        }
        // Map.of().get(null) tira NullPointerException: el Content-Type puede no venir.
        String contentType = archivo.getContentType();
        String[] formato = contentType == null ? null : FORMATOS.get(contentType);
        if (formato == null) {
            throw new ArchivoInvalidoException("La imagen debe ser JPG o PNG");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoInvalidoException("La imagen no puede superar los 5MB");
        }

        BufferedImage imagen = decodificarConLimite(archivo);
        if ("jpg".equals(formato[0]) && imagen.getColorModel().hasAlpha()) {
            // JPEG no tiene transparencia: ImageIO no escribe nada (devuelve false) si se le pasa
            // una imagen con canal alfa. Se aplana sobre blanco.
            imagen = sinTransparencia(imagen);
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            if (!ImageIO.write(imagen, formato[0], salida)) {
                throw new ArchivoInvalidoException("El archivo no es una imagen válida");
            }
            return new Imagen(salida.toByteArray(), contentType, formato[1]);
        } catch (IOException e) {
            throw new ArchivoInvalidoException("El archivo no es una imagen válida");
        }
    }

    private static BufferedImage decodificarConLimite(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream();
             ImageInputStream stream = ImageIO.createImageInputStream(entrada)) {
            Iterator<ImageReader> lectores = stream == null ? null : ImageIO.getImageReaders(stream);
            if (lectores == null || !lectores.hasNext()) {
                throw new ArchivoInvalidoException("El archivo no es una imagen válida");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(stream, true, true);
                if (lector.getWidth(0) > LADO_MAXIMO || lector.getHeight(0) > LADO_MAXIMO) {
                    throw new ArchivoInvalidoException("La imagen es demasiado grande (máximo " + LADO_MAXIMO + " píxeles por lado)");
                }
                BufferedImage imagen = lector.read(0);
                if (imagen == null) {
                    throw new ArchivoInvalidoException("El archivo no es una imagen válida");
                }
                return imagen;
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ArchivoInvalidoException invalido) {
                throw invalido;
            }
            // Cualquier error del decodificador (archivo truncado, formato raro) es "imagen inválida",
            // no un 500.
            throw new ArchivoInvalidoException("El archivo no es una imagen válida");
        }
    }

    private static BufferedImage sinTransparencia(BufferedImage original) {
        BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(original, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
