package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ClientService clientService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public HomeController(ClientService clientService, UserService userService, AuditLogService auditLogService) {
        this.clientService = clientService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalClients", clientService.getAllClients().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("recentAuditLogs", auditLogService.getAllLogs());
        return "dashboard";
    }
}
