package com.happy.simipkit.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bank-sync-log")
public class BankSyncLogController {

    private final JdbcTemplate jdbcTemplate;

    public BankSyncLogController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public String showSyncLogs(Model model) {
        String sql = "SELECT id, event_type, payload_raw, status, processed_at FROM bank_sync_events ORDER BY processed_at DESC LIMIT 100";
        List<Map<String, Object>> events = jdbcTemplate.queryForList(sql);
        model.addAttribute("events", events);
        return "bank-sync-log";
    }
}
