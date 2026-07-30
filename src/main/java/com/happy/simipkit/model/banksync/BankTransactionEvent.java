package com.happy.simipkit.model.banksync;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

/**
 * Base class untuk semua event yang diterima dari sistem bank partner.
 *
 * Setiap bank partner mengirim struktur payload yang berbeda-beda
 * (saldo update, konfirmasi transfer, settlement, dll), sehingga tim
 * dev menggunakan polymorphic typing Jackson agar satu endpoint bisa
 * menerima berbagai bentuk event tanpa perlu endpoint terpisah per bank.
 *
 * Beberapa bank partner (terutama partner lama yang integrasinya custom)
 * mengirim field tambahan di luar skema standar. Field gatewayExtensionData
 * menampung data mentah tersebut agar tidak perlu skema terpisah per partner.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class BankTransactionEvent {

    private String bankPartnerCode;
    private String referenceNumber;
    private LocalDateTime receivedAt;

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    private Object gatewayExtensionData;

    public BankTransactionEvent() {
    }

    public BankTransactionEvent(String bankPartnerCode, String referenceNumber, LocalDateTime receivedAt) {
        this.bankPartnerCode = bankPartnerCode;
        this.referenceNumber = referenceNumber;
        this.receivedAt = receivedAt;
    }

    public String getBankPartnerCode() {
        return bankPartnerCode;
    }

    public void setBankPartnerCode(String bankPartnerCode) {
        this.bankPartnerCode = bankPartnerCode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Object getGatewayExtensionData() {
        return gatewayExtensionData;
    }

    public void setGatewayExtensionData(Object gatewayExtensionData) {
        this.gatewayExtensionData = gatewayExtensionData;
    }
}