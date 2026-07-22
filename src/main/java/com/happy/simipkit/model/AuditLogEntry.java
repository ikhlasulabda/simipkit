package com.happy.simipkit.model;

import java.time.LocalDateTime;

public class AuditLogEntry {

    private Integer id;
    private Integer userId;
    private String action;
    private String ipAddress;
    private String detail;
    private LocalDateTime timestamp;

    public AuditLogEntry() {
    }

    public AuditLogEntry(Integer id, Integer userId, String action, String ipAddress, String detail,
            LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.ipAddress = ipAddress;
        this.detail = detail;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}