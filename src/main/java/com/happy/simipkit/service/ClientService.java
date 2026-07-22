package com.happy.simipkit.service;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.ClientDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private static final Logger logger = LogManager.getLogger(ClientService.class);
    private final JdbcTemplate jdbcTemplate;

    public ClientService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Client> getAllClients() {
        String sql = "SELECT id, nama, nik, alamat, status_kyc, created_at FROM clients ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ClientRowMapper());
    }

    public Client getClientById(String id) {
        String sql = "SELECT id, nama, nik, alamat, status_kyc, created_at FROM clients WHERE id = ?";
        List<Client> list = jdbcTemplate.query(sql, new ClientRowMapper(), id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void createClient(Client client) {
        if (client.getId() == null || client.getId().trim().isEmpty()) {
            client.setId(UUID.randomUUID().toString());
        }
        if (client.getStatusKyc() == null || client.getStatusKyc().trim().isEmpty()) {
            client.setStatusKyc("PENDING");
        }
        logger.info("Membuat data klien baru: {}, NIK: {}", client.getNama(), client.getNik());
        String sql = "INSERT INTO clients (id, nama, nik, alamat, status_kyc) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, client.getId(), client.getNama(), client.getNik(), client.getAlamat(), client.getStatusKyc());
    }

    public void updateClient(Client client) {
        logger.info("Memperbarui data klien id: {}", client.getId());
        String sql = "UPDATE clients SET nama = ?, nik = ?, alamat = ?, status_kyc = ? WHERE id = ?";
        jdbcTemplate.update(sql, client.getNama(), client.getNik(), client.getAlamat(), client.getStatusKyc(), client.getId());
    }

    public void deleteClient(String id) {
        logger.info("Menghapus klien id: {}", id);
        String sql = "DELETE FROM clients WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // Document operations
    public void addDocument(ClientDocument doc) {
        if (doc.getId() == null || doc.getId().trim().isEmpty()) {
            doc.setId(UUID.randomUUID().toString());
        }
        logger.info("Menambahkan dokumen KYC untuk client: {}, file: {}", doc.getClientId(), doc.getNamaFileAsli());
        String sql = "INSERT INTO client_documents (id, client_id, jenis_dokumen, nama_file_asli, nama_file_stored, file_size_bytes) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, doc.getId(), doc.getClientId(), doc.getJenisDokumen(), doc.getNamaFileAsli(), doc.getNamaFileStored(), doc.getFileSizeBytes());
    }

    public List<ClientDocument> getDocumentsByClientId(String clientId) {
        String sql = "SELECT id, client_id, jenis_dokumen, nama_file_asli, nama_file_stored, file_size_bytes, uploaded_at FROM client_documents WHERE client_id = ? ORDER BY uploaded_at DESC";
        return jdbcTemplate.query(sql, new ClientDocumentRowMapper(), clientId);
    }

    public ClientDocument getDocumentById(String documentId) {
        String sql = "SELECT id, client_id, jenis_dokumen, nama_file_asli, nama_file_stored, file_size_bytes, uploaded_at FROM client_documents WHERE id = ?";
        List<ClientDocument> list = jdbcTemplate.query(sql, new ClientDocumentRowMapper(), documentId);
        return list.isEmpty() ? null : list.get(0);
    }

    public void deleteDocument(String documentId) {
        logger.info("Menghapus dokumen id: {}", documentId);
        String sql = "DELETE FROM client_documents WHERE id = ?";
        jdbcTemplate.update(sql, documentId);
    }

    private static class ClientRowMapper implements RowMapper<Client> {
        @Override
        public Client mapRow(ResultSet rs, int rowNum) throws SQLException {
            Client c = new Client();
            c.setId(rs.getString("id"));
            c.setNama(rs.getString("nama"));
            c.setNik(rs.getString("nik"));
            c.setAlamat(rs.getString("alamat"));
            c.setStatusKyc(rs.getString("status_kyc"));
            if (rs.getTimestamp("created_at") != null) {
                c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return c;
        }
    }

    private static class ClientDocumentRowMapper implements RowMapper<ClientDocument> {
        @Override
        public ClientDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
            ClientDocument d = new ClientDocument();
            d.setId(rs.getString("id"));
            d.setClientId(rs.getString("client_id"));
            d.setJenisDokumen(rs.getString("jenis_dokumen"));
            d.setNamaFileAsli(rs.getString("nama_file_asli"));
            d.setNamaFileStored(rs.getString("nama_file_stored"));
            d.setFileSizeBytes(rs.getLong("file_size_bytes"));
            if (rs.getTimestamp("uploaded_at") != null) {
                d.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
            }
            return d;
        }
    }
}
