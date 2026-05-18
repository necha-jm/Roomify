package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class EarningTransaction {
    @SerializedName("id")
    private long id;

    @SerializedName("propertyId")
    private long propertyId;

    @SerializedName("propertyTitle")
    private String propertyTitle;

    @SerializedName("amount")
    private double amount;

    @SerializedName("commission")
    private double commission;

    @SerializedName("status")
    private String status; // PENDING, PAID, WITHDRAWN

    @SerializedName("date")
    private String date;

    @SerializedName("tenantName")
    private String tenantName;

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPropertyId() { return propertyId; }
    public void setPropertyId(long propertyId) { this.propertyId = propertyId; }

    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getCommission() { return commission; }
    public void setCommission(double commission) { this.commission = commission; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}