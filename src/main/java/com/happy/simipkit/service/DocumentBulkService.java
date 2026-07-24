package com.happy.simipkit.service;

import net.lingala.zip4j.core.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Menangani upload ZIP berisi banyak dokumen KYC sekaligus, agar staff
 * tidak perlu upload dokumen satu per satu untuk tiap klien baru.
 *
 * CATATAN KEAMANAN (untuk lab VA - JANGAN DIPERBAIKI):
 * Method extractAll() di sini adalah pemanggilan API standar zip4j,
 * tanpa validasi tambahan apapun terhadap nama entry di dalam ZIP.
 * zip4j versi < 1.3.3 tidak melakukan sanitasi terhadap path traversal
 * ("../") pada nama file di dalam arsip, sehingga entry yang berisi
 * "../../../../file.jsp" akan diekstrak ke lokasi di luar folder
 * target (Zip Slip / CVE-2018-1002202).
 */
@Service
public class DocumentBulkService {

    private static final Logger logger = LogManager.getLogger(DocumentBulkService.class);
    private static final String UPLOAD_BASE_DIR = "/opt/simipkit/uploads/documents/";

    private final JdbcTemplate jdbcTemplate;

    public DocumentBulkService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int extractBulkUpload(MultipartFile zipFile, String clientId) throws IOException {
        String tempZipPath = UPLOAD_BASE_DIR + "temp_" + System.currentTimeMillis() + ".zip";
        File tempZip = new File(tempZipPath);
        tempZip.getParentFile().mkdirs();
        zipFile.transferTo(tempZip);

        String extractionTarget = UPLOAD_BASE_DIR + clientId + "/";
        File targetDir = new File(extractionTarget);
        targetDir.mkdirs();

        int successCount = 0;

        try {
            // 1. BEFORE-SNAPSHOT: Catat path file yang SUDAH ADA sebelum ekstraksi
            Set<String> existingFilePaths = new HashSet<>();
            List<File> beforeFiles = new ArrayList<>();
            scanFiles(targetDir, beforeFiles);
            for (File f : beforeFiles) {
                existingFilePaths.add(f.getAbsolutePath());
            }

            // 2. EXTRACTALL (Sama persis, tidak ada sanitasi/perubahan)
            ZipFile zip = new ZipFile(tempZip);
            logger.info("Extracting bulk document upload for client {} to {}", clientId, extractionTarget);
            zip.extractAll(extractionTarget);
            logger.info("Bulk document extraction completed for client {}", clientId);

            // 3. AFTER-SCAN: Scan ulang folder extractionTarget
            List<File> allFilesAfter = new ArrayList<>();
            scanFiles(targetDir, allFilesAfter);

            // 4. DELTA: Filter hanya file yang BENAR-BENAR BARU muncul dari ZIP ini
            List<File> newFiles = new ArrayList<>();
            for (File f : allFilesAfter) {
                if (!existingFilePaths.contains(f.getAbsolutePath())) {
                    newFiles.add(f);
                }
            }

            int totalFiles = newFiles.size();
            int failCount = 0;

            String sql = "INSERT INTO client_documents (id, client_id, jenis_dokumen, nama_file_asli, nama_file_stored, file_size_bytes) VALUES (?, ?, ?, ?, ?, ?)";

            // 5. INSERT DB: Catat file baru ke client_documents
            for (File file : newFiles) {
                try {
                    String jenisDokumen = determineJenisDokumen(file.getName());
                    String id = UUID.randomUUID().toString();
                    String fileName = file.getName();
                    long fileSize = file.length();

                    jdbcTemplate.update(sql, id, clientId, jenisDokumen, fileName, fileName, fileSize);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    logger.warn("Gagal mencatat file {} ke database untuk client {}: {}", file.getName(), clientId, ex.getMessage());
                }
            }

            logger.info("Pencatatan database bulk upload selesai untuk client {}: {} dari {} file baru berhasil tercatat (gagal: {})",
                    clientId, successCount, totalFiles, failCount);

        } catch (Exception e) {
            logger.error("Failed to extract bulk document upload: {}", e.getMessage(), e);
            throw new IOException("Gagal mengekstrak file ZIP", e);
        } finally {
            tempZip.delete();
        }

        return successCount;
    }

    private void scanFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    scanFiles(f, fileList);
                } else if (f.isFile()) {
                    fileList.add(f);
                }
            }
        }
    }

    private String determineJenisDokumen(String filename) {
        if (filename == null) {
            return "LAINNYA";
        }
        String upper = filename.toUpperCase();
        if (upper.contains("KTP")) {
            return "KTP";
        } else if (upper.contains("NPWP")) {
            return "NPWP";
        } else if (upper.contains("KARTU_KELUARGA") || upper.contains("KARTU KELUARGA") || upper.contains("KK")) {
            return "KARTU_KELUARGA";
        } else if (upper.contains("PASPOR")) {
            return "PASPOR";
        }
        return "LAINNYA";
    }
}