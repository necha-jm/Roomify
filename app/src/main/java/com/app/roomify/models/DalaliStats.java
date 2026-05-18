package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class DalaliStats {
    @SerializedName("totalListings")
    private int totalListings;

    @SerializedName("activeListings")
    private int activeListings;

    @SerializedName("pendingListings")
    private int pendingListings;

    @SerializedName("rentedListings")
    private int rentedListings;

    @SerializedName("totalCommission")
    private double totalCommission;

    @SerializedName("monthlyCommission")
    private double monthlyCommission;

    @SerializedName("totalViews")
    private int totalViews;

    @SerializedName("totalInterested")
    private int totalInterested;

    @SerializedName("averageRating")
    private float averageRating;

    @SerializedName("verificationStatus")
    private String verificationStatus;

    // Getters and setters
    public int getTotalListings() { return totalListings; }
    public void setTotalListings(int totalListings) { this.totalListings = totalListings; }

    public int getActiveListings() { return activeListings; }
    public void setActiveListings(int activeListings) { this.activeListings = activeListings; }

    public int getPendingListings() { return pendingListings; }
    public void setPendingListings(int pendingListings) { this.pendingListings = pendingListings; }

    public int getRentedListings() { return rentedListings; }
    public void setRentedListings(int rentedListings) { this.rentedListings = rentedListings; }

    public double getTotalCommission() { return totalCommission; }
    public void setTotalCommission(double totalCommission) { this.totalCommission = totalCommission; }

    public double getMonthlyCommission() { return monthlyCommission; }
    public void setMonthlyCommission(double monthlyCommission) { this.monthlyCommission = monthlyCommission; }

    public int getTotalViews() { return totalViews; }
    public void setTotalViews(int totalViews) { this.totalViews = totalViews; }

    public int getTotalInterested() { return totalInterested; }
    public void setTotalInterested(int totalInterested) { this.totalInterested = totalInterested; }

    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
}