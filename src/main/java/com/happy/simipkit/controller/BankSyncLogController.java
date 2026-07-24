package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bank-sync-log")
public class BankSyncLogController {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public BankSyncLogController(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String showSyncLogs(Model model) {
        String sql = "SELECT id, event_type, payload_raw, status, processed_at FROM bank_sync_events ORDER BY processed_at DESC LIMIT 100";
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
