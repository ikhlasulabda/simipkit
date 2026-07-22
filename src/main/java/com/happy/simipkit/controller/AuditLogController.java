package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
