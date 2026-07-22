package com.happy.simipkit.model.banksync;

import java.time.LocalDateTime;

public class SettlementEvent extends BankTransactionEvent {

    private String kodeInstrumen;
    private double jumlahUnit;
    private double hargaSettlement;
    private String tanggalSettlement;

    public SettlementEvent() {
    }

    public SettlementEvent(String bankPartnerCode, String referenceNumber, LocalDateTime receivedAt,
            String kodeInstrumen, double jumlahUnit, double hargaSettlement, String tanggalSettlement) {
        super(bankPartnerCode, referenceNumber, receivedAt);
        this.kodeInstrumen = kodeInstrumen;
        this.jumlahUnit = jumlahUnit;
        this.hargaSettlement = hargaSettlement;
        this.tanggalSettlement = tanggalSettlement;
    }

    public String getKodeInstrumen() {
        return kodeInstrumen;
    }

    public void setKodeInstrumen(String kodeInstrumen) {
        this.kodeInstrumen = kodeInstrumen;
    }

    public double getJumlahUnit() {
        return jumlahUnit;
    }

    public void setJumlahUnit(double jumlahUnit) {
        this.jumlahUnit = jumlahUnit;
    }

    public double getHargaSettlement() {
        return hargaSettlement;
    }

    public void setHargaSettlement(double hargaSettlement) {
        this.hargaSettlement = hargaSettlement;
    }

    public String getTanggalSettlement() {
        return tanggalSettlement;
    }

    public void setTanggalSettlement(String tanggalSettlement) {
        this.tanggalSettlement = tanggalSettlement;
    }
}