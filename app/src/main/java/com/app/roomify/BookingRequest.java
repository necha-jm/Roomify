package com.app.roomify;

import androidx.annotation.Nullable;

public class BookingRequest {
    private String id;
    private String userId;
    private String userName;
    private String userPhone;
    private String roomId;
    private String roomTitle;
    private String status; // pending, approved, rejected, cancelled
    private long timestamp;
    private String bookingDate;

    // Empty constructor for Firestore
    public BookingRequest() {}

    // Constructor with required fields
    public BookingRequest(String userId, String userName, String roomId, String roomTitle) {
        this.userId = userId;
        this.userName = userName;
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId != null ? userId : "";
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName != null ? userName : "Unknown User";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone != null ? userPhone : "Not provided";
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getRoomId() {
        return roomId != null ? roomId : "";
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomTitle() {
        return roomTitle != null ? roomTitle : "Unknown Room";
    }

    public void setRoomTitle(String roomTitle) {
        this.roomTitle = roomTitle;
    }

    public String getStatus() {
        return status != null ? status : "pending";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBookingDate() {
        return bookingDate != null ? bookingDate : "";
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    // Helper method to check if booking is pending
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    // Helper method to check if booking is approved
    public boolean isApproved() {
        return "approved".equalsIgnoreCase(status);
    }

    // Helper method to check if booking is rejected
    public boolean isRejected() {
        return "rejected".equalsIgnoreCase(status);
    }

    // Helper method to check if booking is cancelled
    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
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
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BookingRequest{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", roomTitle='" + roomTitle + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}