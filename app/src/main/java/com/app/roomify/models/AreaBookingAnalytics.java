// com/app/roomify/models/AreaBookingAnalytics.java
package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;

public class AreaBookingAnalytics {
    @SerializedName("area")
    private String area;

    @SerializedName("bookingCount")
    private long bookingCount;

    @SerializedName("averagePrice")
    private double averagePrice;

    @SerializedName("occupancyRate")
    private double occupancyRate;

    // Getters and setters
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long bookingCount) { this.bookingCount = bookingCount; }

    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }

    public double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }
}
