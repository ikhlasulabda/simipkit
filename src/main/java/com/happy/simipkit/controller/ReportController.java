package com.happy.simipkit.controller;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.model.PortfolioReportSummary;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.PortfolioService;
import com.happy.simipkit.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final ClientService clientService;
    private final PortfolioService portfolioService;
    private final AuditLogService auditLogService;

    public ReportController(ReportService reportService, ClientService clientService, PortfolioService portfolioService, AuditLogService auditLogService) {
        this.reportService = reportService;
        this.clientService = clientService;
        this.portfolioService = portfolioService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String listReports(Model model) {
        List<PortfolioReportSummary> summaries = portfolioService.getAllReportSummaries();
        model.addAttribute("summaries", summaries);
        model.addAttribute("clients", clientService.getAllClients());
        return "report-list";
    }

    @GetMapping("/generate/{clientId}")
    public String showGenerateForm(@PathVariable("clientId") String clientId, Model model) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return "redirect:/clients";
        }
        model.addAttribute("client", client);
        model.addAttribute("assets", portfolioService.getAssetsByClientId(clientId));
        model.addAttribute("totalValue", portfolioService.getTotalPortfolioValue(clientId));
        return "report-generate";
    }

    @PostMapping("/generate")
    public String generateReport(@RequestParam("clientId") String clientId,
                                 @RequestParam("periode") String periode,
                                 HttpServletRequest request,
                                 HttpSession session,
                                 Model model) {

        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return "redirect:/reports";
        }

        Map<String, Object> summary = reportService.generatePortfolioSummary(clientId, periode);
        Object totalObj = summary.get("total");
        double totalValue = (totalObj instanceof Number) ? ((Number) totalObj).doubleValue() : portfolioService.getTotalPortfolioValue(clientId);

        // Ambil aset sebelum menyimpan summary agar bisa di-snapshot bersama
        List<PortfolioAsset> assets = portfolioService.getAssetsByClientId(clientId);

        PortfolioReportSummary summaryEntity = new PortfolioReportSummary();
        summaryEntity.setClientId(clientId);
        summaryEntity.setPeriode(periode);
        summaryEntity.setTotalNilai(totalValue);

        int summaryId = portfolioService.saveReportSummary(summaryEntity, assets);

        Integer userId = (Integer) session.getAttribute("userId");
        auditLogService.logAction(userId, "REPORT_GENERATE", request.getRemoteAddr(),
                "Generate laporan portofolio client: " + clientId + " periode: " + periode);

        model.addAttribute("client", client);
        model.addAttribute("periode", periode);
        model.addAttribute("summaryMap", summary);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("assets", assets);
        model.addAttribute("summaryId", summaryId);
        model.addAttribute("success", "Laporan portofolio berhasil dibuat dan disimpan.");

        return "report-generate";
    }

    @GetMapping("/summary/{summaryId}/pdf")
    public void downloadReportPdf(@PathVariable("summaryId") int summaryId,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  HttpSession session) throws IOException {

        PortfolioReportSummary summary = portfolioService.getSummaryById(summaryId);
        if (summary == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Laporan tidak ditemukan.");
            return;
        }

        Client client = clientService.getClientById(summary.getClientId());
        if (client == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Klien tidak ditemukan.");
            return;
        }

        List<PortfolioAsset> assets = portfolioService.deserializeAssetsSnapshot(summary.getAssetsSnapshot());

        byte[] pdfBytes = reportService.generatePortfolioPdfBytes(client, summary, assets);

        // Filename dibangun dari data DB (summaryId + clientId), bukan dari input user
        String filename = "laporan-" + summaryId + "-" + summary.getClientId() + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();

        Integer userId = (Integer) session.getAttribute("userId");
        auditLogService.logAction(userId, "REPORT_PDF_DOWNLOAD", request.getRemoteAddr(),
                "Download PDF laporan portofolio client: " + summary.getClientId() + " summary id: " + summaryId);
    }
}
