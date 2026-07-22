package com.happy.simipkit.service;

import com.thoughtworks.xstream.XStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
}