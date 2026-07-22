package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ReportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/report-template-upload")
public class ReportTemplateController {

    private final ReportService reportService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public ReportTemplateController(ReportService reportService, JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.reportService = reportService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String showUploadForm(Model model) {
        String sql = "SELECT id, nama_template, uploaded_at FROM report_templates ORDER BY uploaded_at DESC";
        model.addAttribute("templates", jdbcTemplate.queryForList(sql));
        return "report-template-upload";
    }

    @PostMapping
    public String handleTemplateUpload(@RequestParam(value = "namaTemplate", required = false) String namaTemplate,
                                       @RequestParam(value = "xmlContent", required = false) String xmlContent,
                                       @RequestParam(value = "xmlFile", required = false) MultipartFile xmlFile,
                                       HttpServletRequest request,
                                       HttpSession session,
                                       Model model) {

        String contentToParse = xmlContent;

        if (xmlFile != null && !xmlFile.isEmpty()) {
            try {
                contentToParse = new String(xmlFile.getBytes(), StandardCharsets.UTF_8);
                if (namaTemplate == null || namaTemplate.trim().isEmpty()) {
                    namaTemplate = xmlFile.getOriginalFilename();
                }
            } catch (Exception e) {
                model.addAttribute("error", "Gagal membaca file XML: " + e.getMessage());
                return showUploadForm(model);
            }
        }

        if (contentToParse == null || contentToParse.trim().isEmpty()) {
            model.addAttribute("error", "Konten XML template tidak boleh kosong.");
            return showUploadForm(model);
        }

        if (namaTemplate == null || namaTemplate.trim().isEmpty()) {
            namaTemplate = "Custom Template " + System.currentTimeMillis();
        }

        try {
            // Save to DB
            Integer userId = (Integer) session.getAttribute("userId");
            String insertSql = "INSERT INTO report_templates (nama_template, xml_content, uploaded_by) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, namaTemplate, contentToParse, userId);

            // Call parseReportTemplate as is for preview
            Object parsedResult = reportService.parseReportTemplate(contentToParse);

            auditLogService.logAction(userId, "TEMPLATE_UPLOAD", request.getRemoteAddr(),
                    "Upload template laporan XML: " + namaTemplate);

            model.addAttribute("success", "Template XML berhasil diunggah dan diparse.");
            model.addAttribute("previewResult", parsedResult != null ? parsedResult.toString() : "Success");
            return showUploadForm(model);

        } catch (Exception e) {
            model.addAttribute("error", "Error parsing template XML: " + e.getMessage());
            return showUploadForm(model);
        }
    }
}
