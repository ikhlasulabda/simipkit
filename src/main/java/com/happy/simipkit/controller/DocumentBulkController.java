package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.DocumentBulkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/documents")
public class DocumentBulkController {

    private final DocumentBulkService documentBulkService;
    private final ClientService clientService;
    private final AuditLogService auditLogService;

    public DocumentBulkController(DocumentBulkService documentBulkService,
                                  ClientService clientService,
                                  AuditLogService auditLogService) {
        this.documentBulkService = documentBulkService;
        this.clientService = clientService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/bulk-upload")
    public String showBulkUploadForm(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "document-bulk-upload";
    }

    @PostMapping("/bulk-upload")
    public String handleBulkUpload(@RequestParam("clientId") String clientId,
                                   @RequestParam("zipFile") MultipartFile zipFile,
                                   HttpServletRequest request,
                                   HttpSession session,
                                   Model model) {

        if (zipFile == null || zipFile.isEmpty()) {
            model.addAttribute("error", "File ZIP tidak boleh kosong.");
            model.addAttribute("clients", clientService.getAllClients());
            return "document-bulk-upload";
        }

        try {
            // Panggil extractBulkUpload apa adanya tanpa sanitasi
            documentBulkService.extractBulkUpload(zipFile, clientId);

            Integer userId = (Integer) session.getAttribute("userId");
            auditLogService.logAction(userId, "BULK_DOCUMENT_UPLOAD", request.getRemoteAddr(),
                    "Ekstraksi bulk dokumen ZIP untuk client ID: " + clientId + " (" + zipFile.getOriginalFilename() + ")");

            model.addAttribute("success", "Bulk upload ZIP berhasil diekstraksi untuk client: " + clientId);
            model.addAttribute("clients", clientService.getAllClients());
            return "document-bulk-upload";

        } catch (Exception e) {
            model.addAttribute("error", "Proses ekstraksi gagal: " + e.getMessage());
            model.addAttribute("clients", clientService.getAllClients());
            return "document-bulk-upload";
        }
    }
}
