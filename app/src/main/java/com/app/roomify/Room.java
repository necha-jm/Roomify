package com.app.roomify;

import androidx.room.ColumnInfo;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@IgnoreExtraProperties
public class Room {

    // ==================== BASIC IDENTIFICATION ====================
    @SerializedName("id")
    @Expose(serialize = false)  // Don't send to server
    private long id;

    @ColumnInfo(name = "server_id")
    private long serverId;

    @ColumnInfo(name = "synced")
    private boolean synced;
    private String title;
    private String description;
    private String propertyType;

    // ==================== PRICING & FINANCIALS ====================
    private double price;

    // ==================== LOCATION INFORMATION ====================
    private double latitude;
    private double longitude;
    private String address;

    // ==================== OWNER INFORMATION ====================
    @SerializedName("postedBy")
    private long postedBy;

    private String ownerName;
    private String contactPhone;
    private String contactEmail;

    // ==================== DALALI (AGENT) INFORMATION ====================
    @SerializedName("dalaliId")
    private long dalaliId;

    @SerializedName("dalaliName")
    private String dalaliName;

    @SerializedName("commission")
    private double commission;

    @SerializedName("commissionRate")
    private double commissionRate; // Percentage (e.g., 10% = 10.0)

    // ==================== ROOM SPECIFICATIONS ====================
    private int roomsCount;
    private int bathroomsCount;
    private double area;

    // ==================== AMENITIES & RULES ====================
    private List<String> amenities;

    @Exclude
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }
    private List<String> rules;

    // ==================== MEDIA FILES ====================
    private List<String> images;

    @Expose(serialize = false)  // Don't send to server
    private int imageCount;
    private boolean hasVideo;
    private boolean hasContract;
    private String videoUrl;
    private String contractUrl;

    // ==================== STATUS & METRICS ====================
    private boolean isAvailable;
    private String status;  // "AVAILABLE", "PENDING", "RENTED"
    private int bookingsCount;
    private int viewCount;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // ==================== FEATURED & PROMOTION ====================
    private boolean featured;
    private boolean promoted;

    // ==================== CONSTRUCTORS ====================

    public Room() {
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.isAvailable = true;
        this.status = "AVAILABLE";
        this.bookingsCount = 0;
        this.viewCount = 0;
        this.imageCount = 0;
        this.hasVideo = false;
        this.hasContract = false;
        this.roomsCount = 1;
        this.bathroomsCount = 1;
        this.area = 0;
        this.commission = 0;
        this.commissionRate = 0;
        this.featured = false;
        this.promoted = false;
        this.dalaliId = 0;
    }

    // ==================== EXISTING GETTERS & SETTERS ====================

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getPostedBy() { return postedBy; }
    public void setPostedBy(long postedBy) { this.postedBy = postedBy; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public int getRoomsCount() { return roomsCount; }
    public void setRoomsCount(int roomsCount) { this.roomsCount = roomsCount; }

    public int getBathroomsCount() { return bathroomsCount; }
    public void setBathroomsCount(int bathroomsCount) { this.bathroomsCount = bathroomsCount; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public List<String> getAmenities() { return amenities != null ? amenities : new ArrayList<>(); }
    public void setAmenities(List<String> amenities) { this.amenities = amenities != null ? amenities : new ArrayList<>(); }

    public List<String> getRules() { return rules != null ? rules : new ArrayList<>(); }
    public void setRules(List<String> rules) { this.rules = rules != null ? rules : new ArrayList<>(); }

    public List<String> getImages() { return images != null ? images : new ArrayList<>(); }
    public void setImages(List<String> images) {
        this.images = images != null ? images : new ArrayList<>();
        this.imageCount = this.images.size();
    }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }

    public boolean isHasContract() { return hasContract; }
    public void setHasContract(boolean hasContract) { this.hasContract = hasContract; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        this.hasVideo = videoUrl != null && !videoUrl.isEmpty();
    }

    public String getContractUrl() { return contractUrl; }
    public void setContractUrl(String contractUrl) {
        this.contractUrl = contractUrl;
        this.hasContract = contractUrl != null && !contractUrl.isEmpty();
    }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getBookingsCount() { return bookingsCount; }
    public void setBookingsCount(int bookingsCount) { this.bookingsCount = bookingsCount; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    // ==================== DALALI GETTERS & SETTERS ====================
    public long getDalaliId() { return dalaliId; }
    public void setDalaliId(long dalaliId) { this.dalaliId = dalaliId; }

    public String getDalaliName() { return dalaliName; }
    public void setDalaliName(String dalaliName) { this.dalaliName = dalaliName; }

    public double getCommission() { return commission; }
    public void setCommission(double commission) { this.commission = commission; }

    public double getCommissionRate() { return commissionRate; }
    public void setCommissionRate(double commissionRate) { this.commissionRate = commissionRate; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isPromoted() { return promoted; }
    public void setPromoted(boolean promoted) { this.promoted = promoted; }

    // ==================== HELPER METHODS ====================

    @Exclude
    public String getFormattedPrice() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));
        return formatter.format(price);
    }

    @Exclude
    public String getFormattedPriceMonthly() {
        return getFormattedPrice() + "/month";
    }

    @Exclude
    public String getFirstImageUrl() {
        return (images != null && !images.isEmpty()) ? images.get(0) : null;
    }

    @Exclude
    public boolean hasImages() {
        return (images != null && !images.isEmpty()) || imageCount > 0;
    }

    @Exclude
    public String getRulesText() {
        if (rules != null && !rules.isEmpty()) {
            return android.text.TextUtils.join(", ", rules);
        }
        return "";
    }

    @Exclude
    public String getLocationSummary() {
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split(",");
            return parts[0].trim();
        }
        return "Location not specified";
    }

    @Exclude
    public String getBookingsText() {
        if (bookingsCount == 0) {
            return "No bookings yet";
        } else if (bookingsCount == 1) {
            return "1 interested tenant";
        } else {
            return bookingsCount + " interested tenants";
        }
    }

    @Exclude
    public String getStatusBadge() {
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            return "✓ Available";
        } else if ("PENDING".equalsIgnoreCase(status)) {
            return "⏳ Pending";
        } else if ("RENTED".equalsIgnoreCase(status)) {
            return "✓ Rented";
        }
        return status;
    }

    @Exclude
    public int getStatusColor() {
        if ("AVAILABLE".equalsIgnoreCase(status)) {
            return android.R.color.holo_green_dark;
        } else if ("PENDING".equalsIgnoreCase(status)) {
            return android.R.color.holo_orange_dark;
        } else if ("RENTED".equalsIgnoreCase(status)) {
            return android.R.color.holo_blue_dark;
        }
        return android.R.color.darker_gray;
    }

    @Exclude
    public boolean isStatusAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    @Exclude
    public boolean isStatusPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    @Exclude
    public boolean isStatusRented() {
        return "RENTED".equalsIgnoreCase(status);
    }

    @Exclude
    public String getCommissionFormatted() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("sw", "TZ"));
        return formatter.format(commission);
    }

    @Exclude
    public String getCommissionRateFormatted() {
        return String.format("%.0f%%", commissionRate);
    }

    @Exclude
    public double calculateCommissionFromRate() {
        if (commissionRate > 0) {
            return price * (commissionRate / 100);
        }
        return commission;
    }

    @Exclude
    public String getPropertySummary() {
        StringBuilder sb = new StringBuilder();
        if (roomsCount > 0) sb.append(roomsCount).append(" bed");
        if (bathroomsCount > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(bathroomsCount).append(" bath");
        }
        if (area > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(String.format("%.0f", area)).append(" m²");
        }
        return sb.toString();
    }

    @Exclude
    public String getFormattedViewCount() {
        if (viewCount == 0) return "No views";
        if (viewCount == 1) return "1 view";
        return viewCount + " views";
    }

    @Override
    public String toString() {
        return "Room{id=" + id + ", title='" + title + "', status='" + status + "'}";
    }
}