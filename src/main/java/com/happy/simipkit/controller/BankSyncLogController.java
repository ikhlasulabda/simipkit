package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.BankSyncReconciliationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bank-sync-log")
public class BankSyncLogController {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;
    private final BankSyncReconciliationService reconciliationService;

    public BankSyncLogController(JdbcTemplate jdbcTemplate,
                                 AuditLogService auditLogService,
                                 BankSyncReconciliationService reconciliationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public String showSyncLogs(Model model) {
        String sql = "SELECT id, event_type, payload_raw, status, reconciliation_status, matched_client_id, matched_client_id_secondary, synced_at, processed_at FROM bank_sync_events ORDER BY processed_at DESC LIMIT 100";
        List<Map<String, Object>> events = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> event : events) {
            String payload = (String) event.get("payload_raw");
            String defaultType = (String) event.get("event_type");
            String badge = defaultType != null ? defaultType : "INCOMING";
            String fullTitle = badge;
            String badgeClass = "badge-event-default";

            if (payload != null && payload.contains("\"@class\"")) {
                int classIdx = payload.indexOf("\"@class\"");
                int colonIdx = payload.indexOf(":", classIdx);
                if (colonIdx != -1) {
                    int startQuote = payload.indexOf("\"", colonIdx);
                    if (startQuote != -1) {
                        int endQuote = payload.indexOf("\"", startQuote + 1);
                        if (endQuote != -1) {
                            String fullClass = payload.substring(startQuote + 1, endQuote);
                            fullTitle = fullClass.contains(".") ? fullClass.substring(fullClass.lastIndexOf(".") + 1) : fullClass;

                            if (fullClass.contains("SaldoUpdateEvent")) {
                                badge = "SUE";
                                badgeClass = "badge-event-sue";
                            } else if (fullClass.contains("SettlementEvent")) {
                                badge = "SE";
                                badgeClass = "badge-event-se";
                            } else if (fullClass.contains("TransferConfirmationEvent")) {
                                badge = "TCE";
                                badgeClass = "badge-event-tce";
                            }
                        }
                    }
                }
            }

            event.put("event_badge", badge);
            event.put("event_full_title", fullTitle);
            event.put("event_badge_class", badgeClass);
        }

        model.addAttribute("events", events);
        return "bank-sync-log";
    }

    @PostMapping("/{id}/match")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> matchEvent(@PathVariable("id") int id, HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Sesi tidak valid / belum login.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        try {
            Map<String, Object> res = reconciliationService.matchEvent(id, userId, request.getRemoteAddr());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    @PostMapping("/{id}/sync")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncEvent(@PathVariable("id") int id, HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Sesi tidak valid / belum login.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        try {
            Map<String, Object> res = reconciliationService.syncEvent(id, userId, request.getRemoteAddr());
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    @PostMapping("/delete-all")
    public String deleteAllSyncLogs(HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        String sql = "DELETE FROM bank_sync_events";
        jdbcTemplate.update(sql);
        auditLogService.logAction(userId, "BANK_SYNC_LOG_DELETE_ALL", request.getRemoteAddr(),
                "Hapus SELURUH catatan log sinkronisasi bank feed");
        return "redirect:/bank-sync-log";
    }
}
