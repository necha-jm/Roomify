package com.app.roomify;


import java.util.Date;
import java.util.List;

public class Room {
    private String id;
    private String title;
    private String description;
    private double price;
    private double latitude;
    private double longitude;
    private String address;
    private String postedBy;
    private Date postedDate;
    private boolean isAvailable;
    private List<String> amenities;
    private List<String> images;

    // Empty constructor for Firebase
    public Room() {}

    // Constructor
    public Room(String id, String title, String description, double price,
                double latitude, double longitude, String address,
                String postedBy, boolean isAvailable) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.postedBy = postedBy;
        this.postedDate = new Date();
        this.isAvailable = isAvailable;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
}
