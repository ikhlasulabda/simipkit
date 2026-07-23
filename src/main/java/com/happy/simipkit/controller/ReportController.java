package com.happy.simipkit.controller;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.PortfolioAsset;
import com.happy.simipkit.model.PortfolioReportSummary;
import com.happy.simipkit.model.ReportLayoutConfig;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.PortfolioService;
import com.happy.simipkit.service.ReportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final Logger logger = LogManager.getLogger(ReportController.class);

    private final ReportService reportService;
    private final ClientService clientService;
    private final PortfolioService portfolioService;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;

    public ReportController(ReportService reportService, ClientService clientService,
                            PortfolioService portfolioService, AuditLogService auditLogService,
                            JdbcTemplate jdbcTemplate) {
        this.reportService = reportService;
        this.clientService = clientService;
        this.portfolioService = portfolioService;
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
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

        // Fetch templates for popup selection
        String sqlTemplates = "SELECT id, nama_template FROM report_templates ORDER BY uploaded_at DESC";
        model.addAttribute("templates", jdbcTemplate.queryForList(sqlTemplates));

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

        // Fetch templates for popup selection
        String sqlTemplates = "SELECT id, nama_template FROM report_templates ORDER BY uploaded_at DESC";
        model.addAttribute("templates", jdbcTemplate.queryForList(sqlTemplates));

        return "report-generate";
    }

    @GetMapping("/summary/{summaryId}/pdf")
    public void downloadReportPdf(@PathVariable("summaryId") int summaryId,
                                  @RequestParam(value = "templateId", required = false) String templateIdStr,
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

        ReportLayoutConfig layoutConfig = new ReportLayoutConfig(); // Default layout
        boolean templateApplied = false;
        String fallbackReason = null;

        if (templateIdStr != null && !templateIdStr.trim().isEmpty()) {
            try {
                int templateId = Integer.parseInt(templateIdStr.trim());

                // Parameterized query (no string concatenation)
                String sql = "SELECT nama_template, xml_content FROM report_templates WHERE id = ?";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, templateId);

                if (rows.isEmpty()) {
                    fallbackReason = "Template ID " + templateId + " tidak ditemukan";
                } else {
                    Map<String, Object> row = rows.get(0);
                    String namaTemplate = (String) row.get("nama_template");
                    String xmlContent = (String) row.get("xml_content");

                    try {
                        layoutConfig = reportService.parseReportLayoutTemplate(xmlContent);
                        templateApplied = true;
                    } catch (Exception parseEx) {
                        logger.warn("Gagal parse template ID {}: {}", templateId, parseEx.getMessage());
                        fallbackReason = "Template \"" + namaTemplate + "\" error";
                    }
                }
            } catch (NumberFormatException nfe) {
                fallbackReason = "Format templateId tidak valid";
            } catch (Exception ex) {
                logger.warn("Gagal memproses template ID {}: {}", templateIdStr, ex.getMessage());
                fallbackReason = "Gagal memproses template";
            }
        }

        // Response Headers for template status
        response.setHeader("X-Template-Applied", String.valueOf(templateApplied));
        if (!templateApplied && fallbackReason != null) {
            // CRLF Header Sanitization (Strip \r and \n to prevent Header Injection)
            String sanitizedReason = fallbackReason.replaceAll("[\\r\\n]", " ").trim();
            response.setHeader("X-Template-Fallback-Reason", sanitizedReason);
        }

        byte[] pdfBytes = reportService.generatePortfolioPdfBytes(client, summary, assets, layoutConfig);

        String filename = "laporan-" + summaryId + "-" + summary.getClientId() + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();

        Integer userId = (Integer) session.getAttribute("userId");
        auditLogService.logAction(userId, "REPORT_PDF_DOWNLOAD", request.getRemoteAddr(),
                "Download PDF laporan portofolio client: " + summary.getClientId() + " summary id: " + summaryId +
                " (Template applied: " + templateApplied + ")");
    }

    @PostMapping("/summary/delete/{id}")
    public String deleteReportSummary(@PathVariable("id") int summaryId,
                                       HttpServletRequest request,
                                       HttpSession session) {
        portfolioService.deleteReportSummary(summaryId);
        Integer userId = (Integer) session.getAttribute("userId");
        auditLogService.logAction(userId, "REPORT_SUMMARY_DELETE", request.getRemoteAddr(),
                "Hapus summary laporan portofolio ID: " + summaryId);
        return "redirect:/reports";
    }

    @PostMapping("/summary/delete-all")
    public String deleteAllReportSummaries(HttpServletRequest request,
                                           HttpSession session) {
        portfolioService.deleteAllReportSummaries();
        Integer userId = (Integer) session.getAttribute("userId");
        auditLogService.logAction(userId, "REPORT_SUMMARY_DELETE_ALL", request.getRemoteAddr(),
                "Hapus SELURUH histori summary laporan portofolio");
        return "redirect:/reports";
    }
}
