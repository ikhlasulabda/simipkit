package com.happy.simipkit.model;

import java.time.LocalDateTime;

public class ReportTemplate {

    private Integer id;
    private String namaTemplate;
    private String xmlContent;
    private Integer uploadedBy;
    private LocalDateTime uploadedAt;

    public ReportTemplate() {
    }

    public ReportTemplate(Integer id, String namaTemplate, String xmlContent, Integer uploadedBy,
            LocalDateTime uploadedAt) {
        this.id = id;
        this.namaTemplate = namaTemplate;
        this.xmlContent = xmlContent;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNamaTemplate() {
        return namaTemplate;
    }

    public void setNamaTemplate(String namaTemplate) {
        this.namaTemplate = namaTemplate;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}