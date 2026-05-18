package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class TenantInterest {
    @SerializedName("tenantId")
    private long tenantId;

    @SerializedName("tenantName")
    private String tenantName;

    @SerializedName("tenantEmail")
    private String tenantEmail;

    @SerializedName("tenantPhone")
    private String tenantPhone;

    @SerializedName("interestDate")
    private String interestDate;

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private String status; // PENDING, CONTACTED, VIEWED, BOOKED

    // Getters and setters
    public long getTenantId() { return tenantId; }
    public void setTenantId(long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantEmail() { return tenantEmail; }
    public void setTenantEmail(String tenantEmail) { this.tenantEmail = tenantEmail; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public String getInterestDate() { return interestDate; }
    public void setInterestDate(String interestDate) { this.interestDate = interestDate; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}