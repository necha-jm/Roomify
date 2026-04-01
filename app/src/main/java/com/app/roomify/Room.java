package com.app.roomify;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Room Model Class
 * Represents a property/room listing in the Roomify application
 *
 * @author Roomify Team
 * @version 2.0
 */
public class Room {

    // ==================== BASIC IDENTIFICATION ====================
    private String id;                  // Unique room ID (Firestore document ID)
    private String title;               // Room title or name
    private String description;         // Room description
    private String propertyType;        // Apartment, single room, studio, etc.

    // ==================== PRICING & FINANCIALS ====================
    private double price;               // Price per month or per night

    // ==================== LOCATION INFORMATION ====================
    private double latitude;            // GPS latitude
    private double longitude;           // GPS longitude
    private String address;             // Full address

    // ==================== OWNER INFORMATION ====================
    private String postedBy;            // Owner or landlord ID (Firebase UID)
    private String ownerName;           // NEW: Owner's full name (from PostRoomActivity)
    private String contactPhone;        // Owner contact phone number
    private String contactEmail;        // Owner contact email address

    // ==================== ROOM SPECIFICATIONS ====================
    private int roomsCount;             // Number of bedrooms
    private int bathroomsCount;         // Number of bathrooms
    private double area;                // Room or apartment size in m²

    // ==================== AMENITIES & RULES ====================
    private List<String> amenities;     // List of amenities (wifi, AC, parking, etc.)
    private List<String> rules;         // House rules for the tenant

    // ==================== MEDIA FILES ====================
    private List<String> images;        // URLs to room images
    private int imageCount;             // NEW: Number of images uploaded (from PostRoomActivity)
    private boolean hasVideo;           // NEW: Whether room has a video tour
    private boolean hasContract;        // NEW: Whether room has a contract/agreement PDF
    private String videoUrl;            // URL to short video of room
    private String contractUrl;         // URL to mkataba/contract PDF

    // ==================== STATUS & METRICS ====================
    private boolean isAvailable;        // Availability status (true = available)
    private String status;              // NEW: Room status (active, pending, inactive)
    private int bookingsCount;          // Number of bookings for this room
    private long createdAt;             // Timestamp of creation (milliseconds)

    // ==================== ADDITIONAL FIELDS ====================
    private String rulesText;           // NEW: Text version of rules (from PostRoomActivity)

    // ==================== CONSTRUCTORS ====================

    /**
     * Empty constructor for Firebase Firestore
     * Initializes collections to avoid null pointer exceptions
     */
    public Room() {
        // Initialize lists to avoid null pointer exceptions
        this.amenities = new ArrayList<>();
        this.images = new ArrayList<>();
        this.rules = new ArrayList<>();

        // Initialize default values
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

    /**
     * Constructor with required fields
     * @param title Room title
     * @param address Room address
     * @param price Monthly price
     * @param postedBy Owner's user ID
     */
    public Room(String title, String address, double price, String postedBy) {
        this();
        this.title = title;
        this.address = address;
        this.price = price;
        this.postedBy = postedBy;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Full constructor for PostRoomActivity
     * Creates a complete room object with all fields from the posting form
     */
    public Room(String id, String title, String description, double price,
                double latitude, double longitude, String address,
                String postedBy, String ownerName, String contactPhone,
                String contactEmail, String propertyType, int roomsCount,
                int bathroomsCount, double area, List<String> amenities,
                List<String> rules, int imageCount, boolean hasVideo,
                boolean hasContract, long createdAt, boolean isAvailable,
                String status) {
        this();
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.postedBy = postedBy;
        this.ownerName = ownerName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.propertyType = propertyType;
        this.roomsCount = roomsCount;
        this.bathroomsCount = bathroomsCount;
        this.area = area;
        this.amenities = amenities != null ? amenities : new ArrayList<>();
        this.rules = rules != null ? rules : new ArrayList<>();
        this.imageCount = imageCount;
        this.hasVideo = hasVideo;
        this.hasContract = hasContract;
        this.createdAt = createdAt;
        this.isAvailable = isAvailable;
        this.status = status != null ? status : "active";
    }

    // ==================== GETTERS & SETTERS ====================

    // --- Basic Identification ---
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

    public String getPropertyType() {
        return propertyType != null ? propertyType : "";
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    // --- Pricing & Financials ---
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // --- Location Information ---
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

    // --- Owner Information ---
    public String getPostedBy() {
        return postedBy != null ? postedBy : "";
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    /**
     * NEW: Get owner's full name
     * @return Owner's name as entered in PostRoomActivity
     */
    public String getOwnerName() {
        return ownerName != null ? ownerName : "";
    }

    /**
     * NEW: Set owner's full name
     * @param ownerName Owner's full name
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    // --- Room Specifications ---
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

    // --- Amenities & Rules ---
    public List<String> getAmenities() {
        return amenities != null ? amenities : new ArrayList<>();
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? amenities : new ArrayList<>();
    }

    public List<String> getRules() {
        return rules != null ? rules : new ArrayList<>();
    }

    public void setRules(List<String> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }

    /**
     * NEW: Get rules as plain text (for display)
     * @return Concatenated rules or rules text
     */
    public String getRulesText() {
        if (rulesText != null && !rulesText.isEmpty()) {
            return rulesText;
        }
        if (rules != null && !rules.isEmpty()) {
            return android.text.TextUtils.join(", ", rules);
        }
        return "";
    }

    public void setRulesText(String rulesText) {
        this.rulesText = rulesText;
    }

    // --- Media Files ---
    public List<String> getImages() {
        return images != null ? images : new ArrayList<>();
    }

    public void setImages(List<String> images) {
        this.images = images != null ? images : new ArrayList<>();
    }

    /**
     * NEW: Get image count
     * @return Number of images uploaded
     */
    public int getImageCount() {
        return imageCount;
    }

    /**
     * NEW: Set image count
     * @param imageCount Number of images
     */
    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    /**
     * NEW: Check if room has video
     * @return true if video exists
     */
    public boolean isHasVideo() {
        return hasVideo;
    }

    /**
     * NEW: Set video availability
     * @param hasVideo true if video exists
     */
    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    /**
     * NEW: Check if room has contract
     * @return true if contract exists
     */
    public boolean isHasContract() {
        return hasContract;
    }

    /**
     * NEW: Set contract availability
     * @param hasContract true if contract exists
     */
    public void setHasContract(boolean hasContract) {
        this.hasContract = hasContract;
    }

    public String getVideoUrl() {
        return videoUrl != null ? videoUrl : "";
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        this.hasVideo = (videoUrl != null && !videoUrl.isEmpty());
    }

    public String getContractUrl() {
        return contractUrl != null ? contractUrl : "";
    }

    public void setContractUrl(String contractUrl) {
        this.contractUrl = contractUrl;
        this.hasContract = (contractUrl != null && !contractUrl.isEmpty());
    }

    // --- Status & Metrics ---
    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    /**
     * NEW: Get room status (active, pending, inactive)
     * @return Room status string
     */
    public String getStatus() {
        return status != null ? status : "active";
    }

    /**
     * NEW: Set room status
     * @param status Room status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getBookingsCount() {
        return bookingsCount;
    }

    public void setBookingsCount(int bookingsCount) {
        this.bookingsCount = bookingsCount;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper method to increment bookings count
     */
    public void incrementBookingsCount() {
        this.bookingsCount++;
    }

    /**
     * Helper method to decrement bookings count
     */
    public void decrementBookingsCount() {
        if (this.bookingsCount > 0) {
            this.bookingsCount--;
        }
    }

    /**
     * Helper method to check if room has any bookings
     * @return true if room has at least one booking
     */
    public boolean hasBookings() {
        return bookingsCount > 0;
    }

    /**
     * Helper method to get formatted bookings text
     * @return Human-readable booking count string
     */
    public String getBookingsText() {
        if (bookingsCount == 0) {
            return "No bookings yet";
        } else if (bookingsCount == 1) {
            return "1 booking";
        } else {
            return bookingsCount + " bookings";
        }
    }

    /**
     * Helper method to get formatted price with currency
     * @return Formatted price string (e.g., "TZS 500,000")
     */
    public String getFormattedPrice() {
        return "TZS " + String.format("%,.0f", price);
    }

    /**
     * Helper method to get formatted price with USD prefix
     * @return Formatted price string (e.g., "$500")
     */
    public String getFormattedPriceUSD() {
        return "$" + String.format("%,.0f", price);
    }

    /**
     * Helper method to get location summary (first line of address)
     * @return Short location description
     */
    public String getLocationSummary() {
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split(",");
            return parts[0].trim();
        }
        return "Location not specified";
    }

    /**
     * Helper method to check if room has images
     * @return true if images exist
     */
    public boolean hasImages() {
        return (images != null && !images.isEmpty()) || imageCount > 0;
    }

    /**
     * Helper method to get first image URL
     * @return First image URL or null if none exist
     */
    public String getFirstImageUrl() {
        if (hasImages() && images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null;
    }

    /**
     * Helper method to get amenities count
     * @return Number of amenities
     */
    public int getAmenitiesCount() {
        return amenities != null ? amenities.size() : 0;
    }

    /**
     * Helper method to check if specific amenity exists
     * @param amenity Amenity to check
     * @return true if amenity exists
     */
    public boolean hasAmenity(String amenity) {
        return amenities != null && amenities.contains(amenity);
    }

    /**
     * Helper method to get formatted address (short version)
     * @return Short address string
     */
    public String getShortAddress() {
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split(",");
            if (parts.length >= 2) {
                return parts[0].trim() + ", " + parts[1].trim();
            }
            return address;
        }
        return "";
    }

    /**
     * Helper method to get room type display string
     * @return Formatted room type with details
     */
    public String getRoomTypeDisplay() {
        StringBuilder display = new StringBuilder();
        display.append(roomsCount).append(" bed");
        if (roomsCount > 1) display.append("s");
        display.append(" • ");
        display.append(bathroomsCount).append(" bath");
        if (bathroomsCount > 1) display.append("s");
        if (propertyType != null && !propertyType.isEmpty()) {
            display.append(" • ").append(propertyType);
        }
        return display.toString();
    }

    /**
     * Helper method to check if room is recently posted (within last 7 days)
     * @return true if posted within last 7 days
     */
    public boolean isRecentlyPosted() {
        long sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L;
        return (System.currentTimeMillis() - createdAt) <= sevenDaysInMillis;
    }

    /**
     * Helper method to get time ago text
     * @return Human-readable time since posting
     */
    public String getTimeAgo() {
        long diff = System.currentTimeMillis() - createdAt;
        long days = diff / (24 * 60 * 60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long minutes = diff / (60 * 1000);

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    // ==================== BACKWARD COMPATIBILITY METHODS ====================

    /**
     * Alias for getImages() - maintains backward compatibility
     */
    public List<String> getImageUrls() {
        return getImages();
    }

    /**
     * Alias for setImages() - maintains backward compatibility
     */
    public void setImageUrls(List<String> imageUrls) {
        setImages(imageUrls);
        this.imageCount = imageUrls != null ? imageUrls.size() : 0;
    }

    /**
     * Alias for getRoomsCount() - maintains backward compatibility
     */
    public int getBedrooms() {
        return roomsCount;
    }

    /**
     * Alias for setRoomsCount() - maintains backward compatibility
     */
    public void setBedrooms(int bedrooms) {
        this.roomsCount = bedrooms;
    }

    /**
     * Alias for getBathroomsCount() - maintains backward compatibility
     */
    public int getBathrooms() {
        return bathroomsCount;
    }

    /**
     * Alias for setBathroomsCount() - maintains backward compatibility
     */
    public void setBathrooms(int bathrooms) {
        this.bathroomsCount = bathrooms;
    }

    /**
     * Get posted date as Date object
     * @return Date object from createdAt timestamp
     */
    public Date getPostedDate() {
        return createdAt > 0 ? new Date(createdAt) : new Date();
    }

    /**
     * Set posted date from Date object
     * @param postedDate Date object
     */
    public void setPostedDate(Date postedDate) {
        this.createdAt = postedDate != null ? postedDate.getTime() : System.currentTimeMillis();
    }

    /**
     * Check if room has video (alternative method name)
     * @return true if video exists
     */
    public boolean hasVideo() {
        return hasVideo;
    }

    /**
     * Check if room has contract (alternative method name)
     * @return true if contract exists
     */
    public boolean hasContract() {
        return hasContract;
    }

    // ==================== OBJECT METHODS ====================

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
                ", ownerName='" + ownerName + '\'' +
                ", available=" + isAvailable +
                ", bookingsCount=" + bookingsCount +
                '}';
    }

    /**
     * Detailed toString for debugging
     * @return Detailed room information
     */
    public String toDetailedString() {
        return "Room{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", address='" + address + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", roomsCount=" + roomsCount +
                ", bathroomsCount=" + bathroomsCount +
                ", area=" + area +
                ", amenities=" + amenities +
                ", imageCount=" + imageCount +
                ", hasVideo=" + hasVideo +
                ", hasContract=" + hasContract +
                ", postedBy='" + postedBy + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", isAvailable=" + isAvailable +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", bookingsCount=" + bookingsCount +
                '}';
    }
}