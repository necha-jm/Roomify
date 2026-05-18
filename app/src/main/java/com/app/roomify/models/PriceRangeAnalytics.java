// com/app/roomify/models/PriceRangeAnalytics.java
package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class PriceRangeAnalytics {
    @SerializedName("priceRange")
    private String priceRange;

    @SerializedName("bookingCount")
    private long bookingCount;

    @SerializedName("averagePrice")
    private double averagePrice;

    // Getters and setters
    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long bookingCount) { this.bookingCount = bookingCount; }

    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }
}
