package com.happy.simipkit.model;

import java.time.LocalDateTime;

public class Client {

    private String id;
    private String nama;
    private String nik;
    private String alamat;
    private String statusKyc; // "PENDING", "VERIFIED", "REJECTED"
    private String nomorRekening;
    private double saldoRdn;
    private LocalDateTime createdAt;

    public Client() {
    }

    public Client(String id, String nama, String nik, String alamat, String statusKyc, LocalDateTime createdAt) {
        this.id = id;
        this.nama = nama;
        this.nik = nik;
        this.alamat = alamat;
        this.statusKyc = statusKyc;
        this.createdAt = createdAt;
    }

    public Client(String id, String nama, String nik, String alamat, String statusKyc, String nomorRekening, double saldoRdn, LocalDateTime createdAt) {
        this.id = id;
        this.nama = nama;
        this.nik = nik;
        this.alamat = alamat;
        this.statusKyc = statusKyc;
        this.nomorRekening = nomorRekening;
        this.saldoRdn = saldoRdn;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public String getStatusKyc() {
        return statusKyc;
    }

    public void setStatusKyc(String statusKyc) {
        this.statusKyc = statusKyc;
    }

    public String getNomorRekening() {
        return nomorRekening;
    }

    public void setNomorRekening(String nomorRekening) {
        this.nomorRekening = nomorRekening;
    }

    public double getSaldoRdn() {
        return saldoRdn;
    }

    public void setSaldoRdn(double saldoRdn) {
        this.saldoRdn = saldoRdn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}