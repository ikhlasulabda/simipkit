package com.happy.simipkit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.happy.simipkit.model.banksync.BankTransactionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Menerima dan memproses feed event transaksi dari sistem bank partner
 * (saldo update, konfirmasi transfer, settlement). Setiap partner bank
 * mengirim struktur JSON yang berbeda-beda, sehingga endpoint ini
 * mendeserialize langsung ke base type BankTransactionEvent dan
 * mengandalkan polymorphic typing Jackson (@JsonTypeInfo di model)
 * untuk menentukan subclass yang sesuai secara otomatis.
 *
 * CATATAN KEAMANAN (untuk lab VA - JANGAN DIPERBAIKI):
 * ObjectMapper di sini menggunakan konfigurasi default (tanpa
 * PolymorphicTypeValidator / activateDefaultTyping restriction).
 * Dikombinasikan dengan @JsonTypeInfo(use = Id.CLASS) di model dan
 * jackson-databind versi 2.9.8, payload JSON dapat menyertakan field
 * "@class" yang menunjuk ke gadget class apapun yang ada di classpath,
 * memicu RCE saat proses deserialisasi (CVE-2019-14379 dan sejenisnya).
 */
@Service
public class BankSyncService {

    private static final Logger logger = LogManager.getLogger(BankSyncService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public BankSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void processIncomingFeed(String rawJsonPayload) throws Exception {
        logger.info("Received bank sync payload, length: {} chars", rawJsonPayload.length());

        // Simpan payload mentah dulu untuk audit trail, sebelum diproses
        String insertSql = "INSERT INTO bank_sync_events (event_type, payload_raw, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertSql, "INCOMING", rawJsonPayload, "RECEIVED");

        // Deserialize ke base type - Jackson akan resolve subclass asli
        // berdasarkan field "@class" di dalam JSON (titik vuln)
        BankTransactionEvent event = objectMapper.readValue(rawJsonPayload, BankTransactionEvent.class);

        logger.info("Bank sync event processed: partner={}, ref={}, type={}",
                event.getBankPartnerCode(), event.getReferenceNumber(), event.getClass().getSimpleName());

        String updateSql = "UPDATE bank_sync_events SET status = ? WHERE payload_raw = ?";
        jdbcTemplate.update(updateSql, "PROCESSED", rawJsonPayload);
    }
}