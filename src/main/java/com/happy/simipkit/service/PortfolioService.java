package com.happy.simipkit.service;

import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.model.PortfolioReportSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioService {

    private static final Logger logger = LogManager.getLogger(PortfolioService.class);
    private final JdbcTemplate jdbcTemplate;

    public PortfolioService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PortfolioAsset> getAssetsByClientId(String clientId) {
        String sql = "SELECT id, client_id, jenis_instrumen, nama_instrumen, jumlah, nilai, allocation_percent, updated_at FROM portfolio_assets WHERE client_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, new PortfolioAssetRowMapper(), clientId);
    }

    public PortfolioAsset getAssetById(Integer id) {
        String sql = "SELECT id, client_id, jenis_instrumen, nama_instrumen, jumlah, nilai, allocation_percent, updated_at FROM portfolio_assets WHERE id = ?";
        List<PortfolioAsset> list = jdbcTemplate.query(sql, new PortfolioAssetRowMapper(), id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void addAsset(PortfolioAsset asset) {
        logger.info("Menambahkan aset portofolio untuk client: {}, instrumen: {}", asset.getClientId(), asset.getNamaInstrumen());
        String sql = "INSERT INTO portfolio_assets (client_id, jenis_instrumen, nama_instrumen, jumlah, nilai, allocation_percent) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, asset.getClientId(), asset.getJenisInstrumen(), asset.getNamaInstrumen(), asset.getJumlah(), asset.getNilai(), asset.getAllocationPercent());
    }

    public void updateAsset(PortfolioAsset asset) {
        logger.info("Memperbarui aset portofolio id: {}", asset.getId());
        String sql = "UPDATE portfolio_assets SET jenis_instrumen = ?, nama_instrumen = ?, jumlah = ?, nilai = ?, allocation_percent = ? WHERE id = ?";
        jdbcTemplate.update(sql, asset.getJenisInstrumen(), asset.getNamaInstrumen(), asset.getJumlah(), asset.getNilai(), asset.getAllocationPercent(), asset.getId());
    }

    public void deleteAsset(Integer id) {
        logger.info("Menghapus aset portofolio id: {}", id);
        String sql = "DELETE FROM portfolio_assets WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public double getTotalPortfolioValue(String clientId) {
        String sql = "SELECT COALESCE(SUM(nilai), 0) FROM portfolio_assets WHERE client_id = ?";
        Double total = jdbcTemplate.queryForObject(sql, Double.class, clientId);
        return total != null ? total : 0.0;
    }

    public int saveReportSummary(PortfolioReportSummary summary, List<PortfolioAsset> assets) {
        logger.info("Menyimpan summary laporan portofolio untuk client: {}, periode: {}", summary.getClientId(), summary.getPeriode());
        String snapshot = serializeAssetsToJson(assets);
        summary.setAssetsSnapshot(snapshot);
        String sql = "INSERT INTO portfolio_report_summary (client_id, periode, total_nilai, assets_snapshot) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, summary.getClientId());
            ps.setString(2, summary.getPeriode());
            ps.setDouble(3, summary.getTotalNilai());
            ps.setString(4, snapshot);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.intValue() : -1;
    }

    public List<PortfolioReportSummary> getSummariesByClientId(String clientId) {
        String sql = "SELECT id, client_id, periode, total_nilai, generated_at FROM portfolio_report_summary WHERE client_id = ? ORDER BY generated_at DESC";
        return jdbcTemplate.query(sql, new PortfolioReportSummaryRowMapper(), clientId);
    }

    public List<PortfolioReportSummary> getAllReportSummaries() {
        String sql = "SELECT id, client_id, periode, total_nilai, generated_at FROM portfolio_report_summary ORDER BY generated_at DESC";
        return jdbcTemplate.query(sql, new PortfolioReportSummaryRowMapper());
    }

    public PortfolioReportSummary getSummaryById(int summaryId) {
        String sql = "SELECT id, client_id, periode, total_nilai, generated_at, assets_snapshot " +
                     "FROM portfolio_report_summary WHERE id = ?";
        List<PortfolioReportSummary> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            PortfolioReportSummary s = new PortfolioReportSummary();
            s.setId(rs.getInt("id"));
            s.setClientId(rs.getString("client_id"));
            s.setPeriode(rs.getString("periode"));
            s.setTotalNilai(rs.getDouble("total_nilai"));
            if (rs.getTimestamp("generated_at") != null) {
                s.setGeneratedAt(rs.getTimestamp("generated_at").toLocalDateTime());
            }
            s.setAssetsSnapshot(rs.getString("assets_snapshot"));
            return s;
        }, summaryId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<PortfolioAsset> deserializeAssetsSnapshot(String json) {
        return deserializeAssetsFromJson(json);
    }

    // -------------------------------------------------------------------------
    // JSON serialization/deserialization helpers (manual, tanpa library eksternal)
    // Format: flat array of object, misal:
    // [{"jenisInstrumen":"SAHAM","namaInstrumen":"PT XYZ","jumlah":100.0,"nilai":5000000.0,"allocationPercent":30.0}]
    // -------------------------------------------------------------------------

    private static String escapeJsonString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String serializeAssetsToJson(List<PortfolioAsset> assets) {
        if (assets == null || assets.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < assets.size(); i++) {
            PortfolioAsset a = assets.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"jenisInstrumen\":\"").append(escapeJsonString(a.getJenisInstrumen())).append("\",");
            sb.append("\"namaInstrumen\":\"").append(escapeJsonString(a.getNamaInstrumen())).append("\",");
            sb.append("\"jumlah\":").append(BigDecimal.valueOf(a.getJumlah()).toPlainString()).append(",");
            sb.append("\"nilai\":").append(BigDecimal.valueOf(a.getNilai()).toPlainString()).append(",");
            sb.append("\"allocationPercent\":").append(BigDecimal.valueOf(a.getAllocationPercent()).toPlainString());
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static List<PortfolioAsset> deserializeAssetsFromJson(String json) {
        List<PortfolioAsset> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) return result;
        int pos = 0;
        while (pos < json.length()) {
            int start = json.indexOf('{', pos);
            if (start < 0) break;
            int end = json.indexOf('}', start);
            if (end < 0) break;
            String obj = json.substring(start + 1, end);
            PortfolioAsset asset = new PortfolioAsset();
            asset.setJenisInstrumen(extractStringValue(obj, "jenisInstrumen"));
            asset.setNamaInstrumen(extractStringValue(obj, "namaInstrumen"));
            asset.setJumlah(extractDoubleValue(obj, "jumlah"));
            asset.setNilai(extractDoubleValue(obj, "nilai"));
            asset.setAllocationPercent(extractDoubleValue(obj, "allocationPercent"));
            result.add(asset);
            pos = end + 1;
        }
        return result;
    }

    private static String extractStringValue(String obj, String key) {
        String search = "\"" + key + "\":\"";
        int idx = obj.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        StringBuilder value = new StringBuilder();
        int i = start;
        while (i < obj.length()) {
            char c = obj.charAt(i);
            if (c == '\\' && i + 1 < obj.length()) {
                char next = obj.charAt(i + 1);
                if (next == '"') value.append('"');
                else if (next == '\\') value.append('\\');
                else if (next == 'n') value.append('\n');
                else if (next == 'r') value.append('\r');
                else if (next == 't') value.append('\t');
                else value.append(next);
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
                i++;
            }
        }
        return value.toString();
    }

    private static double extractDoubleValue(String obj, String key) {
        String search = "\"" + key + "\":";
        int idx = obj.indexOf(search);
        if (idx < 0) return 0.0;
        int start = idx + search.length();
        int end = start;
        while (end < obj.length()) {
            char c = obj.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'E' || c == 'e') {
                end++;
            } else {
                break;
            }
        }
        try {
            return Double.parseDouble(obj.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static class PortfolioAssetRowMapper implements RowMapper<PortfolioAsset> {
        @Override
        public PortfolioAsset mapRow(ResultSet rs, int rowNum) throws SQLException {
            PortfolioAsset a = new PortfolioAsset();
            a.setId(rs.getInt("id"));
            a.setClientId(rs.getString("client_id"));
            a.setJenisInstrumen(rs.getString("jenis_instrumen"));
            a.setNamaInstrumen(rs.getString("nama_instrumen"));
            a.setJumlah(rs.getDouble("jumlah"));
            a.setNilai(rs.getDouble("nilai"));
            a.setAllocationPercent(rs.getDouble("allocation_percent"));
            if (rs.getTimestamp("updated_at") != null) {
                a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            return a;
        }
    }

    private static class PortfolioReportSummaryRowMapper implements RowMapper<PortfolioReportSummary> {
        @Override
        public PortfolioReportSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            PortfolioReportSummary s = new PortfolioReportSummary();
            s.setId(rs.getInt("id"));
            s.setClientId(rs.getString("client_id"));
            s.setPeriode(rs.getString("periode"));
            s.setTotalNilai(rs.getDouble("total_nilai"));
            if (rs.getTimestamp("generated_at") != null) {
                s.setGeneratedAt(rs.getTimestamp("generated_at").toLocalDateTime());
            }
            return s;
        }
    }
}
