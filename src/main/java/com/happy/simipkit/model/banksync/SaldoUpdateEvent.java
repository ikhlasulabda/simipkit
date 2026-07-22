package com.happy.simipkit.model.banksync;

import java.time.LocalDateTime;

public class SaldoUpdateEvent extends BankTransactionEvent {

    private String nomorRekening;
    private double saldoBaru;
    private double saldoSebelumnya;

    public SaldoUpdateEvent() {
    }

    public SaldoUpdateEvent(String bankPartnerCode, String referenceNumber, LocalDateTime receivedAt,
            String nomorRekening, double saldoBaru, double saldoSebelumnya) {
        super(bankPartnerCode, referenceNumber, receivedAt);
        this.nomorRekening = nomorRekening;
        this.saldoBaru = saldoBaru;
        this.saldoSebelumnya = saldoSebelumnya;
    }

    public String getNomorRekening() {
        return nomorRekening;
    }

    public void setNomorRekening(String nomorRekening) {
        this.nomorRekening = nomorRekening;
    }

    public double getSaldoBaru() {
        return saldoBaru;
    }

    public void setSaldoBaru(double saldoBaru) {
        this.saldoBaru = saldoBaru;
    }

    public double getSaldoSebelumnya() {
        return saldoSebelumnya;
    }

    public void setSaldoSebelumnya(double saldoSebelumnya) {
        this.saldoSebelumnya = saldoSebelumnya;
    }
}