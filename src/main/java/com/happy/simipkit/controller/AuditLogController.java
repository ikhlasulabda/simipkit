package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String viewAuditLogs(Model model) {
        model.addAttribute("logs", auditLogService.getAllLogs());
        return "audit-log";
    }

    @PostMapping("/delete-all")
    public String deleteAllAuditLogs(HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        auditLogService.deleteAllLogs();
        auditLogService.logAction(userId, "AUDIT_LOG_DELETE_ALL", request.getRemoteAddr(),
                "Hapus SELURUH catatan audit log sistem");
        return "redirect:/audit-log";
    }
}
