package com.happy.simipkit.controller;

import com.happy.simipkit.config.AppConfig;
import com.happy.simipkit.model.Client;
import com.happy.simipkit.model.ClientDocument;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.util.FileNamingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/documents")
public class ClientDocumentController {

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
            String uploadBaseDir = AppConfig.getUploadDir();
            String targetDirPath = uploadBaseDir + clientId + "/";
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

    @PostMapping("/delete/{documentId}")
    public String deleteDocument(@PathVariable("documentId") String documentId,
                                 HttpServletRequest request,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        ClientDocument doc = clientService.getDocumentById(documentId);
        if (doc == null) {
            return "redirect:/clients";
        }

        String clientId = doc.getClientId();
        Integer userId = (Integer) session.getAttribute("userId");

        String uploadBaseDir = AppConfig.getUploadDir();
        String targetFilePath = uploadBaseDir + clientId + "/" + doc.getNamaFileStored();
        File physicalFile = new File(targetFilePath);

        if (!physicalFile.exists()) {
            // Edge Case 3a: File fisik sudah tidak ada di disk (misal dihapus manual lewat CLI)
            clientService.deleteDocument(documentId);
            auditLogService.logAction(userId, "CLIENT_DOCUMENT_DELETE", request.getRemoteAddr(),
                    "Hapus dokumen " + doc.getJenisDokumen() + " (" + doc.getNamaFileAsli() + ") untuk client ID: " + clientId +
                    " - File fisik tidak ditemukan di disk, hanya record database yang dihapus");
            redirectAttributes.addFlashAttribute("success", "Record database dokumen berhasil dihapus (file fisik sudah tidak ditemukan di disk).");
        } else {
            // File fisik ada di disk -> coba hapus file fisik dulu (Urutan 3c)
            boolean deleted = physicalFile.delete();
            if (deleted) {
                // Berhasil hapus file fisik -> hapus record DB
                clientService.deleteDocument(documentId);
                auditLogService.logAction(userId, "CLIENT_DOCUMENT_DELETE", request.getRemoteAddr(),
                        "Hapus dokumen " + doc.getJenisDokumen() + " (" + doc.getNamaFileAsli() + ") untuk client ID: " + clientId);
                redirectAttributes.addFlashAttribute("success", "Dokumen " + doc.getNamaFileAsli() + " berhasil dihapus dari storage dan database.");
            } else {
                // Edge Case 3b: Kegagalan hapus file fisik (permission / disk error) -> JANGAN hapus record DB!
                auditLogService.logAction(userId, "CLIENT_DOCUMENT_DELETE_FAILED", request.getRemoteAddr(),
                        "Gagal menghapus file fisik dokumen " + doc.getNamaFileAsli() + " untuk client ID: " + clientId + " (I/O Error)");
                redirectAttributes.addFlashAttribute("error", "Gagal menghapus file fisik dokumen dari disk server (permission/disk error). Record database tidak dihapus.");
            }
        }

        return "redirect:/clients/detail/" + clientId;
    }
}
