package ar.edu.uade.recordatorios.gui;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja el ícono de la app (círculo teal + campana) en código, para no
 * depender de un archivo de imagen externo ni del logo real de FKT Lomas
 * (evita cualquier tema de derechos sobre ese logo). Lo usan tanto el
 * ícono de la bandeja del sistema (TrayIconManager) como el generador del
 * .ico para el instalador de Windows (ver GenerarIcono).
 */
public final class IconoApp {

    private static final Color TEAL = new Color(0x0f, 0x76, 0x6e);

    private IconoApp() {
    }

    public static BufferedImage dibujar(int size) {
        BufferedImage imagen = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.setColor(TEAL);
        g.fillOval(0, 0, size, size);

        g.setColor(Color.WHITE);
        double cx = size / 2.0;
        double top = size * 0.22;
        double bodyBottom = size * 0.66;
        double halfTop = size * 0.11;
        double halfBottom = size * 0.27;

        Path2D.Double campana = new Path2D.Double();
        campana.moveTo(cx - halfTop, top + halfTop);
        campana.curveTo(cx - halfTop, top, cx + halfTop, top, cx + halfTop, top + halfTop);
        campana.curveTo(cx + halfTop, size * 0.40, cx + halfBottom, size * 0.44, cx + halfBottom, bodyBottom);
        campana.lineTo(cx - halfBottom, bodyBottom);
        campana.curveTo(cx - halfBottom, size * 0.44, cx - halfTop, size * 0.40, cx - halfTop, top + halfTop);
        campana.closePath();
        g.fill(campana);

        double baseAncho = 2 * (halfBottom + size * 0.03);
        double baseAlto = size * 0.05;
        g.fillRoundRect((int) (cx - baseAncho / 2), (int) bodyBottom, (int) baseAncho, (int) baseAlto,
            (int) (size * 0.02), (int) (size * 0.02));

        double badajoDiam = size * 0.10;
        g.fillOval((int) (cx - badajoDiam / 2), (int) (bodyBottom + baseAlto + size * 0.02),
            (int) badajoDiam, (int) badajoDiam);

        g.dispose();
        return imagen;
    }

    /** ICO multi-resolución con frames PNG (soportado desde Windows Vista) — más simple que codificar BMP crudo. */
    public static void generarIco(Path destino, int... tamanios) throws IOException {
        List<byte[]> pngs = new ArrayList<>();
        for (int tam : tamanios) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(dibujar(tam), "png", out);
            pngs.add(out.toByteArray());
        }

        int totalPng = pngs.stream().mapToInt(b -> b.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(6 + 16 * tamanios.length + totalPng);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 0);   // reservado
        buffer.putShort((short) 1);   // tipo: ícono
        buffer.putShort((short) tamanios.length);

        int offset = 6 + 16 * tamanios.length;
        for (int i = 0; i < tamanios.length; i++) {
            int tam = tamanios[i];
            byte[] png = pngs.get(i);
            buffer.put((byte) (tam >= 256 ? 0 : tam));
            buffer.put((byte) (tam >= 256 ? 0 : tam));
            buffer.put((byte) 0);   // sin paleta
            buffer.put((byte) 0);   // reservado
            buffer.putShort((short) 1);   // planos de color
            buffer.putShort((short) 32);  // bits por pixel
            buffer.putInt(png.length);
            buffer.putInt(offset);
            offset += png.length;
        }
        for (byte[] png : pngs) {
            buffer.put(png);
        }

        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Files.write(destino, buffer.array());
    }
}
