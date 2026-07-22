package com.happy.simipkit.service;

import net.lingala.zip4j.core.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

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

    public void extractBulkUpload(MultipartFile zipFile, String clientId) throws IOException {
        String tempZipPath = UPLOAD_BASE_DIR + "temp_" + System.currentTimeMillis() + ".zip";
        File tempZip = new File(tempZipPath);
        tempZip.getParentFile().mkdirs();
        zipFile.transferTo(tempZip);

        String extractionTarget = UPLOAD_BASE_DIR + clientId + "/";
        new File(extractionTarget).mkdirs();

        try {
            ZipFile zip = new ZipFile(tempZip);
            logger.info("Extracting bulk document upload for client {} to {}", clientId, extractionTarget);
            zip.extractAll(extractionTarget);
            logger.info("Bulk document extraction completed for client {}", clientId);
        } catch (Exception e) {
            logger.error("Failed to extract bulk document upload: {}", e.getMessage(), e);
            throw new IOException("Gagal mengekstrak file ZIP", e);
        } finally {
            tempZip.delete();
        }
    }
}