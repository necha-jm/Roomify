package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RoomDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;

    // ==================== UI COMPONENTS ====================
    private TextView tvTitle, tvPrice, tvAddress, tvDescription, tvBedrooms, tvBathrooms, tvArea, tvPostedDate;
    private TextView tvRoomStatus, tvOwnerName, tvOwnerRating, tvMemberSince;
    private ImageView ivOwnerProfile;
    private MaterialButton btnGetDirections, btnCallOwner, btnMessageOwner;
    private Button btnBookNow;
    private ImageButton btnFavorite;
    private ViewPager2 viewPagerImages;
    private LinearLayout imageIndicator;
    private RecyclerView rvAmenities;
    private View amenitiesCard;
    private MapView mapPreview;

    // ==================== DATA HOLDERS ====================
    private double roomLat = 0, roomLng = 0;
    private String roomId;
    private Room currentRoom;
    private boolean alreadyRequested = false;
    private boolean isRoomLoaded = false;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> amenitiesList = new ArrayList<>();

    // Firebase
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Initialize
        initializeFirebase();
        initializeOSMDroid();

        // Get roomId
        roomId = getIntent().getStringExtra("room_id");
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Room ID missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();
        setupClickListeners();

        // Load data
        btnBookNow.setEnabled(false);
        btnBookNow.setText("Loading...");

        loadRoomDetails();
        checkIfAlreadyRequested();
        checkIfFavorite();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initializeOSMDroid() {
        try {
            Configuration.getInstance().load(
                    getApplicationContext(),
                    getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
            );
        } catch (Exception e) {
            Log.w(TAG, "OSMDroid init: " + e.getMessage());
        }
    }

    private void initializeViews() {
        // Basic Info
        tvTitle = findViewById(R.id.tvRoomTitle);
        tvPrice = findViewById(R.id.tvRoomPrice);
        tvAddress = findViewById(R.id.tvRoomAddress);
        tvDescription = findViewById(R.id.tvRoomDescription);
        tvBedrooms = findViewById(R.id.tvBedrooms);
        tvBathrooms = findViewById(R.id.tvBathrooms);
        tvArea = findViewById(R.id.tvArea);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvRoomStatus = findViewById(R.id.tvRoomStatus);

        // Owner Info
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvOwnerRating = findViewById(R.id.tvOwnerRating);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        ivOwnerProfile = findViewById(R.id.ivOwnerProfile);

        // Buttons
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnCallOwner = findViewById(R.id.btnCallOwner);
        btnMessageOwner = findViewById(R.id.btnMessageOwner);
        btnBookNow = findViewById(R.id.btnBookNow);
        btnFavorite = findViewById(R.id.btnFavorite);

        // Media
        viewPagerImages = findViewById(R.id.viewPagerImages);
        imageIndicator = findViewById(R.id.imageIndicator);
        rvAmenities = findViewById(R.id.rvAmenities);
        amenitiesCard = findViewById(R.id.tvAmenity);

        // Map
        mapPreview = findViewById(R.id.mapPreview);

        // Setup RecyclerView
        if (rvAmenities != null) {
            rvAmenities.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupClickListeners() {
        if (btnGetDirections != null) {
            btnGetDirections.setOnClickListener(v -> openDirections());
        }
        if (btnCallOwner != null) {
            btnCallOwner.setOnClickListener(v -> contactRoomOwner());
        }
        if (btnMessageOwner != null) {
            btnMessageOwner.setOnClickListener(v -> messageRoomOwner());
        }
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                if (alreadyRequested) {
                    Toast.makeText(this, "You already requested this room", Toast.LENGTH_SHORT).show();
                } else {
                    requestRoomBooking();
                }
            });
        }
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        }
    }

    private void loadRoomDetails() {
        FirebaseUtils.getRoom(roomId, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, "Failed to load room", Toast.LENGTH_SHORT).show();
                btnBookNow.setEnabled(false);
                btnBookNow.setText("Error Loading");
                return;
            }

            DocumentSnapshot doc = task.getResult();
            Room room = doc.toObject(Room.class);
            if (room == null) {
                Toast.makeText(this, "Room data not found", Toast.LENGTH_SHORT).show();
                btnBookNow.setEnabled(false);
                btnBookNow.setText("Data Error");
                return;
            }

            currentRoom = room;
            isRoomLoaded = true;
            roomLat = room.getLatitude();
            roomLng = room.getLongitude();

            displayBasicInfo(room);
            displayOwnerInfo(room);
            displayAmenities(room);
            loadRoomMedia(room);
            setupMapPreview(room);
            updateRoomStatus(room);
            updateBookButtonState(room);
        });
    }

    private void displayBasicInfo(Room room) {
        if (tvTitle != null) tvTitle.setText(getSafeString(room.getTitle()));
        if (tvPrice != null) tvPrice.setText("$" + room.getPrice() + "/month");
        if (tvAddress != null) tvAddress.setText(getSafeString(room.getAddress()));
        if (tvDescription != null) tvDescription.setText(getSafeString(room.getDescription()));
        if (tvBedrooms != null) {
            tvBedrooms.setText(room.getRoomsCount() + " " +
                    (room.getRoomsCount() == 1 ? "Bedroom" : "Bedrooms"));
        }
        if (tvBathrooms != null) {
            tvBathrooms.setText(room.getBathroomsCount() + " " +
                    (room.getBathroomsCount() == 1 ? "Bathroom" : "Bathrooms"));
        }
        if (tvArea != null) {
            if (room.getArea() > 0) {
                tvArea.setText(room.getArea() + " m²");
                tvArea.setVisibility(View.VISIBLE);
            } else {
                tvArea.setVisibility(View.GONE);
            }
        }
        if (tvPostedDate != null) {
            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(room.getCreatedAt()));
            tvPostedDate.setText("Posted: " + date);
        }
    }

    private void displayOwnerInfo(Room room) {
        if (tvOwnerName == null) return;

        String ownerName = room.getOwnerName();
        if (ownerName != null && !ownerName.isEmpty()) {
            tvOwnerName.setText(ownerName);
        } else {
            String ownerId = room.getPostedBy();
            if (ownerId != null && !ownerId.isEmpty()) {
                tvOwnerName.setText("Loading...");
                loadOwnerDetailsFromUserCollection(ownerId);
            } else {
                tvOwnerName.setText("Owner information not available");
            }
        }

        if (tvOwnerRating != null) tvOwnerRating.setText("★ 4.8 (24 reviews)");
        if (tvMemberSince != null) tvMemberSince.setText("Member since 2023");
        if (ivOwnerProfile != null) {
            ivOwnerProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void loadOwnerDetailsFromUserCollection(String ownerId) {
        if (db == null) return;

        db.collection("users").document(ownerId).get()
                .addOnSuccessListener(userDoc -> {
                    if (tvOwnerName != null) {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("name");
                            if (name == null || name.isEmpty()) {
                                name = userDoc.getString("fullName");
                            }
                            tvOwnerName.setText(name != null && !name.isEmpty() ? name : "Owner");
                        } else {
                            tvOwnerName.setText("Owner");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvOwnerName != null) tvOwnerName.setText("Owner");
                });
    }

    private void displayAmenities(Room room) {
        List<String> amenities = room.getAmenities();

        if (amenities != null && !amenities.isEmpty() && rvAmenities != null) {
            amenitiesList = amenities;
            AmenitiesAdapter adapter = new AmenitiesAdapter(amenitiesList);
            rvAmenities.setAdapter(adapter);
            rvAmenities.setVisibility(View.VISIBLE);
        } else {
            if (amenitiesCard != null) {
                amenitiesCard.setVisibility(View.GONE);
            }
        }
    }

    private void loadRoomMedia(Room room) {
        if (room.getImageCount() > 0) {
            loadImagesFromStorage();
        } else {
            if (viewPagerImages != null) viewPagerImages.setVisibility(View.GONE);
            if (imageIndicator != null) imageIndicator.setVisibility(View.GONE);
        }
    }

    private void loadImagesFromStorage() {
        if (storage == null || roomId == null) return;

        StorageReference roomImagesRef = storage.getReference()
                .child("rooms/" + roomId + "/images");

        roomImagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    if (listResult.getItems().isEmpty()) {
                        hideImageGallery();
                        return;
                    }

                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUrls.add(uri.toString());
                            setupImagePager();
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get image URL: " + e.getMessage());
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load images", e);
                    hideImageGallery();
                });
    }

    private void hideImageGallery() {
        if (viewPagerImages != null) viewPagerImages.setVisibility(View.GONE);
        if (imageIndicator != null) imageIndicator.setVisibility(View.GONE);
    }

    private void setupImagePager() {
        if (imageUrls.isEmpty() || viewPagerImages == null) {
            hideImageGallery();
            return;
        }

        ImagePagerAdapter adapter = new ImagePagerAdapter(imageUrls);
        viewPagerImages.setAdapter(adapter);
        setupImageIndicator();
    }

    private void setupImageIndicator() {
        if (imageIndicator == null || imageUrls.isEmpty()) return;

        imageIndicator.removeAllViews();

        for (int i = 0; i < imageUrls.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(8), dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            imageIndicator.addView(dot);
        }

        if (imageIndicator.getChildCount() > 0) {
            imageIndicator.getChildAt(0).setBackgroundResource(R.drawable.dot_active);
        }

        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < imageIndicator.getChildCount(); i++) {
                    View dot = imageIndicator.getChildAt(i);
                    dot.setBackgroundResource(
                            i == position ? R.drawable.dot_active : R.drawable.dot_inactive
                    );
                }
            }
        });
    }

    private void setupMapPreview(Room room) {
        if (mapPreview == null) return;

        try {
            mapPreview.setMultiTouchControls(true);
            mapPreview.getController().setZoom(15.0);

            GeoPoint roomPoint;
            if (roomLat != 0 && roomLng != 0) {
                roomPoint = new GeoPoint(roomLat, roomLng);
            } else {
                roomPoint = new GeoPoint(-6.7924, 39.2083);
            }

            mapPreview.getController().setCenter(roomPoint);

            Marker roomMarker = new Marker(mapPreview);
            roomMarker.setPosition(roomPoint);
            roomMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            roomMarker.setTitle(getSafeString(room.getTitle()));
            roomMarker.setSnippet("$" + room.getPrice() + "/month");
            mapPreview.getOverlays().add(roomMarker);
            mapPreview.invalidate();

        } catch (Exception e) {
            Log.e(TAG, "Map error: " + e.getMessage());
            if (mapPreview != null) mapPreview.setVisibility(View.GONE);
        }
    }

    private void updateRoomStatus(Room room) {
        if (tvRoomStatus == null) return;

        if (room.isAvailable()) {
            tvRoomStatus.setText("Available");
            tvRoomStatus.setBackgroundResource(R.drawable.status_available_bg);
        } else {
            tvRoomStatus.setText("Booked");
            tvRoomStatus.setBackgroundResource(R.drawable.status_booked_bg);
        }
        tvRoomStatus.setTextColor(0xFFFFFFFF);
    }

    private void updateBookButtonState(Room room) {
        if (btnBookNow == null) return;

        String ownerId = room.getPostedBy();
        String currentUserId = FirebaseUtils.getCurrentUserId();

        if (ownerId != null && ownerId.equals(currentUserId)) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Your Room");
            return;
        }

        if (ownerId == null || ownerId.isEmpty()) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Owner Info Missing");
            return;
        }

        if (!alreadyRequested) {
            btnBookNow.setEnabled(true);
            btnBookNow.setText("Book Now");
        } else {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Already Requested");
        }
    }

    private void checkIfAlreadyRequested() {
        String userId = FirebaseUtils.getCurrentUserId();
        if (userId == null || db == null) return;

        db.collection("users")
                .document(userId)
                .collection("bookings")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        alreadyRequested = true;
                        if (btnBookNow != null) {
                            btnBookNow.setEnabled(false);
                            btnBookNow.setText("Already Requested");
                        }
                    } else if (isRoomLoaded && currentRoom != null) {
                        updateBookButtonState(currentRoom);
                    }
                });
    }

    private void checkIfFavorite() {
        String userId = FirebaseUtils.getCurrentUserId();
        if (userId == null || db == null || btnFavorite == null) return;

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                    } else {
                        btnFavorite.setImageResource(R.drawable.ic_favorite_outline);
                    }
                });
    }

    private void toggleFavorite() {
        String userId = FirebaseUtils.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please login to save favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        if (db == null || btnFavorite == null) return;

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    btnFavorite.setImageResource(R.drawable.ic_favorite_outline);
                                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Map<String, Object> favorite = new HashMap<>();
                        favorite.put("roomId", roomId);
                        favorite.put("timestamp", System.currentTimeMillis());
                        favorite.put("title", currentRoom != null ? currentRoom.getTitle() : "");

                        doc.getReference().set(favorite)
                                .addOnSuccessListener(aVoid -> {
                                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void requestRoomBooking() {
        if (!isRoomLoaded || currentRoom == null) {
            Toast.makeText(this, "Loading room info...", Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerId = currentRoom.getPostedBy();
        if (ownerId == null || ownerId.trim().isEmpty()) {
            Toast.makeText(this, "Cannot request booking: owner info missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseUtils.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to request a room", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ownerId.equals(currentUserId)) {
            Toast.makeText(this, "You cannot request your own room", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBookNow.setEnabled(false);
        btnBookNow.setText("Sending Request...");

        String ownerName = currentRoom.getOwnerName();
        if (ownerName == null || ownerName.isEmpty()) ownerName = "Owner";

        String finalOwnerName = ownerName;
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    String userName = userDoc.getString("name");
                    if (userName == null || userName.isEmpty()) {
                        userName = userDoc.getString("fullName");
                    }
                    if (userName == null || userName.isEmpty()) userName = "User";

                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("userId", currentUserId);
                    bookingData.put("userName", userName);
                    bookingData.put("roomId", currentRoom.getId());
                    bookingData.put("roomTitle", currentRoom.getTitle());
                    bookingData.put("ownerId", ownerId);
                    bookingData.put("ownerName", finalOwnerName);
                    bookingData.put("timestamp", System.currentTimeMillis());
                    bookingData.put("status", "pending");

                    FirebaseUtils.getRoomBookingsCollection(currentRoom.getId())
                            .add(bookingData)
                            .addOnSuccessListener(docRef -> {
                                db.collection("users")
                                        .document(currentUserId)
                                        .collection("bookings")
                                        .document(docRef.getId())
                                        .set(bookingData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Booking request sent", Toast.LENGTH_SHORT).show();
                                            alreadyRequested = true;
                                            btnBookNow.setEnabled(false);
                                            btnBookNow.setText("Already Requested");
                                        });
                            });
                });
    }

    private void openDirections() {
        if (roomLat == 0 || roomLng == 0) {
            Toast.makeText(this, "Room location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double originLat = location != null ? location.getLatitude() : 0;
            double originLng = location != null ? location.getLongitude() : 0;

            String uri = "https://www.google.com/maps/dir/?api=1" +
                    "&origin=" + originLat + "," + originLng +
                    "&destination=" + roomLat + "," + roomLng +
                    "&travelmode=driving";

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        });
    }

    private void contactRoomOwner() {
        if (currentRoom == null) return;
        String phone = currentRoom.getContactPhone();
        if (phone != null && !phone.isEmpty()) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
        } else {
            Toast.makeText(this, "Owner phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void messageRoomOwner() {
        if (currentRoom == null) return;

        String email = currentRoom.getContactEmail();
        if (email != null && !email.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Room: " + currentRoom.getTitle());
            startActivity(Intent.createChooser(intent, "Send Email"));
        } else {
            Toast.makeText(this, "Owner email not available", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSafeString(String str) {
        return str != null ? str : "";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapPreview != null) mapPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapPreview != null) mapPreview.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openDirections();
        }
    }
}