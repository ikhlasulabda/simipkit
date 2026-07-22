package com.happy.simipkit.model;

import java.time.LocalDateTime;

public class ClientDocument {

    private String id;
    private String clientId;
    private String jenisDokumen; // "KTP", "NPWP", "PASPOR", dll
    private String namaFileAsli;
    private String namaFileStored;
    private long fileSizeBytes;
    private LocalDateTime uploadedAt;

    public ClientDocument() {
    }

    public ClientDocument(String id, String clientId, String jenisDokumen, String namaFileAsli,
            String namaFileStored, long fileSizeBytes, LocalDateTime uploadedAt) {
        this.id = id;
        this.clientId = clientId;
        this.jenisDokumen = jenisDokumen;
        this.namaFileAsli = namaFileAsli;
        this.namaFileStored = namaFileStored;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJenisDokumen() {
        return jenisDokumen;
    }

    public void setJenisDokumen(String jenisDokumen) {
        this.jenisDokumen = jenisDokumen;
    }

    public String getNamaFileAsli() {
        return namaFileAsli;
    }

    public void setNamaFileAsli(String namaFileAsli) {
        this.namaFileAsli = namaFileAsli;
    }

    public String getNamaFileStored() {
        return namaFileStored;
    }

    public void setNamaFileStored(String namaFileStored) {
        this.namaFileStored = namaFileStored;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}