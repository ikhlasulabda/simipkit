package com.happy.simipkit.util;

import java.util.UUID;

/**
 * Utility untuk menghasilkan nama file unik berbasis UUID.
 * Mencegah bentrokan/tumpang tindih nama file saat upload dokumen.
 */
public class FileNamingUtil {

    public static String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}
