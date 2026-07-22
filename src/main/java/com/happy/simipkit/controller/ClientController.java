package com.happy.simipkit.controller;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.PortfolioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final PortfolioService portfolioService;
    private final AuditLogService auditLogService;

    public ClientController(ClientService clientService, PortfolioService portfolioService, AuditLogService auditLogService) {
        this.clientService = clientService;
        this.portfolioService = portfolioService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String listClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "client-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("isNew", true);
        return "client-form";
    }

    @PostMapping("/save")
    public String saveClient(@ModelAttribute("client") Client client, HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (client.getId() != null && !client.getId().trim().isEmpty() && clientService.getClientById(client.getId()) != null) {
            clientService.updateClient(client);
            auditLogService.logAction(userId, "CLIENT_UPDATE", request.getRemoteAddr(), "Update client ID: " + client.getId() + " - " + client.getNama());
        } else {
            clientService.createClient(client);
            auditLogService.logAction(userId, "CLIENT_CREATE", request.getRemoteAddr(), "Tambah client baru: " + client.getNama());
        }
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Client client = clientService.getClientById(id);
        if (client == null) {
            return "redirect:/clients";
        }
        model.addAttribute("client", client);
        model.addAttribute("isNew", false);
        return "client-form";
    }

    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable("id") String id, Model model) {
        Client client = clientService.getClientById(id);
        if (client == null) {
            return "redirect:/clients";
        }
        model.addAttribute("client", client);
        model.addAttribute("documents", clientService.getDocumentsByClientId(id));
        model.addAttribute("assets", portfolioService.getAssetsByClientId(id));
        model.addAttribute("totalValue", portfolioService.getTotalPortfolioValue(id));
        return "client-detail";
    }

    @GetMapping("/delete/{id}")
    public String deleteClient(@PathVariable("id") String id, HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        clientService.deleteClient(id);
        auditLogService.logAction(userId, "CLIENT_DELETE", request.getRemoteAddr(), "Hapus client ID: " + id);
        return "redirect:/clients";
    }
}
