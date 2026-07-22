package com.happy.simipkit.model.banksync;

import java.time.LocalDateTime;

public class TransferConfirmationEvent extends BankTransactionEvent {

    private String nomorRekeningPengirim;
    private String nomorRekeningTujuan;
    private double jumlahTransfer;
    private String statusTransfer; // "SUCCESS", "PENDING", "FAILED"

    public TransferConfirmationEvent() {
    }

    public TransferConfirmationEvent(String bankPartnerCode, String referenceNumber, LocalDateTime receivedAt,
            String nomorRekeningPengirim, String nomorRekeningTujuan,
            double jumlahTransfer, String statusTransfer) {
        super(bankPartnerCode, referenceNumber, receivedAt);
        this.nomorRekeningPengirim = nomorRekeningPengirim;
        this.nomorRekeningTujuan = nomorRekeningTujuan;
        this.jumlahTransfer = jumlahTransfer;
        this.statusTransfer = statusTransfer;
    }

    public String getNomorRekeningPengirim() {
        return nomorRekeningPengirim;
    }

    public void setNomorRekeningPengirim(String nomorRekeningPengirim) {
        this.nomorRekeningPengirim = nomorRekeningPengirim;
    }

    public String getNomorRekeningTujuan() {
        return nomorRekeningTujuan;
    }

    public void setNomorRekeningTujuan(String nomorRekeningTujuan) {
        this.nomorRekeningTujuan = nomorRekeningTujuan;
    }

    public double getJumlahTransfer() {
        return jumlahTransfer;
    }

    public void setJumlahTransfer(double jumlahTransfer) {
        this.jumlahTransfer = jumlahTransfer;
    }

    public String getStatusTransfer() {
        return statusTransfer;
    }

    public void setStatusTransfer(String statusTransfer) {
        this.statusTransfer = statusTransfer;
    }
}