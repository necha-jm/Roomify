package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    @SerializedName("businessName")
    private String businessName;

    @SerializedName("phone")
    private String phone;

    // NEW FIELDS FOR DALALI (AGENT)
    @SerializedName("licenseNumber")
    private String licenseNumber;

    @SerializedName("locationArea")
    private String locationArea;

    @SerializedName("verificationStatus")
    private String verificationStatus;  // pending, verified, rejected

    // Default constructor (required for Gson)
    public RegisterRequest() {
    }

    // ========== EXISTING GETTERS & SETTERS (UNCHANGED) ==========
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // ========== NEW GETTERS & SETTERS FOR DALALI ==========
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getLocationArea() {
        return locationArea;
    }

    public void setLocationArea(String locationArea) {
        this.locationArea = locationArea;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    // ========== HELPER METHOD TO CHECK IF DALALI ==========
    public boolean isDalali() {
        return "dalali".equalsIgnoreCase(role);
    }

    // ========== HELPER METHOD TO CHECK IF OWNER ==========
    public boolean isOwner() {
        return "owner".equalsIgnoreCase(role);
    }

    // ========== HELPER METHOD TO CHECK IF TENANT ==========
    public boolean isTenant() {
        return "tenant".equalsIgnoreCase(role);
    }
}