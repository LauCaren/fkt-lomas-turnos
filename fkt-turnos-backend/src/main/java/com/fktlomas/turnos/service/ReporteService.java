package com.fktlomas.turnos.service;

import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.caja.GastoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.RetiroDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Genera los reportes exportables que piden las secretarias (agenda y caja, en Excel o PDF) —
 * tanto para bajarlos desde el panel como para el envío mensual automático de caja
 * (ver ReporteCajaProgramadoService). Ambas librerías (Apache POI, Apache PDFBox) son
 * Apache 2.0, aptas para un producto comercial cerrado sin costo de licencia.
 */
@Service
public class ReporteService {

    public byte[] excelAgenda(List<TurnoAdminDto> turnos, LocalDate desde, LocalDate hasta) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Agenda " + desde + " a " + hasta);
            CellStyle encabezado = estiloEncabezado(libro);
            String[] columnas = {"Fecha", "Hora", "Médico", "Especialidad", "Estado", "Paciente", "DNI", "Obra social", "Motivo"};
            escribirEncabezado(hoja, columnas, encabezado);

            int fila = 1;
            for (TurnoAdminDto t : turnos) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(t.fecha().toString());
                r.createCell(1).setCellValue(t.horaInicio().toString());
                r.createCell(2).setCellValue(t.medicoNombre());
                r.createCell(3).setCellValue(t.especialidadNombre());
                r.createCell(4).setCellValue(t.estado());
                r.createCell(5).setCellValue(nullASvacio(t.pacienteNombre()));
                r.createCell(6).setCellValue(nullASvacio(t.pacienteDni()));
                r.createCell(7).setCellValue(nullASvacio(t.pacienteObraSocial()));
                r.createCell(8).setCellValue(nullASvacio(t.motivoConsulta()));
            }
            autoAjustarColumnas(hoja, columnas.length);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel de agenda", e);
        }
    }

    public byte[] excelCaja(List<IngresoDto> ingresos, List<RetiroDto> retiros, List<GastoDto> gastos, LocalDate desde, LocalDate hasta) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle encabezado = estiloEncabezado(libro);

            Sheet hojaIngresos = libro.createSheet("Ingresos");
            String[] colIngresos = {"Fecha", "Médico", "Monto", "Método de pago", "Registrado por", "Anulado", "Motivo anulación"};
            escribirEncabezado(hojaIngresos, colIngresos, encabezado);
            int filaI = 1;
            for (IngresoDto i : ingresos) {
                Row r = hojaIngresos.createRow(filaI++);
                r.createCell(0).setCellValue(i.fecha().toString());
                r.createCell(1).setCellValue(i.medicoNombre());
                r.createCell(2).setCellValue(i.monto().doubleValue());
                r.createCell(3).setCellValue(i.metodoPago());
                r.createCell(4).setCellValue(i.registradoPorNombre());
                r.createCell(5).setCellValue(i.anulado() ? "Sí" : "No");
                r.createCell(6).setCellValue(nullASvacio(i.motivoAnulacion()));
            }
            autoAjustarColumnas(hojaIngresos, colIngresos.length);

            Sheet hojaRetiros = libro.createSheet("Retiros");
            String[] colRetiros = {"Fecha y hora", "Médico", "Monto", "Entregado por", "Anulado", "Motivo anulación"};
            escribirEncabezado(hojaRetiros, colRetiros, encabezado);
            int filaR = 1;
            for (RetiroDto r0 : retiros) {
                Row r = hojaRetiros.createRow(filaR++);
                r.createCell(0).setCellValue(r0.fechaHora().toString());
                r.createCell(1).setCellValue(r0.medicoNombre());
                r.createCell(2).setCellValue(r0.monto().doubleValue());
                r.createCell(3).setCellValue(r0.entregadoPorNombre());
                r.createCell(4).setCellValue(r0.anulado() ? "Sí" : "No");
                r.createCell(5).setCellValue(nullASvacio(r0.motivoAnulacion()));
            }
            autoAjustarColumnas(hojaRetiros, colRetiros.length);

            Sheet hojaGastos = libro.createSheet("Gastos");
            String[] colGastos = {"Fecha", "Categoría", "Monto", "Descripción", "Registrado por", "Anulado", "Motivo anulación"};
            escribirEncabezado(hojaGastos, colGastos, encabezado);
            int filaG = 1;
            for (GastoDto g : gastos) {
                Row r = hojaGastos.createRow(filaG++);
                r.createCell(0).setCellValue(g.fecha().toString());
                r.createCell(1).setCellValue(g.categoriaNombre());
                r.createCell(2).setCellValue(g.monto().doubleValue());
                r.createCell(3).setCellValue(nullASvacio(g.descripcion()));
                r.createCell(4).setCellValue(g.registradoPorNombre());
                r.createCell(5).setCellValue(g.anulado() ? "Sí" : "No");
                r.createCell(6).setCellValue(nullASvacio(g.motivoAnulacion()));
            }
            autoAjustarColumnas(hojaGastos, colGastos.length);

            Sheet hojaResumen = libro.createSheet("Resumen");
            escribirEncabezado(hojaResumen, new String[]{"Concepto", "Monto"}, encabezado);
            BigDecimal totalIngresos = sumar(ingresos.stream().filter(i -> !i.anulado()).map(IngresoDto::monto).toList());
            BigDecimal totalRetiros = sumar(retiros.stream().filter(r -> !r.anulado()).map(RetiroDto::monto).toList());
            BigDecimal totalGastos = sumar(gastos.stream().filter(g -> !g.anulado()).map(GastoDto::monto).toList());
            escribirFilaResumen(hojaResumen, 1, "Ingresos (" + desde + " a " + hasta + ")", totalIngresos);
            escribirFilaResumen(hojaResumen, 2, "Retiros", totalRetiros);
            escribirFilaResumen(hojaResumen, 3, "Gastos", totalGastos);
            escribirFilaResumen(hojaResumen, 4, "Caja física (ingresos - retiros - gastos)", totalIngresos.subtract(totalRetiros).subtract(totalGastos));
            autoAjustarColumnas(hojaResumen, 2);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel de caja", e);
        }
    }

    public byte[] pdfAgenda(List<TurnoAdminDto> turnos, LocalDate desde, LocalDate hasta) {
        String[] columnas = {"Fecha", "Hora", "Médico", "Paciente", "DNI", "Estado", "Motivo"};
        float[] anchos = {55, 35, 90, 100, 55, 60, 105};
        List<String[]> filas = turnos.stream()
                .map(t -> new String[]{
                        t.fecha().toString(), t.horaInicio().toString().substring(0, 5), t.medicoNombre(),
                        nullASvacio(t.pacienteNombre()), nullASvacio(t.pacienteDni()), t.estado(), nullASvacio(t.motivoConsulta())
                })
                .toList();
        return generarPdf("Agenda " + desde + " a " + hasta, columnas, anchos, filas);
    }

    public byte[] pdfCaja(List<IngresoDto> ingresos, List<RetiroDto> retiros, List<GastoDto> gastos, LocalDate desde, LocalDate hasta) {
        try (PDDocument doc = new PDDocument()) {
            EscritorPdf escritor = new EscritorPdf(doc, "Caja " + desde + " a " + hasta);

            escritor.titulo("Ingresos");
            escritor.tabla(new String[]{"Fecha", "Médico", "Monto", "Método", "Registró", "Anulado"}, new float[]{55, 110, 60, 80, 100, 45},
                    ingresos.stream().map(i -> new String[]{i.fecha().toString(), i.medicoNombre(), i.monto().toString(),
                            i.metodoPago(), i.registradoPorNombre(), i.anulado() ? "Sí" : "No"}).toList());

            escritor.titulo("Retiros");
            escritor.tabla(new String[]{"Fecha y hora", "Médico", "Monto", "Entregó", "Anulado"}, new float[]{100, 110, 60, 100, 80},
                    retiros.stream().map(r -> new String[]{r.fechaHora().toString(), r.medicoNombre(), r.monto().toString(),
                            r.entregadoPorNombre(), r.anulado() ? "Sí" : "No"}).toList());

            escritor.titulo("Gastos");
            escritor.tabla(new String[]{"Fecha", "Categoría", "Monto", "Descripción", "Anulado"}, new float[]{55, 100, 60, 175, 60},
                    gastos.stream().map(g -> new String[]{g.fecha().toString(), g.categoriaNombre(), g.monto().toString(),
                            nullASvacio(g.descripcion()), g.anulado() ? "Sí" : "No"}).toList());

            BigDecimal totalIngresos = sumar(ingresos.stream().filter(i -> !i.anulado()).map(IngresoDto::monto).toList());
            BigDecimal totalRetiros = sumar(retiros.stream().filter(r -> !r.anulado()).map(RetiroDto::monto).toList());
            BigDecimal totalGastos = sumar(gastos.stream().filter(g -> !g.anulado()).map(GastoDto::monto).toList());
            escritor.titulo("Resumen");
            escritor.tabla(new String[]{"Concepto", "Monto"}, new float[]{300, 150}, List.of(
                    new String[]{"Ingresos", totalIngresos.toString()},
                    new String[]{"Retiros", totalRetiros.toString()},
                    new String[]{"Gastos", totalGastos.toString()},
                    new String[]{"Caja física", totalIngresos.subtract(totalRetiros).subtract(totalGastos).toString()}
            ));

            escritor.cerrar();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            doc.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF de caja", e);
        }
    }

    private byte[] generarPdf(String titulo, String[] columnas, float[] anchos, List<String[]> filas) {
        try (PDDocument doc = new PDDocument()) {
            EscritorPdf escritor = new EscritorPdf(doc, titulo);
            escritor.tabla(columnas, anchos, filas);
            escritor.cerrar();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            doc.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF", e);
        }
    }

    // ---- helpers Excel ----

    private CellStyle estiloEncabezado(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font negrita = libro.createFont();
        negrita.setBold(true);
        estilo.setFont(negrita);
        return estilo;
    }

    private void escribirEncabezado(Sheet hoja, String[] columnas, CellStyle estilo) {
        Row fila = hoja.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilo);
        }
    }

    private void escribirFilaResumen(Sheet hoja, int indice, String concepto, BigDecimal monto) {
        Row r = hoja.createRow(indice);
        r.createCell(0).setCellValue(concepto);
        r.createCell(1).setCellValue(monto.doubleValue());
    }

    private void autoAjustarColumnas(Sheet hoja, int cantidadColumnas) {
        for (int i = 0; i < cantidadColumnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    private BigDecimal sumar(List<BigDecimal> montos) {
        return montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String nullASvacio(String s) {
        return s != null ? s : "";
    }

    /** Escribe título + tablas simples en un PDDocument, paginando automáticamente. */
    private static class EscritorPdf {
        private static final float MARGEN = 40;
        private static final float ALTO_FILA = 16;

        private final PDDocument doc;
        private PDPage pagina;
        private PDPageContentStream cs;
        private float y;
        private final float anchoUtil;

        EscritorPdf(PDDocument doc, String titulo) throws IOException {
            this.doc = doc;
            this.anchoUtil = PDRectangle.A4.getWidth() - 2 * MARGEN;
            nuevaPagina();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            cs.beginText();
            cs.newLineAtOffset(MARGEN, y);
            cs.showText(titulo);
            cs.endText();
            y -= 30;
        }

        void titulo(String texto) throws IOException {
            asegurarEspacio(ALTO_FILA * 2);
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            cs.beginText();
            cs.newLineAtOffset(MARGEN, y);
            cs.showText(texto);
            cs.endText();
            y -= ALTO_FILA;
        }

        void tabla(String[] columnas, float[] anchos, List<String[]> filas) throws IOException {
            escribirFila(columnas, anchos, true);
            if (filas.isEmpty()) {
                escribirFila(new String[]{"(sin datos)"}, new float[]{anchoUtil}, false);
            }
            for (String[] fila : filas) {
                escribirFila(fila, anchos, false);
            }
            y -= ALTO_FILA / 2;
        }

        private void escribirFila(String[] valores, float[] anchos, boolean negrita) throws IOException {
            asegurarEspacio(ALTO_FILA);
            cs.setFont(new PDType1Font(negrita ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), 8.5f);
            float x = MARGEN;
            for (int i = 0; i < valores.length; i++) {
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(recortar(valores[i], anchos.length > i ? anchos[i] : 100));
                cs.endText();
                x += anchos.length > i ? anchos[i] : 100;
            }
            y -= ALTO_FILA;
        }

        private String recortar(String texto, float anchoMax) {
            if (texto == null) return "";
            int maxChars = (int) (anchoMax / 4.3);
            return texto.length() > maxChars ? texto.substring(0, Math.max(0, maxChars - 1)) + "…" : texto;
        }

        private void asegurarEspacio(float necesario) throws IOException {
            if (y - necesario < MARGEN) {
                cs.close();
                nuevaPagina();
            }
        }

        private void nuevaPagina() throws IOException {
            pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            cs = new PDPageContentStream(doc, pagina);
            y = PDRectangle.A4.getHeight() - MARGEN;
        }

        void cerrar() throws IOException {
            cs.close();
        }
    }
}
