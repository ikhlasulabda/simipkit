package com.happy.simipkit.controller;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.PortfolioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ClientService clientService;
    private final AuditLogService auditLogService;

    public PortfolioController(PortfolioService portfolioService, ClientService clientService, AuditLogService auditLogService) {
        this.portfolioService = portfolioService;
        this.clientService = clientService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/list/{clientId}")
    public String listPortfolio(@PathVariable("clientId") String clientId, Model model) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return "redirect:/clients";
        }
        List<PortfolioAsset> assets = portfolioService.getAssetsByClientId(clientId);
        double totalValue = portfolioService.getTotalPortfolioValue(clientId);

        model.addAttribute("client", client);
        model.addAttribute("assets", assets);
        model.addAttribute("totalValue", totalValue);
        return "portfolio-list";
    }

    @GetMapping("/new/{clientId}")
    public String showCreateForm(@PathVariable("clientId") String clientId, Model model) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return "redirect:/clients";
        }
        PortfolioAsset asset = new PortfolioAsset();
        asset.setClientId(clientId);

        model.addAttribute("client", client);
        model.addAttribute("asset", asset);
        model.addAttribute("isNew", true);
        return "portfolio-form";
    }

    @PostMapping("/save")
    public String saveAsset(@ModelAttribute("asset") PortfolioAsset asset, HttpServletRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (asset.getId() != null && asset.getId() > 0) {
            portfolioService.updateAsset(asset);
            auditLogService.logAction(userId, "PORTFOLIO_UPDATE", request.getRemoteAddr(),
                    "Update aset portofolio ID: " + asset.getId() + " untuk client: " + asset.getClientId());
        } else {
            portfolioService.addAsset(asset);
            auditLogService.logAction(userId, "PORTFOLIO_CREATE", request.getRemoteAddr(),
                    "Tambah aset portofolio baru " + asset.getNamaInstrumen() + " untuk client: " + asset.getClientId());
        }

        recalculateAllocations(asset.getClientId());

        return "redirect:/portfolio/list/" + asset.getClientId();
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        PortfolioAsset asset = portfolioService.getAssetById(id);
        if (asset == null) {
            return "redirect:/clients";
        }
        Client client = clientService.getClientById(asset.getClientId());
        model.addAttribute("client", client);
        model.addAttribute("asset", asset);
        model.addAttribute("isNew", false);
        return "portfolio-form";
    }

    @PostMapping("/delete/{id}")
    public String deleteAsset(@PathVariable("id") Integer id, HttpServletRequest request, HttpSession session) {
        PortfolioAsset asset = portfolioService.getAssetById(id);
        String clientId = (asset != null) ? asset.getClientId() : "";
        if (asset != null) {
            Integer userId = (Integer) session.getAttribute("userId");
            portfolioService.deleteAsset(id);
            auditLogService.logAction(userId, "PORTFOLIO_DELETE", request.getRemoteAddr(),
                    "Hapus aset portofolio ID: " + id + " client: " + clientId);
            recalculateAllocations(clientId);
        }
        return "redirect:/portfolio/list/" + clientId;
    }

    private void recalculateAllocations(String clientId) {
        List<PortfolioAsset> assets = portfolioService.getAssetsByClientId(clientId);
        double total = portfolioService.getTotalPortfolioValue(clientId);
        if (total > 0) {
            for (PortfolioAsset a : assets) {
                double pct = (a.getNilai() / total) * 100.0;
                a.setAllocationPercent(Math.round(pct * 100.0) / 100.0);
                portfolioService.updateAsset(a);
            }
        }
    }
}
