package com.happy.simipkit.controller;

import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.ClientDocument;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.util.FileNamingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/documents")
public class ClientDocumentController {

    private static final String UPLOAD_BASE_DIR = "/opt/simipkit/uploads/documents/";

    private final ClientService clientService;
    private final AuditLogService auditLogService;

    public ClientDocumentController(ClientService clientService, AuditLogService auditLogService) {
        this.clientService = clientService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/upload/{clientId}")
    public String showUploadForm(@PathVariable("clientId") String clientId, Model model) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return "redirect:/clients";
        }
        model.addAttribute("client", client);
        return "document-upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("clientId") String clientId,
                                   @RequestParam("jenisDokumen") String jenisDokumen,
                                   @RequestParam("file") MultipartFile file,
                                   HttpServletRequest request,
                                   HttpSession session,
                                   Model model) {

        if (file.isEmpty()) {
            model.addAttribute("error", "File dokumen tidak boleh kosong.");
            model.addAttribute("client", clientService.getClientById(clientId));
            return "document-upload";
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String storedFilename = FileNamingUtil.generateUniqueFileName(originalFilename);
            String targetDirPath = UPLOAD_BASE_DIR + clientId + "/";
            File targetDir = new File(targetDirPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            File destFile = new File(targetDir, storedFilename);
            file.transferTo(destFile);

            ClientDocument doc = new ClientDocument();
            doc.setClientId(clientId);
            doc.setJenisDokumen(jenisDokumen);
            doc.setNamaFileAsli(originalFilename);
            doc.setNamaFileStored(storedFilename);
            doc.setFileSizeBytes(file.getSize());

            clientService.addDocument(doc);

            Integer userId = (Integer) session.getAttribute("userId");
            auditLogService.logAction(userId, "DOCUMENT_UPLOAD", request.getRemoteAddr(),
                    "Upload dokumen " + jenisDokumen + " (" + originalFilename + ") untuk client ID: " + clientId);

            return "redirect:/clients/detail/" + clientId;

        } catch (IOException e) {
            model.addAttribute("error", "Gagal mengunggah file: " + e.getMessage());
            model.addAttribute("client", clientService.getClientById(clientId));
            return "document-upload";
        }
    }
}
