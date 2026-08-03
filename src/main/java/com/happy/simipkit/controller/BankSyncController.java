package com.happy.simipkit.controller;

import com.happy.simipkit.config.AppConfig;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.BankSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/api/sync")
public class BankSyncController {

    private static final Logger logger = LogManager.getLogger(BankSyncController.class);

    private final BankSyncService bankSyncService;
    private final AuditLogService auditLogService;
    private final ObjectMapper mapper;

    public BankSyncController(BankSyncService bankSyncService, AuditLogService auditLogService) {
        this.bankSyncService = bankSyncService;
        this.auditLogService = auditLogService;
        this.mapper = new ObjectMapper();
    }

    @PostMapping("/bank-feed")
    @ResponseBody
    public ResponseEntity<Map<String, String>> receiveBankFeed(
            @RequestBody String rawJsonPayload,
            @RequestHeader(value = "X-Signature", required = false) String signatureHeader,
            HttpServletRequest request) {

        String bankPartnerCode = "UNKNOWN";
        try {
            JsonNode rootNode = mapper.readTree(rawJsonPayload);
            if (rootNode != null && rootNode.has("bankPartnerCode")) {
                bankPartnerCode = rootNode.get("bankPartnerCode").asText();
            }
        } catch (Exception e) {
            // Keep UNKNOWN if parse fails
        }

        String secret = AppConfig.getSecretEnvOrDefault("BANK_SYNC_SHARED_SECRET", AppConfig.BANK_SYNC_SHARED_SECRET_DEFAULT);

        boolean isSignatureValid = false;
        if (signatureHeader != null && !signatureHeader.trim().isEmpty()) {
            try {
                String calculatedSig = calculateHmac(rawJsonPayload, secret);
                isSignatureValid = MessageDigest.isEqual(
                        calculatedSig.getBytes(StandardCharsets.UTF_8),
                        signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
                );
            } catch (Exception e) {
                logger.error("Error calculating HMAC-SHA256 signature", e);
            }
        }

        if (!isSignatureValid) {
            auditLogService.logAction(null, "BANK_FEED_SIGNATURE_REJECTED", request.getRemoteAddr(),
                    "Signature rejection for bank feed. Bank Partner Code: " + bankPartnerCode);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Invalid or missing signature"));
        }

        try {
            // Teruskan raw json payload apa adanya ke service
            bankSyncService.processIncomingFeed(rawJsonPayload);
            return ResponseEntity.ok(Collections.singletonMap("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    private String calculateHmac(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
