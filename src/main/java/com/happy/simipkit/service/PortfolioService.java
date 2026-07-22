package com.happy.simipkit.service;

import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.model.PortfolioReportSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    public void saveReportSummary(PortfolioReportSummary summary) {
        logger.info("Menyimpan summary laporan portofolio untuk client: {}, periode: {}", summary.getClientId(), summary.getPeriode());
        String sql = "INSERT INTO portfolio_report_summary (client_id, periode, total_nilai) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, summary.getClientId(), summary.getPeriode(), summary.getTotalNilai());
    }

    public List<PortfolioReportSummary> getSummariesByClientId(String clientId) {
        String sql = "SELECT id, client_id, periode, total_nilai, generated_at FROM portfolio_report_summary WHERE client_id = ? ORDER BY generated_at DESC";
        return jdbcTemplate.query(sql, new PortfolioReportSummaryRowMapper(), clientId);
    }

    public List<PortfolioReportSummary> getAllReportSummaries() {
        String sql = "SELECT id, client_id, periode, total_nilai, generated_at FROM portfolio_report_summary ORDER BY generated_at DESC";
        return jdbcTemplate.query(sql, new PortfolioReportSummaryRowMapper());
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
