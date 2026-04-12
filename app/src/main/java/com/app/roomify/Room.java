package com.app.roomify;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Room {

    // ==================== BASIC IDENTIFICATION ====================
    @SerializedName("id")
    private long id;

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

    // ==================== ROOM SPECIFICATIONS ====================
    private int roomsCount;
    private int bathroomsCount;
    private double area;

    // ==================== AMENITIES & RULES ====================
    private List<String> amenities;
    private List<String> rules;

    // ==================== MEDIA FILES ====================
    private List<String> images;
    private int imageCount;
    private boolean hasVideo;
    private boolean hasContract;
    private String videoUrl;
    private String contractUrl;

    // ==================== STATUS & METRICS ====================
    private boolean isAvailable;
    private String status;
    private int bookingsCount;

    // FIXED: Use String to receive the datetime from backend
    // Gson with java-time adapter will convert LocalDateTime to String
    @SerializedName("createdAt")
    private String createdAt;

    // ==================== CONSTRUCTORS ====================

    public Room() {
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.isAvailable = true;
        this.status = "active";
        this.bookingsCount = 0;
        this.imageCount = 0;
        this.hasVideo = false;
        this.hasContract = false;
        this.roomsCount = 1;
        this.bathroomsCount = 1;
        this.area = 0;
    }

    // ==================== GETTERS & SETTERS ====================

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

    public int getBookingsCount() { return bookingsCount; }
    public void setBookingsCount(int bookingsCount) { this.bookingsCount = bookingsCount; }

    // ==================== HELPER METHODS ====================

    @Exclude
    public String getFormattedPrice() { return "$" + String.format("%,.0f", price); }

    @Exclude
    public String getFirstImageUrl() {
        return (images != null && !images.isEmpty()) ? images.get(0) : null;
    }

    @Exclude
    public boolean hasImages() { return (images != null && !images.isEmpty()) || imageCount > 0; }

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
            return "1 booking";
        } else {
            return bookingsCount + " bookings";
        }
    }

    @Override
    public String toString() {
        return "Room{id=" + id + ", title='" + title + "'}";
    }
}