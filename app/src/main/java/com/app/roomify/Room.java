package com.app.roomify;

import java.util.Date;
import java.util.List;

public class Room {
    private String id;                  // Unique room ID
    private String title;               // Room title or name
    private String description;         // Room description
    private double price;               // Price per month or per night
    private double latitude;            // GPS latitude
    private double longitude;           // GPS longitude
    private String address;             // Full address
    private String postedBy;            // Owner or landlord ID
    private Date postedDate;            // Date posted
    private boolean isAvailable;        // Availability status
    private List<String> amenities;     // List of amenities (wifi, AC, etc.)
    private List<String> images;        // URLs to room images
    private String videoUrl;            // URL to short video of room
    private String contractUrl;         // URL to mkataba/contract PDF
    private int roomsCount;             // Number of rooms
    private int bathroomsCount;         // Number of bathrooms
    private double area;                // Room or apartment size in m²
    private String propertyType;        // Apartment, single room, studio, etc.
    private List<String> rules;         // Rules for the tenant
    private long createdAt;             // Timestamp of creation

    // New: Contact info
    private String contactPhone;
    private String contactEmail;

    // Empty constructor for Firebase or ORM
    public Room() {
    }



    // Full constructor
    public Room(String id, String title, String description, double price,
                double latitude, double longitude, String address,
                String postedBy, boolean isAvailable, List<String> amenities,
                List<String> images, String videoUrl, String contractUrl,
                int roomsCount, int bathroomsCount, double area,
                String propertyType, List<String> rules, long createdAt, String contactEmail) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.postedBy = postedBy;
        this.contactEmail = contactEmail;
        this.postedDate = new Date();
        this.isAvailable = isAvailable;
        this.amenities = amenities;

        this.images = images;
        this.videoUrl = videoUrl;
        this.contractUrl = contractUrl;
        this.roomsCount = roomsCount;
        this.bathroomsCount = bathroomsCount;
        this.area = area;
        this.propertyType = propertyType;
        this.rules = rules;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public Date getPostedDate() { return postedDate; }
    public void setPostedDate(Date postedDate) { this.postedDate = postedDate; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getContractUrl() { return contractUrl; }
    public void setContractUrl(String contractUrl) { this.contractUrl = contractUrl; }

    public int getRoomsCount() { return roomsCount; }
    public void setRoomsCount(int roomsCount) { this.roomsCount = roomsCount; }

    public int getBathroomsCount() { return bathroomsCount; }
    public void setBathroomsCount(int bathroomsCount) { this.bathroomsCount = bathroomsCount; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
