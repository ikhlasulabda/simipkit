package com.happy.simipkit.service;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.model.PortfolioReportSummary;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.thoughtworks.xstream.XStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Menangani generate laporan portofolio klien, termasuk parsing
 * custom template laporan (format XML) yang di-upload admin agar
 * layout laporan bisa disesuaikan tanpa perlu deploy ulang aplikasi.
 *
 * CATATAN KEAMANAN (untuk lab VA - JANGAN DIPERBAIKI):
 * parseReportTemplate() memanggil xstream.fromXML() langsung terhadap
 * XML yang berasal dari input admin, tanpa whitelist/Security Framework
 * XStream. Pada XStream versi 1.4.10, ini rentan RCE (CVE-2020-26217)
 * karena versi tersebut masih memakai blocklist (bukan default-deny),
 * sehingga payload XML yang memanipulasi tipe objek dapat memicu
 * eksekusi command di server.
 */
@Service
public class ReportService {

    private static final Logger logger = LogManager.getLogger(ReportService.class);
    private final JdbcTemplate jdbcTemplate;
    private final XStream xstream;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.xstream = new XStream();
        // Tidak ada xstream.addPermission(...) / setupDefaultSecurity() di sini -
        // ini yang bikin instance ini tetap memakai behavior default versi 1.4.10
    }

    /**
     * Parse XML template laporan yang disimpan di database menjadi
     * objek konfigurasi layout, dipakai saat generate laporan PDF.
     */
    public Object parseReportTemplate(String xmlContent) {
        logger.info("Parsing report template XML, length: {} chars", xmlContent.length());
        Object templateConfig = xstream.fromXML(xmlContent);
        logger.info("Report template parsed successfully");
        return templateConfig;
    }

    public Map<String, Object> generatePortfolioSummary(String clientId, String periode) {
        // logic hitung total nilai portofolio, dipanggil dari PortfolioReportController
        String sql = "SELECT SUM(nilai) as total FROM portfolio_assets WHERE client_id = ?";
        return jdbcTemplate.queryForMap(sql, clientId);
    }

    /**
     * Generate dokumen PDF laporan portofolio menggunakan iTextPDF 5.5.13.3.
     * Data yang dirender adalah snapshot immutable dari saat laporan dibuat
     * (bukan data live dari portfolio_assets).
     */
    public byte[] generatePortfolioPdfBytes(Client client, PortfolioReportSummary summary, List<PortfolioAsset> assets) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitle = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font fontSubtitle = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(100, 116, 139));
            Font fontLabel = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
            Font fontNormal = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            Font fontTableHeader = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
            Font fontTableCell = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Font fontSectionTitle = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            // Judul laporan
            Paragraph title = new Paragraph("LAPORAN PORTOFOLIO KLIEN", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("SIMIPKIT - Sistem Informasi Manajemen Investasi dan Portofolio Klien", fontSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16);
            document.add(subtitle);

            // Garis pemisah
            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new BaseColor(15, 23, 42));
            document.add(new Chunk(separator));

            // Spasi setelah garis
            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingAfter(12);
            document.add(spacer);

            // Tabel info klien (label | nilai)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(65);
            infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            infoTable.setWidths(new int[]{3, 5});
            infoTable.setSpacingAfter(20);

            addInfoRow(infoTable, "Nama Klien", client.getNama() != null ? client.getNama() : "-", fontLabel, fontNormal);
            addInfoRow(infoTable, "NIK", client.getNik() != null ? client.getNik() : "-", fontLabel, fontNormal);
            addInfoRow(infoTable, "Periode", summary.getPeriode() != null ? summary.getPeriode() : "-", fontLabel, fontNormal);
            addInfoRow(infoTable, "Total Nilai", "Rp " + String.format("%,.2f", summary.getTotalNilai()), fontLabel, fontNormal);
            if (summary.getGeneratedAt() != null) {
                String generatedAt = summary.getGeneratedAt().toString().replace("T", " ");
                addInfoRow(infoTable, "Dibuat Pada", generatedAt, fontLabel, fontNormal);
            }
            document.add(infoTable);

            // Judul tabel rincian
            Paragraph sectionTitle = new Paragraph("Rincian Instrumen Portofolio", fontSectionTitle);
            sectionTitle.setSpacingAfter(8);
            document.add(sectionTitle);

            // Tabel rincian aset
            PdfPTable assetsTable = new PdfPTable(5);
            assetsTable.setWidthPercentage(100);
            assetsTable.setWidths(new int[]{2, 4, 2, 3, 2});
            assetsTable.setSpacingAfter(20);
            assetsTable.setHeaderRows(1);

            BaseColor headerBg = new BaseColor(15, 23, 42);
            String[] headers = {"Jenis Instrumen", "Nama Instrumen", "Jumlah Unit", "Nilai (IDR)", "Alokasi (%)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fontTableHeader));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setBorderColor(new BaseColor(30, 41, 59));
                assetsTable.addCell(cell);
            }

            if (assets == null || assets.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Tidak ada data instrumen.", fontTableCell));
                emptyCell.setColspan(5);
                emptyCell.setPadding(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                assetsTable.addCell(emptyCell);
            } else {
                boolean even = false;
                BaseColor rowEven = new BaseColor(248, 250, 252);
                for (PortfolioAsset a : assets) {
                    BaseColor rowBg = even ? rowEven : BaseColor.WHITE;
                    addAssetCell(assetsTable, a.getJenisInstrumen(), fontTableCell, rowBg);
                    addAssetCell(assetsTable, a.getNamaInstrumen(), fontTableCell, rowBg);
                    addAssetCell(assetsTable, String.valueOf(a.getJumlah()), fontTableCell, rowBg);
                    addAssetCell(assetsTable, "Rp " + String.format("%,.2f", a.getNilai()), fontTableCell, rowBg);
                    addAssetCell(assetsTable, String.format("%.2f", a.getAllocationPercent()) + "%", fontTableCell, rowBg);
                    even = !even;
                }
            }
            document.add(assetsTable);

            // Footer
            Paragraph footer = new Paragraph("Dokumen ini digenerate secara otomatis oleh sistem SIMIPKIT. No. Ref: " + summary.getId(), fontSubtitle);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            logger.error("Gagal generate PDF laporan portofolio untuk client: {}", client.getId(), e);
        }
        return baos.toByteArray();
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);
        table.addCell(valueCell);
    }

    private void addAssetCell(PdfPTable table, String content, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "-", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        table.addCell(cell);
    }
}