package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RoomDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 201;

    // ==================== UI COMPONENTS ====================
    // Basic Info
    private TextView tvTitle, tvPrice, tvAddress, tvDescription, tvBedrooms, tvBathrooms, tvArea, tvPostedDate;
    private TextView tvRoomStatus, tvOwnerName, tvOwnerRating, tvMemberSince;
    private ImageView ivOwnerProfile;

    // Action Buttons
    private MaterialButton btnGetDirections, btnCallOwner, btnMessageOwner;
    private Button btnBookNow;
    private ImageButton btnFavorite;

    // Media Components
    private ViewPager2 viewPagerImages;
    private LinearLayout imageIndicator;
    private RecyclerView rvAmenities;
    private View amenitiesCard;
    private MapView mapPreview;

    // Media Action Layouts and Buttons
    private LinearLayout mediaActionsLayout;
    private LinearLayout videoActionsRow;
    private LinearLayout contractActionsRow;
    private MaterialButton btnViewImages;
    private MaterialButton btnPlayVideo;
    private MaterialButton btnDownloadVideo;
    private MaterialButton btnViewContractDoc;
    private MaterialButton btnDownloadContract;

    // ==================== NEW VIDEO PREVIEW COMPONENTS ====================
    private LinearLayout videoPreviewSection;
    private ImageView ivVideoThumbnail;
    private ImageView btnPlayVideoOverlay;
    private MaterialButton btnPlayVideoFull;
    private MaterialButton btnDownloadVideoFull;

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
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Initialize
        initializeFirebase();
        initializeOSMDroid();
        executorService = Executors.newSingleThreadExecutor();

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
        if (btnBookNow != null) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Loading...");
        }

        loadRoomDetails();
        checkIfAlreadyRequested();
        checkIfFavorite();
    }

    // ==================== INITIALIZATION METHODS ====================

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
            Configuration.getInstance().setUserAgentValue(getPackageName());
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

        // Media Action Layouts
        mediaActionsLayout = findViewById(R.id.mediaActionsLayout);
        videoActionsRow = findViewById(R.id.videoActionsRow);
        contractActionsRow = findViewById(R.id.contractActionsRow);

        // Media Buttons
        btnViewImages = findViewById(R.id.btnViewImages);
        btnPlayVideo = findViewById(R.id.btnPlayVideo);
        btnDownloadVideo = findViewById(R.id.btnDownloadVideo);
        btnViewContractDoc = findViewById(R.id.btnViewContractDoc);
        btnDownloadContract = findViewById(R.id.btnDownloadContract);

        // ==================== NEW VIDEO PREVIEW COMPONENTS ====================
        videoPreviewSection = findViewById(R.id.videoPreviewSection);
        ivVideoThumbnail = findViewById(R.id.ivVideoThumbnail);
        btnPlayVideoOverlay = findViewById(R.id.btnPlayVideoOverlay);
        btnPlayVideoFull = findViewById(R.id.btnPlayVideoFull);
        btnDownloadVideoFull = findViewById(R.id.btnDownloadVideoFull);

        // Media Components
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

        // Verify views are initialized
        verifyViews();
    }

    private void verifyViews() {
        Log.d(TAG, "=== VERIFYING VIEWS ===");
        Log.d(TAG, "mediaActionsLayout: " + (mediaActionsLayout != null ? "FOUND" : "NULL"));
        Log.d(TAG, "videoActionsRow: " + (videoActionsRow != null ? "FOUND" : "NULL"));
        Log.d(TAG, "videoPreviewSection: " + (videoPreviewSection != null ? "FOUND" : "NULL"));
        Log.d(TAG, "ivVideoThumbnail: " + (ivVideoThumbnail != null ? "FOUND" : "NULL"));
        Log.d(TAG, "btnPlayVideoOverlay: " + (btnPlayVideoOverlay != null ? "FOUND" : "NULL"));
        Log.d(TAG, "btnPlayVideoFull: " + (btnPlayVideoFull != null ? "FOUND" : "NULL"));
        Log.d(TAG, "btnDownloadVideoFull: " + (btnDownloadVideoFull != null ? "FOUND" : "NULL"));
        Log.d(TAG, "contractActionsRow: " + (contractActionsRow != null ? "FOUND" : "NULL"));
        Log.d(TAG, "=====================");
    }

    private void setupClickListeners() {
        // Navigation
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

        // Media Actions
        if (btnViewImages != null) {
            btnViewImages.setOnClickListener(v -> viewAllImages());
        }

        // Video Play Listeners (Multiple ways to play video)
        if (btnPlayVideo != null) {
            btnPlayVideo.setOnClickListener(v -> playVideo());
        }
        if (btnPlayVideoOverlay != null) {
            btnPlayVideoOverlay.setOnClickListener(v -> playVideo());
        }
        if (btnPlayVideoFull != null) {
            btnPlayVideoFull.setOnClickListener(v -> playVideo());
        }

        // Video Download Listeners
        if (btnDownloadVideo != null) {
            btnDownloadVideo.setOnClickListener(v -> downloadVideo());
        }
        if (btnDownloadVideoFull != null) {
            btnDownloadVideoFull.setOnClickListener(v -> downloadVideo());
        }

        if (btnViewContractDoc != null) {
            btnViewContractDoc.setOnClickListener(v -> viewContract());
        }
        if (btnDownloadContract != null) {
            btnDownloadContract.setOnClickListener(v -> downloadContract());
        }

        // Image gallery click
        if (viewPagerImages != null) {
            viewPagerImages.setOnClickListener(v -> viewAllImages());
        }
    }

    // ==================== DATA LOADING METHODS ====================

    private void loadRoomDetails() {
        FirebaseUtils.getRoom(roomId, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, "Failed to load room", Toast.LENGTH_SHORT).show();
                if (btnBookNow != null) {
                    btnBookNow.setEnabled(false);
                    btnBookNow.setText("Error Loading");
                }
                return;
            }

            DocumentSnapshot doc = task.getResult();
            Room room = doc.toObject(Room.class);
            if (room == null) {
                Toast.makeText(this, "Room data not found", Toast.LENGTH_SHORT).show();
                if (btnBookNow != null) {
                    btnBookNow.setEnabled(false);
                    btnBookNow.setText("Data Error");
                }
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
            updateMediaButtonsVisibility(room);
        });
    }

    // ==================== DISPLAY METHODS ====================

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
                    Log.e(TAG, "Failed to load owner details", e);
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

    // ==================== MEDIA METHODS ====================

    private void loadRoomMedia(Room room) {
        if (room.getImageCount() > 0) {
            loadImagesFromStorage();
        } else {
            hideImageGallery();
        }

        Log.d(TAG, "Has video: " + room.isHasVideo());
        Log.d(TAG, "Has contract: " + room.isHasContract());
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
        if (btnViewImages != null) btnViewImages.setVisibility(View.GONE);
    }

    private void setupImagePager() {
        if (imageUrls.isEmpty() || viewPagerImages == null) {
            hideImageGallery();
            return;
        }

        ImagePagerAdapter adapter = new ImagePagerAdapter(imageUrls);
        viewPagerImages.setAdapter(adapter);
        setupImageIndicator();

        if (btnViewImages != null) {
            btnViewImages.setVisibility(View.VISIBLE);
        }
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

    private void updateMediaButtonsVisibility(Room room) {
        if (room == null) return;

        try {
            boolean hasAnyMedia = false;

            // Video Section
            boolean hasVideo = room.isHasVideo() &&
                    room.getVideoUrl() != null &&
                    !room.getVideoUrl().isEmpty();

            // Show/Hide Video Preview Section (NEW)
            if (videoPreviewSection != null) {
                videoPreviewSection.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
                if (hasVideo) {
                    hasAnyMedia = true;
                    // Load video thumbnail
                    loadVideoThumbnail(room.getVideoUrl());
                }
            }

            // Original Video Actions Row (keep for compatibility)
            if (videoActionsRow != null) {
                videoActionsRow.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
                if (hasVideo) hasAnyMedia = true;
            }

            // Contract Section
            boolean hasContract = room.isHasContract() &&
                    room.getContractUrl() != null &&
                    !room.getContractUrl().isEmpty();

            if (contractActionsRow != null) {
                contractActionsRow.setVisibility(hasContract ? View.VISIBLE : View.GONE);
                if (hasContract) hasAnyMedia = true;
            }

            // Show/hide individual buttons
            if (btnPlayVideo != null) {
                btnPlayVideo.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
            }
            if (btnDownloadVideo != null) {
                btnDownloadVideo.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
            }
            if (btnViewContractDoc != null) {
                btnViewContractDoc.setVisibility(hasContract ? View.VISIBLE : View.GONE);
            }
            if (btnDownloadContract != null) {
                btnDownloadContract.setVisibility(hasContract ? View.VISIBLE : View.GONE);
            }

            // Hide entire media card if no media
            View mediaCard = findViewById(R.id.mediaActionsCard);
            if (!hasAnyMedia) {
                if (mediaActionsLayout != null) mediaActionsLayout.setVisibility(View.GONE);
                if (mediaCard != null) mediaCard.setVisibility(View.GONE);
            } else {
                if (mediaActionsLayout != null) mediaActionsLayout.setVisibility(View.VISIBLE);
                if (mediaCard != null) mediaCard.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "Media visibility - Video: " + hasVideo + ", Contract: " + hasContract);

        } catch (Exception e) {
            Log.e(TAG, "Error updating media buttons: " + e.getMessage());
        }
    }

    // ==================== NEW VIDEO THUMBNAIL METHODS ====================

    private void loadVideoThumbnail(String videoUrl) {
        if (ivVideoThumbnail == null) return;

        // Show placeholder first
        ivVideoThumbnail.setImageResource(R.drawable.ic_video_placeholder);

        // Try to extract thumbnail from video URL
        executorService.execute(() -> {
            try {
                // For remote video URLs, we can use MediaMetadataRetriever
                // Note: This requires the video to be downloaded partially
                // For now, we'll just use placeholder
                // You can implement thumbnail extraction using Glide or other libraries

                runOnUiThread(() -> {
                    // Keep placeholder or try to load from custom thumbnail
                    // Glide.with(this).load(videoUrl + "?thumbnail").into(ivVideoThumbnail);
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load video thumbnail: " + e.getMessage());
            }
        });
    }

    // ==================== MEDIA ACTION METHODS ====================

    private void viewAllImages() {
        if (imageUrls == null || imageUrls.isEmpty()) {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MediaViewerActivity.class);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, MediaViewerActivity.MEDIA_TYPE_IMAGES);
        intent.putStringArrayListExtra(MediaViewerActivity.EXTRA_MEDIA_URLS, new ArrayList<>(imageUrls));
        intent.putExtra(MediaViewerActivity.EXTRA_CURRENT_POSITION,
                viewPagerImages != null ? viewPagerImages.getCurrentItem() : 0);
        intent.putExtra(MediaViewerActivity.EXTRA_ROOM_TITLE, currentRoom != null ? currentRoom.getTitle() : "Room");
        startActivity(intent);
    }

    private void playVideo() {
        if (currentRoom == null || !currentRoom.isHasVideo() || currentRoom.getVideoUrl() == null) {
            Toast.makeText(this, "No video available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> videoUrls = new ArrayList<>();
        videoUrls.add(currentRoom.getVideoUrl());

        Intent intent = new Intent(this, MediaViewerActivity.class);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, MediaViewerActivity.MEDIA_TYPE_VIDEO);
        intent.putStringArrayListExtra(MediaViewerActivity.EXTRA_MEDIA_URLS, new ArrayList<>(videoUrls));
        intent.putExtra(MediaViewerActivity.EXTRA_ROOM_TITLE, currentRoom.getTitle());
        startActivity(intent);
    }

    private void downloadVideo() {
        if (currentRoom == null || !currentRoom.isHasVideo() || currentRoom.getVideoUrl() == null) {
            Toast.makeText(this, "No video available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }

        String videoUrl = currentRoom.getVideoUrl();
        String fileName = "room_video_" + roomId + "_" + System.currentTimeMillis() + ".mp4";
        downloadFile(videoUrl, fileName, "Video");
    }

    private void viewContract() {
        if (currentRoom == null || !currentRoom.isHasContract() || currentRoom.getContractUrl() == null) {
            Toast.makeText(this, "No contract available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MediaViewerActivity.class);
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, MediaViewerActivity.MEDIA_TYPE_DOCUMENT);
        intent.putExtra("document_url", currentRoom.getContractUrl());
        intent.putExtra("document_name", "Contract_" + roomId + ".pdf");
        intent.putExtra(MediaViewerActivity.EXTRA_ROOM_TITLE, currentRoom.getTitle());
        startActivity(intent);
    }

    private void downloadContract() {
        if (currentRoom == null || !currentRoom.isHasContract() || currentRoom.getContractUrl() == null) {
            Toast.makeText(this, "No contract available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }

        String contractUrl = currentRoom.getContractUrl();
        String fileName = "contract_" + roomId + "_" + System.currentTimeMillis() + ".pdf";
        downloadFile(contractUrl, fileName, "Contract");
    }

    private void downloadFile(String fileUrl, String fileName, String fileType) {
        Toast.makeText(this, "Downloading " + fileType + "...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();

                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs();
                    }

                    File outputFile = new File(downloadDir, fileName);
                    FileOutputStream outputStream = new FileOutputStream(outputFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }

                    outputStream.close();
                    inputStream.close();
                    connection.disconnect();

                    final long finalTotalBytes = totalBytes;
                    runOnUiThread(() -> {
                        Toast.makeText(this, fileType + " downloaded: " + fileName +
                                " (" + (finalTotalBytes / 1024) + " KB)", Toast.LENGTH_LONG).show();
                        showDownloadCompleteDialog(fileName);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Download failed: Server error", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Download error: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showDownloadCompleteDialog(String fileName) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Download Complete")
                .setMessage(fileName + " has been downloaded to Downloads folder")
                .setPositiveButton("Open", (dialog, which) -> openDownloadedFile(fileName))
                .setNegativeButton("Close", null)
                .show();
    }

    private void openDownloadedFile(String fileName) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadDir, fileName);

        if (file.exists()) {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, getMimeType(fileName));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(Intent.createChooser(intent, "Open with"));
            } catch (Exception e) {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        return "*/*";
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    // ==================== MAP METHODS ====================

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
                if (room.getAddress() != null && !room.getAddress().isEmpty()) {
                    geocodeAddress(room.getAddress());
                }
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

    private void geocodeAddress(String address) {
        if (address == null || address.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                roomLat = location.getLatitude();
                roomLng = location.getLongitude();

                if (mapPreview != null && mapPreview.getController() != null) {
                    GeoPoint geoPoint = new GeoPoint(roomLat, roomLng);
                    mapPreview.getController().setCenter(geoPoint);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
        }
    }

    // ==================== STATUS & BOOKING METHODS ====================

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
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check bookings", e));
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
                    if (btnFavorite != null) {
                        if (doc.exists()) {
                            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                        } else {
                            btnFavorite.setImageResource(R.drawable.ic_favorite_outline);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check favorite", e));
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
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show());
                    } else {
                        Map<String, Object> favorite = new HashMap<>();
                        favorite.put("roomId", roomId);
                        favorite.put("timestamp", System.currentTimeMillis());
                        favorite.put("title", currentRoom != null ? currentRoom.getTitle() : "");

                        doc.getReference().set(favorite)
                                .addOnSuccessListener(aVoid -> {
                                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show());
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

        if (btnBookNow != null) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Sending Request...");
        }

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
                    bookingData.put("bookingDate", new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(new Date()));

                    String finalUserName = userName;
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
                                            if (btnBookNow != null) {
                                                btnBookNow.setEnabled(false);
                                                btnBookNow.setText("Already Requested");
                                            }

                                            FirebaseUtils.sendNotificationToUser(
                                                    ownerId,
                                                    "New Booking Request",
                                                    finalUserName + " wants to book " + currentRoom.getTitle(),
                                                    docRef.getId()
                                            );
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to save booking", Toast.LENGTH_SHORT).show();
                                            if (btnBookNow != null) {
                                                btnBookNow.setEnabled(true);
                                                btnBookNow.setText("Book Now");
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                                if (btnBookNow != null) {
                                    btnBookNow.setEnabled(true);
                                    btnBookNow.setText("Book Now");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get user details", Toast.LENGTH_SHORT).show();
                    if (btnBookNow != null) {
                        btnBookNow.setEnabled(true);
                        btnBookNow.setText("Book Now");
                    }
                });
    }

    // ==================== CONTACT & NAVIGATION METHODS ====================

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
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
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

    // ==================== HELPER METHODS ====================

    private String getSafeString(String str) {
        return str != null ? str : "";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    protected void onResume() {
        super.onResume();
        if (mapPreview != null) {
            try {
                mapPreview.onResume();
            } catch (Exception e) {
                Log.e(TAG, "Error resuming map: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapPreview != null) {
            try {
                mapPreview.onPause();
            } catch (Exception e) {
                Log.e(TAG, "Error pausing map: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openDirections();
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
        }
    }
}