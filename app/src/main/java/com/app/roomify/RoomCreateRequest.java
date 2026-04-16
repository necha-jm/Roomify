package com.app.roomify;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RoomCreateRequest {

    // Basic Information
    private String title;
    private String description;
    private String propertyType;

    // Pricing
    private double price;

    // Location
    private double latitude;
    private double longitude;
    private String address;

    // Owner Information
    @SerializedName("postedBy")
    private long postedBy;
    private String ownerName;
    private String contactPhone;
    private String contactEmail;

    // Room Details
    private int roomsCount;
    private int bathroomsCount;
    private double area;

    // Amenities & Rules
    private List<String> amenities;
    private List<String> rules;

    // Media Flags
    private boolean hasVideo;
    private boolean hasContract;

    // Constructor - Creates request from Room object
    public RoomCreateRequest(Room room) {
        this.title = room.getTitle();
        this.description = room.getDescription();
        this.propertyType = room.getPropertyType();
        this.price = room.getPrice();
        this.latitude = room.getLatitude();
        this.longitude = room.getLongitude();
        this.address = room.getAddress();
        this.postedBy = room.getPostedBy();
        this.ownerName = room.getOwnerName();
        this.contactPhone = room.getContactPhone();
        this.contactEmail = room.getContactEmail();
        this.roomsCount = room.getRoomsCount();
        this.bathroomsCount = room.getBathroomsCount();
        this.area = room.getArea();
        this.amenities = room.getAmenities();
        this.rules = room.getRules();
        this.hasVideo = room.isHasVideo();
        this.hasContract = room.isHasContract();
    }

    // Getters (required for Gson serialization)
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPropertyType() { return propertyType; }
    public double getPrice() { return price; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public long getPostedBy() { return postedBy; }
    public String getOwnerName() { return ownerName; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public int getRoomsCount() { return roomsCount; }
    public int getBathroomsCount() { return bathroomsCount; }
    public double getArea() { return area; }
    public List<String> getAmenities() { return amenities; }
    public List<String> getRules() { return rules; }
    public boolean isHasVideo() { return hasVideo; }
    public boolean isHasContract() { return hasContract; }
}
