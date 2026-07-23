package com.happy.simipkit.model;

import java.time.LocalDateTime;

public class PortfolioReportSummary {

    private Integer id;
    private String clientId;
    private String periode;
    private double totalNilai;
    private LocalDateTime generatedAt;
    private String assetsSnapshot;

    public PortfolioReportSummary() {
    }

    public PortfolioReportSummary(Integer id, String clientId, String periode, double totalNilai,
            LocalDateTime generatedAt) {
        this.id = id;
        this.clientId = clientId;
        this.periode = periode;
        this.totalNilai = totalNilai;
        this.generatedAt = generatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public double getTotalNilai() {
        return totalNilai;
    }

    public void setTotalNilai(double totalNilai) {
        this.totalNilai = totalNilai;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getAssetsSnapshot() {
        return assetsSnapshot;
    }

    public void setAssetsSnapshot(String assetsSnapshot) {
        this.assetsSnapshot = assetsSnapshot;
    }
}