package com.app.roomify.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.List;

@Entity(tableName = "rooms")
@TypeConverters(Converters.class)
public class RoomEntity {

    @PrimaryKey(autoGenerate = true)  // IMPORTANT: Auto-generate local ID
    private long id;

    private long serverId;  // Store server-generated ID separately
    private String title;
    private String description;
    private String propertyType;
    private double price;
    private double latitude;
    private double longitude;
    private String address;
    private long postedBy;
    private String ownerName;
    private String contactPhone;
    private String contactEmail;
    private int roomsCount;
    private int bathroomsCount;
    private double area;

    // These will use the same converter
    private List<String> amenities;
    private List<String> rules;
    private List<String> images;

    private int imageCount;
    private boolean hasVideo;
    private boolean hasContract;
    private String videoUrl;
    private String contractUrl;
    private boolean isAvailable;
    private String status;
    private int bookingsCount;
    private String createdAt;
    private boolean isSynced;
    private long lastUpdated;

    // Constructors
    public RoomEntity() {}

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getServerId() { return serverId; }
    public void setServerId(long serverId) { this.serverId = serverId; }

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

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public List<String> getRules() { return rules; }
    public void setRules(List<String> rules) { this.rules = rules; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }

    public boolean isHasContract() { return hasContract; }
    public void setHasContract(boolean hasContract) { this.hasContract = hasContract; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getContractUrl() { return contractUrl; }
    public void setContractUrl(String contractUrl) { this.contractUrl = contractUrl; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getBookingsCount() { return bookingsCount; }
    public void setBookingsCount(int bookingsCount) { this.bookingsCount = bookingsCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}