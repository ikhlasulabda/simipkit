package com.happy.simipkit.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReportLayoutConfig {

    public static final Set<String> VALID_COLUMNS = new HashSet<>(Arrays.asList(
            "jenisInstrumen", "namaInstrumen", "jumlah", "nilai", "allocationPercent"
    ));

    private String title = "LAPORAN PORTOFOLIO KLIEN";
    private String subtitle = "SIMIPKIT - Sistem Informasi Manajemen Investasi dan Portofolio Klien";
    private boolean showClientInfo = true;
    private boolean showGeneratedDate = true;
    private List<String> columns = new ArrayList<>(Arrays.asList(
            "jenisInstrumen", "namaInstrumen", "jumlah", "nilai", "allocationPercent"
    ));
    private String footerNote = "Dokumen ini digenerate secara otomatis oleh sistem SIMIPKIT.";

    public ReportLayoutConfig() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            this.subtitle = subtitle;
        }
    }

    public boolean isShowClientInfo() {
        return showClientInfo;
    }

    public void setShowClientInfo(boolean showClientInfo) {
        this.showClientInfo = showClientInfo;
    }

    public boolean isShowGeneratedDate() {
        return showGeneratedDate;
    }

    public void setShowGeneratedDate(boolean showGeneratedDate) {
        this.showGeneratedDate = showGeneratedDate;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        if (columns != null && !columns.isEmpty()) {
            this.columns = columns;
        }
    }

    public String getFooterNote() {
        return footerNote;
    }

    public void setFooterNote(String footerNote) {
        if (footerNote != null && !footerNote.trim().isEmpty()) {
            this.footerNote = footerNote;
        }
    }
}
