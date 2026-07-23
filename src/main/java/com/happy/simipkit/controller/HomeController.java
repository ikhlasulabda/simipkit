package com.happy.simipkit.controller;

import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.ClientService;
import com.happy.simipkit.service.UserService;
import com.happy.simipkit.util.CurrencyUtil;
import com.happy.simipkit.util.JsStringUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final ClientService clientService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;

    public HomeController(ClientService clientService, UserService userService,
                          AuditLogService auditLogService, JdbcTemplate jdbcTemplate) {
        this.clientService = clientService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        // --- Existing 4 stat cards ---
        model.addAttribute("totalClients", clientService.getAllClients().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("recentAuditLogs", auditLogService.getAllLogs());

        // --- 1a. Total AUM ---
        Double totalAumRaw = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(nilai), 0) FROM portfolio_assets", Double.class);
        if (totalAumRaw == null) totalAumRaw = 0.0;
        model.addAttribute("totalAumRaw", totalAumRaw);
        model.addAttribute("totalAum", CurrencyUtil.format(totalAumRaw));

        // --- 1b. AUM by instrument type (donut chart) ---
        List<String> instrLabels = new ArrayList<>();
        List<Double> instrValues = new ArrayList<>();
        List<Double> instrPercents = new ArrayList<>();
        List<Map<String, Object>> instrRows = jdbcTemplate.queryForList(
                "SELECT jenis_instrumen, SUM(nilai) AS total FROM portfolio_assets GROUP BY jenis_instrumen ORDER BY total DESC");
        for (Map<String, Object> row : instrRows) {
            instrLabels.add(JsStringUtil.escape((String) row.get("jenis_instrumen")));
            double val = ((Number) row.get("total")).doubleValue();
            instrValues.add(val);
            // Guard: prevent NaN/Infinity when totalAum == 0
            instrPercents.add(totalAumRaw > 0 ? Math.round((val / totalAumRaw) * 10000.0) / 100.0 : 0.0);
        }
        model.addAttribute("instrLabels", instrLabels);
        model.addAttribute("instrValues", instrValues);
        model.addAttribute("instrPercents", instrPercents);

        // --- 1c. Cumulative AUM growth trend (line/area chart) ---
        List<String> trendDates = new ArrayList<>();
        List<Double> trendValues = new ArrayList<>();
        List<Map<String, Object>> trendRows = jdbcTemplate.queryForList(
                "SELECT DATE(updated_at) AS tanggal, SUM(nilai) AS daily_total FROM portfolio_assets GROUP BY DATE(updated_at) ORDER BY tanggal ASC");
        double cumulativeSum = 0.0;
        for (Map<String, Object> row : trendRows) {
            Object dateObj = row.get("tanggal");
            trendDates.add(JsStringUtil.escape(dateObj != null ? dateObj.toString() : ""));
            cumulativeSum += ((Number) row.get("daily_total")).doubleValue();
            trendValues.add(cumulativeSum);
        }
        model.addAttribute("trendDates", trendDates);
        model.addAttribute("trendValues", trendValues);

        // --- 1d. Top 5 clients by AUM (horizontal bar chart) ---
        List<String> topNames = new ArrayList<>();
        List<Double> topValues = new ArrayList<>();
        List<Map<String, Object>> topRows = jdbcTemplate.queryForList(
                "SELECT c.nama, SUM(pa.nilai) AS total_aum FROM portfolio_assets pa JOIN clients c ON pa.client_id = c.id GROUP BY pa.client_id, c.nama ORDER BY total_aum DESC LIMIT 5");
        for (Map<String, Object> row : topRows) {
            topNames.add(JsStringUtil.escape((String) row.get("nama")));
            topValues.add(((Number) row.get("total_aum")).doubleValue());
        }
        model.addAttribute("topNames", topNames);
        model.addAttribute("topValues", topValues);

        // --- 1e. KYC status distribution (bar chart) ---
        List<String> kycLabels = new ArrayList<>();
        List<Integer> kycCounts = new ArrayList<>();
        List<Map<String, Object>> kycRows = jdbcTemplate.queryForList(
                "SELECT status_kyc, COUNT(*) AS jumlah FROM clients GROUP BY status_kyc ORDER BY jumlah DESC");
        for (Map<String, Object> row : kycRows) {
            kycLabels.add(JsStringUtil.escape((String) row.get("status_kyc")));
            kycCounts.add(((Number) row.get("jumlah")).intValue());
        }
        model.addAttribute("kycLabels", kycLabels);
        model.addAttribute("kycCounts", kycCounts);

        // --- 1f. Document type distribution (donut chart) ---
        List<String> docLabels = new ArrayList<>();
        List<Integer> docCounts = new ArrayList<>();
        List<Map<String, Object>> docRows = jdbcTemplate.queryForList(
                "SELECT jenis_dokumen, COUNT(*) AS jumlah FROM client_documents GROUP BY jenis_dokumen ORDER BY jumlah DESC");
        for (Map<String, Object> row : docRows) {
            docLabels.add(JsStringUtil.escape((String) row.get("jenis_dokumen")));
            docCounts.add(((Number) row.get("jumlah")).intValue());
        }
        model.addAttribute("docLabels", docLabels);
        model.addAttribute("docCounts", docCounts);

        return "dashboard";
    }
}
