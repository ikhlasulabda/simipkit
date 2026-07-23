package com.happy.simipkit.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Utility pemformatan nilai mata uang IDR.
 * Menggunakan format Indonesia: pemisah ribuan titik, tanpa desimal.
 * Rentang aman sampai Rp 999.999.999.999.999 (15 digit).
 *
 * Contoh output:
 *   10500000.0  ->  "Rp 10.500.000"
 *   1.05E7      ->  "Rp 10.500.000"
 *   750.0       ->  "Rp 750"
 */
public class CurrencyUtil {

    // DecimalFormat tidak thread-safe; buat instance baru tiap pemanggilan
    // (static pattern saja, bukan instance)
    private static final String PATTERN = "#,###";

    private CurrencyUtil() {}

    /**
     * Format angka double ke string IDR penuh tanpa notasi ilmiah.
     * @param amount nilai dalam Rupiah (boleh double besar)
     * @return string siap tampil, contoh "Rp 10.500.000"
     */
    public static String format(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat(PATTERN, symbols);
        return "Rp " + df.format((long) amount);
    }

    /**
     * Overload untuk Number (menangani Long, Integer, Float, Double, BigDecimal)
     * yang mungkin datang dari JdbcTemplate queryForMap.
     */
    public static String format(Number amount) {
        if (amount == null) return "Rp 0";
        return format(amount.doubleValue());
    }
}
