package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private long id;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("role")
    private String role;

    @SerializedName("emailVerified")
    private boolean emailVerified;

    // ========== DALALI-SPECIFIC FIELDS ==========
    @SerializedName("businessName")
    private String businessName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("licenseNumber")
    private String licenseNumber;

    @SerializedName("locationArea")
    private String locationArea;

    @SerializedName("verificationStatus")
    private String verificationStatus;

    @SerializedName("rating")
    private float rating;

    @SerializedName("totalTransactions")
    private int totalTransactions;

    @SerializedName("joinedDate")
    private String joinedDate;

    @SerializedName("profileImage")
    private String profileImage;

    // ========== CONSTRUCTORS ==========
    public User() {}

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // ========== EXISTING GETTERS & SETTERS ==========
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    // ========== DALALI GETTERS & SETTERS ==========
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLocationArea() { return locationArea; }
    public void setLocationArea(String locationArea) { this.locationArea = locationArea; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    // ========== HELPER METHODS ==========
    public boolean isTenant() {
        return "tenant".equalsIgnoreCase(role);
    }

    public boolean isOwner() {
        return "owner".equalsIgnoreCase(role);
    }

    public boolean isDalali() {
        return "dalali".equalsIgnoreCase(role);
    }

    public boolean isVerified() {
        return "VERIFIED".equalsIgnoreCase(verificationStatus);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(verificationStatus);
    }

    public String getDisplayName() {
        if (businessName != null && !businessName.isEmpty() && isOwner()) {
            return businessName;
        }
        return name;
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "U";
        String[] parts = name.split(" ");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    public String getFormattedRating() {
        if (rating == 0) return "New Agent";
        return String.format("%.1f ★", rating);
    }

    public String getVerificationBadge() {
        if (isVerified()) return "✓ Verified";
        if (isPending()) return "⏳ Pending";
        return "⚠️ Unverified";
    }

    public int getVerificationColor() {
        if (isVerified()) return android.R.color.holo_green_dark;
        if (isPending()) return android.R.color.holo_orange_dark;
        return android.R.color.holo_red_dark;
    }
}