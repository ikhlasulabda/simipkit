package com.happy.simipkit.service;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.model.PortfolioReportSummary;
import com.happy.simipkit.model.ReportLayoutConfig;
import com.happy.simipkit.util.CurrencyUtil;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * Parser aman berbasis DOM untuk template layout PDF (terpisah dari parseReportTemplate XStream).
     * Menerapkan XXE hardening standar.
     */
    public ReportLayoutConfig parseReportLayoutTemplate(String xmlContent) throws Exception {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("XML content is empty.");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xmlContent)));
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        if (!"reportTemplate".equals(root.getNodeName())) {
            throw new IllegalArgumentException("Root element must be <reportTemplate>.");
        }

        ReportLayoutConfig config = new ReportLayoutConfig();

        NodeList titleList = root.getElementsByTagName("title");
        if (titleList.getLength() > 0) {
            config.setTitle(titleList.item(0).getTextContent().trim());
        }

        NodeList subtitleList = root.getElementsByTagName("subtitle");
        if (subtitleList.getLength() > 0) {
            config.setSubtitle(subtitleList.item(0).getTextContent().trim());
        }

        NodeList clientInfoList = root.getElementsByTagName("showClientInfo");
        if (clientInfoList.getLength() > 0) {
            config.setShowClientInfo("true".equalsIgnoreCase(clientInfoList.item(0).getTextContent().trim()));
        }

        NodeList genDateList = root.getElementsByTagName("showGeneratedDate");
        if (genDateList.getLength() > 0) {
            config.setShowGeneratedDate("true".equalsIgnoreCase(genDateList.item(0).getTextContent().trim()));
        }

        NodeList tableList = root.getElementsByTagName("table");
        if (tableList.getLength() > 0) {
            Element tableElem = (Element) tableList.item(0);
            NodeList colsList = tableElem.getElementsByTagName("column");
            if (colsList.getLength() > 0) {
                List<String> parsedCols = new ArrayList<>();
                for (int i = 0; i < colsList.getLength(); i++) {
                    String colKey = colsList.item(i).getTextContent().trim();
                    if (ReportLayoutConfig.VALID_COLUMNS.contains(colKey)) {
                        parsedCols.add(colKey);
                    }
                }
                if (!parsedCols.isEmpty()) {
                    config.setColumns(parsedCols);
                }
            }
        }

        NodeList footerList = root.getElementsByTagName("footerNote");
        if (footerList.getLength() > 0) {
            config.setFooterNote(footerList.item(0).getTextContent().trim());
        }

        return config;
    }

    public Map<String, Object> generatePortfolioSummary(String clientId, String periode) {
        String sql = "SELECT SUM(nilai) as total FROM portfolio_assets WHERE client_id = ?";
        return jdbcTemplate.queryForMap(sql, clientId);
    }

    /**
     * Overloaded method default layout PDF.
     */
    public byte[] generatePortfolioPdfBytes(Client client, PortfolioReportSummary summary, List<PortfolioAsset> assets) {
        return generatePortfolioPdfBytes(client, summary, assets, new ReportLayoutConfig());
    }

    /**
     * Generate dokumen PDF laporan portofolio menggunakan iTextPDF 5.5.13.3
     * sesuai konfigurasi layout ReportLayoutConfig.
     */
    public byte[] generatePortfolioPdfBytes(Client client, PortfolioReportSummary summary, List<PortfolioAsset> assets, ReportLayoutConfig layoutConfig) {
        if (layoutConfig == null) {
            layoutConfig = new ReportLayoutConfig();
        }
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

            // Judul laporan dinamis
            Paragraph title = new Paragraph(layoutConfig.getTitle(), fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            // Subjudul dinamis
            Paragraph subtitle = new Paragraph(layoutConfig.getSubtitle(), fontSubtitle);
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

            // Tabel info klien (jika dikonfigurasi showClientInfo = true)
            if (layoutConfig.isShowClientInfo()) {
                PdfPTable infoTable = new PdfPTable(2);
                infoTable.setWidthPercentage(65);
                infoTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                infoTable.setWidths(new int[]{3, 5});
                infoTable.setSpacingAfter(20);

                addInfoRow(infoTable, "Nama Klien", client.getNama() != null ? client.getNama() : "-", fontLabel, fontNormal);
                addInfoRow(infoTable, "NIK", client.getNik() != null ? client.getNik() : "-", fontLabel, fontNormal);
                addInfoRow(infoTable, "Periode", summary.getPeriode() != null ? summary.getPeriode() : "-", fontLabel, fontNormal);
                addInfoRow(infoTable, "Total Nilai", CurrencyUtil.format(summary.getTotalNilai()), fontLabel, fontNormal);

                if (layoutConfig.isShowGeneratedDate() && summary.getGeneratedAt() != null) {
                    String generatedAt = summary.getGeneratedAt().toString().replace("T", " ");
                    addInfoRow(infoTable, "Dibuat Pada", generatedAt, fontLabel, fontNormal);
                }
                document.add(infoTable);
            }

            // Judul tabel rincian
            Paragraph sectionTitle = new Paragraph("Rincian Instrumen Portofolio", fontSectionTitle);
            sectionTitle.setSpacingAfter(8);
            document.add(sectionTitle);

            // Tabel rincian aset berdasar kolom terpilih
            List<String> activeColumns = layoutConfig.getColumns();
            int numCols = activeColumns.size();

            // Mapping header label & width per column
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("jenisInstrumen", "Jenis Instrumen");
            headerMap.put("namaInstrumen", "Nama Instrumen");
            headerMap.put("jumlah", "Jumlah Unit");
            headerMap.put("nilai", "Nilai (IDR)");
            headerMap.put("allocationPercent", "Alokasi (%)");

            Map<String, Integer> widthMap = new HashMap<>();
            widthMap.put("jenisInstrumen", 2);
            widthMap.put("namaInstrumen", 4);
            widthMap.put("jumlah", 2);
            widthMap.put("nilai", 3);
            widthMap.put("allocationPercent", 2);

            int[] widths = new int[numCols];
            for (int i = 0; i < numCols; i++) {
                widths[i] = widthMap.getOrDefault(activeColumns.get(i), 2);
            }

            PdfPTable assetsTable = new PdfPTable(numCols);
            assetsTable.setWidthPercentage(100);
            assetsTable.setWidths(widths);
            assetsTable.setSpacingAfter(20);
            assetsTable.setHeaderRows(1);

            BaseColor headerBg = new BaseColor(15, 23, 42);
            for (String colKey : activeColumns) {
                String headerText = headerMap.getOrDefault(colKey, colKey);
                PdfPCell cell = new PdfPCell(new Phrase(headerText, fontTableHeader));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setBorderColor(new BaseColor(30, 41, 59));
                assetsTable.addCell(cell);
            }

            if (assets == null || assets.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Tidak ada data instrumen.", fontTableCell));
                emptyCell.setColspan(numCols);
                emptyCell.setPadding(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                assetsTable.addCell(emptyCell);
            } else {
                boolean even = false;
                BaseColor rowEven = new BaseColor(248, 250, 252);
                for (PortfolioAsset a : assets) {
                    BaseColor rowBg = even ? rowEven : BaseColor.WHITE;
                    for (String colKey : activeColumns) {
                        String valStr = "-";
                        if ("jenisInstrumen".equals(colKey)) {
                            valStr = a.getJenisInstrumen();
                        } else if ("namaInstrumen".equals(colKey)) {
                            valStr = a.getNamaInstrumen();
                        } else if ("jumlah".equals(colKey)) {
                            valStr = String.valueOf((long) a.getJumlah());
                        } else if ("nilai".equals(colKey)) {
                            valStr = CurrencyUtil.format(a.getNilai());
                        } else if ("allocationPercent".equals(colKey)) {
                            valStr = String.format("%.2f", a.getAllocationPercent()) + "%";
                        }
                        addAssetCell(assetsTable, valStr, fontTableCell, rowBg);
                    }
                    even = !even;
                }
            }
            document.add(assetsTable);

            // Footer dinamis
            Paragraph footer = new Paragraph(layoutConfig.getFooterNote() + " No. Ref: " + summary.getId(), fontSubtitle);
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