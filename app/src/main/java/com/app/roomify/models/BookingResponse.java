package com.app.roomify.models;

public class BookingResponse {

    private Long id;
    private Long userId;
    private Long roomId;
    private String status;
    private double totalPrice;
    private String startDate;
    private String endDate;
    private String createdAt;
    private String roomTitle;
    private String tenantName;
    private String tenantEmail;
    private String ownerName;
    private String ownerEmail;

    // Add these fields for userName and userEmail (alias for tenant)
    private String userName;
    private String userEmail;

    public BookingResponse() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
        this.userName = tenantName; // Also set userName
    }

    public String getTenantEmail() { return tenantEmail; }
    public void setTenantEmail(String tenantEmail) {
        this.tenantEmail = tenantEmail;
        this.userEmail = tenantEmail; // Also set userEmail
    }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    // Add these missing getters
    public String getUserName() {
        return userName != null ? userName : tenantName;
    }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() {
        return userEmail != null ? userEmail : tenantEmail;
    }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}