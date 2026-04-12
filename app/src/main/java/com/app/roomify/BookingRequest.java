package com.app.roomify;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Expose;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingRequest {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("roomId")
    private Long roomId;

    @SerializedName("roomTitle")
    private String roomTitle;

    @SerializedName("userName")
    private String userName;

    @SerializedName("userPhone")
    private String userPhone;

    @SerializedName("status")
    private String status;

    @SerializedName("bookingDate")
    private String bookingDate;

    // DON'T SEND these to backend - remove @SerializedName or set serialize = false
    @Expose(serialize = false)
    private long createdAt;

    @Expose(serialize = false)
    private long updatedAt;

    @SerializedName("totalPrice")
    private double totalPrice;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("numberOfGuests")
    private int numberOfGuests;

    @SerializedName("specialRequests")
    private String specialRequests;

    // Default constructor
    public BookingRequest() {}

    // Constructor for creating new booking
    public BookingRequest(Long userId, String userName, Long roomId, String roomTitle,
                          String startDate, String endDate, int numberOfGuests, double totalPrice) {
        this.userId = userId;
        this.userName = userName;
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfGuests = numberOfGuests;
        this.totalPrice = totalPrice;
        this.status = "PENDING";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomTitle() { return roomTitle != null ? roomTitle : "Unknown Room"; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }

    public String getUserName() { return userName != null ? userName : "Unknown User"; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone != null ? userPhone : "Not provided"; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getStatus() { return status != null ? status : "PENDING"; }
    public void setStatus(String status) { this.status = status; }

    public String getBookingDate() {
        if (bookingDate == null && createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            bookingDate = sdf.format(new Date(createdAt));
        }
        return bookingDate != null ? bookingDate : "";
    }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    // Helper methods
    public boolean isPending() { return "PENDING".equalsIgnoreCase(status); }
    public boolean isAccepted() { return "ACCEPTED".equalsIgnoreCase(status); }
    public boolean isRejected() { return "REJECTED".equalsIgnoreCase(status); }
    public boolean isCancelled() { return "CANCELLED".equalsIgnoreCase(status); }

    public int getStatusColor() {
        switch (status.toUpperCase()) {
            case "PENDING": return android.R.color.holo_orange_dark;
            case "ACCEPTED": return android.R.color.holo_green_dark;
            case "REJECTED": return android.R.color.holo_red_dark;
            default: return android.R.color.darker_gray;
        }
    }

    public String getFormattedPrice() { return String.format("$%.2f", totalPrice); }

    public int getDurationDays() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            if (start != null && end != null) {
                long diff = end.getTime() - start.getTime();
                return (int) (diff / (1000 * 60 * 60 * 24));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof BookingRequest) {
            BookingRequest other = (BookingRequest) obj;
            return id != null && id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }

    @Override
    public String toString() {
        return "BookingRequest{" +
                "id=" + id +
                ", userId=" + userId +
                ", roomId=" + roomId +
                ", roomTitle='" + roomTitle + '\'' +
                ", userName='" + userName + '\'' +
                ", status='" + status + '\'' +
                ", totalPrice=" + totalPrice +
                '}';
    }
}