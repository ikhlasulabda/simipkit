package com.happy.simipkit.service;

import com.happy.simipkit.model.AuditLogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private static final Logger logger = LogManager.getLogger(AuditLogService.class);
    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Mencatat aksi pengguna ke database audit_log dan ke Log4j logger.
     */
    public void logAction(Integer userId, String action, String ipAddress, String detail) {
        // Log4j INFO level logging parameter input user
        logger.info("AUDIT LOG -> User: {}, Action: {}, IP: {}, Detail: {}", userId, action, ipAddress, detail);

        String sql = "INSERT INTO audit_log (user_id, action, ip_address, detail) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, userId, action, ipAddress, detail);
        } catch (Exception e) {
            logger.error("Gagal menyimpan audit log ke database: {}", e.getMessage(), e);
        }
    }

    public List<AuditLogEntry> getAllLogs() {
        String sql = "SELECT id, user_id, action, ip_address, detail, timestamp FROM audit_log ORDER BY timestamp DESC LIMIT 200";
        return jdbcTemplate.query(sql, new AuditLogRowMapper());
    }

    public void deleteAllLogs() {
        String sql = "DELETE FROM audit_log";
        jdbcTemplate.update(sql);
    }

    private static class AuditLogRowMapper implements RowMapper<AuditLogEntry> {
        @Override
        public AuditLogEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            AuditLogEntry entry = new AuditLogEntry();
            entry.setId(rs.getInt("id"));
            int uid = rs.getInt("user_id");
            entry.setUserId(rs.wasNull() ? null : uid);
            entry.setAction(rs.getString("action"));
            entry.setIpAddress(rs.getString("ip_address"));
            entry.setDetail(rs.getString("detail"));
            if (rs.getTimestamp("timestamp") != null) {
                entry.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
            }
            return entry;
        }
    }
}
