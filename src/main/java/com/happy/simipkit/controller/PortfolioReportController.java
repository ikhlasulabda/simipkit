package com.happy.simipkit.controller;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.PortfolioService;
import com.happy.simipkit.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/portfolio/report")
public class PortfolioReportController {

    private final PortfolioService portfolioService;
    private final ClientService clientService;
    private final ReportService reportService;

    public PortfolioReportController(PortfolioService portfolioService, ClientService clientService, ReportService reportService) {
        this.portfolioService = portfolioService;
        this.clientService = clientService;
        this.reportService = reportService;
    }

    @GetMapping("/{clientId}")
    public String viewPortfolioSummary(@PathVariable("clientId") String clientId,
                                       @RequestParam(value = "periode", defaultValue = "2026-Q3") String periode,
                                       Model model) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return "redirect:/clients";
        }

        Map<String, Object> summaryMap = reportService.generatePortfolioSummary(clientId, periode);
        double totalValue = portfolioService.getTotalPortfolioValue(clientId);

        model.addAttribute("client", client);
        model.addAttribute("periode", periode);
        model.addAttribute("summaryMap", summaryMap);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("assets", portfolioService.getAssetsByClientId(clientId));
        model.addAttribute("summaries", portfolioService.getSummariesByClientId(clientId));

        return "report-generate";
    }
}
