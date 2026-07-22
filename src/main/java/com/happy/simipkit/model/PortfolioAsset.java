package com.happy.simipkit.model;

import java.time.LocalDateTime;

public class PortfolioAsset {

    private Integer id;
    private String clientId;
    private String jenisInstrumen; // "SAHAM", "REKSADANA", "OBLIGASI", dll
    private String namaInstrumen;
    private double jumlah;
    private double nilai;
    private double allocationPercent;
    private LocalDateTime updatedAt;

    public PortfolioAsset() {
    }

    public PortfolioAsset(Integer id, String clientId, String jenisInstrumen, String namaInstrumen,
            double jumlah, double nilai, double allocationPercent, LocalDateTime updatedAt) {
        this.id = id;
        this.clientId = clientId;
        this.jenisInstrumen = jenisInstrumen;
        this.namaInstrumen = namaInstrumen;
        this.jumlah = jumlah;
        this.nilai = nilai;
        this.allocationPercent = allocationPercent;
        this.updatedAt = updatedAt;
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

    public String getJenisInstrumen() {
        return jenisInstrumen;
    }

    public void setJenisInstrumen(String jenisInstrumen) {
        this.jenisInstrumen = jenisInstrumen;
    }

    public String getNamaInstrumen() {
        return namaInstrumen;
    }

    public void setNamaInstrumen(String namaInstrumen) {
        this.namaInstrumen = namaInstrumen;
    }

    public double getJumlah() {
        return jumlah;
    }

    public void setJumlah(double jumlah) {
        this.jumlah = jumlah;
    }

    public double getNilai() {
        return nilai;
    }

    public void setNilai(double nilai) {
        this.nilai = nilai;
    }

    public double getAllocationPercent() {
        return allocationPercent;
    }

    public void setAllocationPercent(double allocationPercent) {
        this.allocationPercent = allocationPercent;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}