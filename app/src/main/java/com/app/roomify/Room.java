package com.app.roomify;

import androidx.annotation.Nullable;

import java.util.ArrayList;
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
    private String contactPhone;        // Owner contact phone
    private String contactEmail;        // Owner contact email
    private int bookingsCount;          // NEW: Number of bookings for this room

    // Empty constructor for Firebase
    public Room() {
        // Initialize lists to avoid null pointer exceptions
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.isAvailable = true;
        this.bookingsCount = 0;  // Initialize to 0
    }

    // Constructor with required fields
    public Room(String title, String address, double price, String postedBy) {
        this();
        this.title = title;
        this.address = address;
        this.price = price;
        this.postedBy = postedBy;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters with Null Safety
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostedBy() {
        return postedBy != null ? postedBy : "";
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public List<String> getAmenities() {
        return amenities != null ? amenities : new ArrayList<>();
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? amenities : new ArrayList<>();
    }

    public List<String> getImages() {
        return images != null ? images : new ArrayList<>();
    }

    public void setImages(List<String> images) {
        this.images = images != null ? images : new ArrayList<>();
    }

    public String getVideoUrl() {
        return videoUrl != null ? videoUrl : "";
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getContractUrl() {
        return contractUrl != null ? contractUrl : "";
    }

    public void setContractUrl(String contractUrl) {
        this.contractUrl = contractUrl;
    }

    public int getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(int roomsCount) {
        this.roomsCount = roomsCount;
    }

    public int getBathroomsCount() {
        return bathroomsCount;
    }

    public void setBathroomsCount(int bathroomsCount) {
        this.bathroomsCount = bathroomsCount;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getPropertyType() {
        return propertyType != null ? propertyType : "";
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public List<String> getRules() {
        return rules != null ? rules : new ArrayList<>();
    }

    public void setRules(List<String> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getContactPhone() {
        return contactPhone != null ? contactPhone : "";
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail != null ? contactEmail : "";
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    // NEW: Bookings Count getter and setter
    public int getBookingsCount() {
        return bookingsCount;
    }

    public void setBookingsCount(int bookingsCount) {
        this.bookingsCount = bookingsCount;
    }

    // NEW: Helper method to increment bookings count
    public void incrementBookingsCount() {
        this.bookingsCount++;
    }

    // NEW: Helper method to decrement bookings count
    public void decrementBookingsCount() {
        if (this.bookingsCount > 0) {
            this.bookingsCount--;
        }
    }

    // NEW: Helper method to check if room has any bookings
    public boolean hasBookings() {
        return bookingsCount > 0;
    }

    // NEW: Helper method to get formatted bookings text
    public String getBookingsText() {
        if (bookingsCount == 0) {
            return "No bookings yet";
        } else if (bookingsCount == 1) {
            return "1 booking";
        } else {
            return bookingsCount + " bookings";
        }
    }

    // Helper methods for backward compatibility
    public List<String> getImageUrls() {
        return getImages();
    }

    public void setImageUrls(List<String> imageUrls) {
        setImages(imageUrls);
    }

    public int getBedrooms() {
        return roomsCount;
    }

    public void setBedrooms(int bedrooms) {
        this.roomsCount = bedrooms;
    }

    public int getBathrooms() {
        return bathroomsCount;
    }

    public void setBathrooms(int bathrooms) {
        this.bathroomsCount = bathrooms;
    }

    public Date getPostedDate() {
        return createdAt > 0 ? new Date(createdAt) : new Date();
    }

    public void setPostedDate(Date postedDate) {
        this.createdAt = postedDate != null ? postedDate.getTime() : System.currentTimeMillis();
    }

    // Helper method to get formatted price
    public String getFormattedPrice() {
        return "TZS " + String.format("%,.0f", price);
    }

    // Helper method to get location summary
    public String getLocationSummary() {
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split(",");
            return parts[0].trim();
        }
        return "Location not specified";
    }

    // Helper method to check if room has images
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    // Helper method to get first image URL
    public String getFirstImageUrl() {
        if (hasImages()) {
            return images.get(0);
        }
        return null;
    }

    // Helper method to check if room has video
    public boolean hasVideo() {
        return videoUrl != null && !videoUrl.isEmpty();
    }

    // Helper method to check if room has contract
    public boolean hasContract() {
        return contractUrl != null && !contractUrl.isEmpty();
    }

    // Helper method to get amenities count
    public int getAmenitiesCount() {
        return amenities != null ? amenities.size() : 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Room) {
            Room other = (Room) obj;
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
        return "Room{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", address='" + address + '\'' +
                ", available=" + isAvailable +
                ", bookingsCount=" + bookingsCount +
                '}';
    }
}