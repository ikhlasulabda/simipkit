package com.happy.simipkit.controller;

import com.happy.simipkit.service.BankSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/api/sync")
public class BankSyncController {

    private final BankSyncService bankSyncService;

    public BankSyncController(BankSyncService bankSyncService) {
        this.bankSyncService = bankSyncService;
    }

    @PostMapping("/bank-feed")
    @ResponseBody
    public ResponseEntity<Map<String, String>> receiveBankFeed(@RequestBody String rawJsonPayload) {
        try {
            // Teruskan raw json payload apa adanya ke service
            bankSyncService.processIncomingFeed(rawJsonPayload);
            return ResponseEntity.ok(Collections.singletonMap("status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
