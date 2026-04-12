package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateRoomRequest {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private double price;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("address")
    private String address;

    @SerializedName("propertyType")
    private String propertyType;

    @SerializedName("contactPhone")
    private String contactPhone;

    @SerializedName("contactEmail")
    private String contactEmail;

    @SerializedName("ownerName")
    private String ownerName;

    @SerializedName("amenities")
    private List<String> amenities;

    @SerializedName("roomsCount")
    private int roomsCount;

    @SerializedName("bathroomsCount")
    private int bathroomsCount;

    @SerializedName("area")
    private double area;

    @SerializedName("rules")
    private List<String> rules;

    @SerializedName("postedBy")
    private Long postedBy;

    @SerializedName("hasVideo")
    private boolean hasVideo;

    @SerializedName("hasContract")
    private boolean hasContract;

    @SerializedName("available")
    private boolean available;

    @SerializedName("status")
    private String status;

    // Getters and Setters
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

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public int getRoomsCount() { return roomsCount; }
    public void setRoomsCount(int roomsCount) { this.roomsCount = roomsCount; }

    public int getBathroomsCount() { return bathroomsCount; }
    public void setBathroomsCount(int bathroomsCount) { this.bathroomsCount = bathroomsCount; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }

    public Long getPostedBy() { return postedBy; }
    public void setPostedBy(Long postedBy) { this.postedBy = postedBy; }

    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }

    public boolean isHasContract() { return hasContract; }
    public void setHasContract(boolean hasContract) { this.hasContract = hasContract; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
