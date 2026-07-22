package com.happy.simipkit.service;

import com.happy.simipkit.model.User;
import com.happy.simipkit.security.PasswordHasher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> getAllUsers() {
        String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users ORDER BY id ASC";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users WHERE username = ?";
        List<User> list = jdbcTemplate.query(sql, new UserRowMapper(), username);
        return list.isEmpty() ? null : list.get(0);
    }

    public User findById(Integer id) {
        String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users WHERE id = ?";
        List<User> list = jdbcTemplate.query(sql, new UserRowMapper(), id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void createUser(User user, String plainPassword) {
        logger.info("Membuat user baru: {}, role: {}", user.getUsername(), user.getRole());
        String passwordHash = PasswordHasher.hash(plainPassword);
        user.setPasswordHash(passwordHash);

        String sql = "INSERT INTO users (username, password_hash, role, is_active) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getUsername(), user.getPasswordHash(), 
                user.getRole() != null ? user.getRole() : "staff", user.isActive());
    }

    public void updateUser(User user, String newPlainPassword) {
        logger.info("Memperbarui user: id={}", user.getId());
        if (newPlainPassword != null && !newPlainPassword.trim().isEmpty()) {
            String passwordHash = PasswordHasher.hash(newPlainPassword);
            String sql = "UPDATE users SET username = ?, password_hash = ?, role = ?, is_active = ? WHERE id = ?";
            jdbcTemplate.update(sql, user.getUsername(), passwordHash, user.getRole(), user.isActive(), user.getId());
        } else {
            String sql = "UPDATE users SET username = ?, role = ?, is_active = ? WHERE id = ?";
            jdbcTemplate.update(sql, user.getUsername(), user.getRole(), user.isActive(), user.getId());
        }
    }

    public void deleteUser(Integer id) {
        logger.info("Menghapus user id: {}", id);
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User u = new User();
            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setPasswordHash(rs.getString("password_hash"));
            u.setRole(rs.getString("role"));
            u.setActive(rs.getBoolean("is_active"));
            if (rs.getTimestamp("created_at") != null) {
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return u;
        }
    }
}
