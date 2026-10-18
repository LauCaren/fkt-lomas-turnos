package com.fktlomas.turnos.storage;

import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenSeguraTest {

    @Test
    void reCodificaLaImagenYDescartaLoQueVieneDespuesDeLosPixeles() throws Exception {
        // Un JPEG válido con texto agregado al final (lo que haría un "políglota" o donde suelen
        // viajar metadatos): el resultado tiene que ser una imagen válida sin ese contenido extra.
        byte[] jpeg = imagen("jpg", 64, 48);
        byte[] conBasura = concatenar(jpeg, "<script>alert(1)</script>".getBytes(StandardCharsets.US_ASCII));

        ImagenSegura.Imagen resultado = ImagenSegura.procesar(new MockMultipartFile("foto", "f.jpg", "image/jpeg", conBasura));

        assertThat(resultado.extension()).isEqualTo(".jpg");
        assertThat(resultado.contentType()).isEqualTo("image/jpeg");
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(resultado.contenido()));
        assertThat(leida.getWidth()).isEqualTo(64);
        assertThat(new String(resultado.contenido(), StandardCharsets.ISO_8859_1)).doesNotContain("<script>");
    }

    @Test
    void rechazaUnaImagenConDimensionesExageradas() throws Exception {
        byte[] enorme = imagen("png", ImagenSegura.LADO_MAXIMO + 1, 1);

        assertThatThrownBy(() -> ImagenSegura.procesar(new MockMultipartFile("foto", "f.png", "image/png", enorme)))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("demasiado grande");
    }

    @Test
    void rechazaAlgoQueNoEsUnaImagenAunqueDigaSerlo() {
        byte[] html = "<html><body>hola</body></html>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ImagenSegura.procesar(new MockMultipartFile("foto", "f.png", "image/png", html)))
                .isInstanceOf(ArchivoInvalidoException.class);
    }

    @Test
    void rechazaFormatosQueNoSonJpgNiPng() {
        assertThatThrownBy(() -> ImagenSegura.procesar(new MockMultipartFile("foto", "f.gif", "image/gif", new byte[]{1, 2, 3})))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("JPG o PNG");
    }

    private static byte[] imagen(String formato, int ancho, int alto) throws Exception {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, salida);
        return salida.toByteArray();
    }

    private static byte[] concatenar(byte[] a, byte[] b) {
        byte[] todo = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, todo, a.length, b.length);
        return todo;
    }
}
