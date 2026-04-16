package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.BookingResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    // Video Preview Components
    private LinearLayout videoPreviewSection;
    private ImageView ivVideoThumbnail;
    private ImageView btnPlayVideoOverlay;
    private MaterialButton btnPlayVideoFull;
    private MaterialButton btnDownloadVideoFull;

    // ==================== DATA HOLDERS ====================
    private double roomLat = 0, roomLng = 0;
    private long roomId;
    private long currentUserId = -1L;
    private Room currentRoom;
    private boolean alreadyRequested = false;
    private boolean isRoomLoaded = false;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> amenitiesList = new ArrayList<>();
    private String videoUrl = null;
    private String contractUrl = null;
    private boolean hasVideo = false;
    private boolean hasContract = false;

    // Backend Components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Initialize components
        initializeBackend();
        initializeOSMDroid();
        executorService = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get user safely
        User user = tokenManager.getUser();
        if (user != null) {
            currentUserId = user.getId();
        }

        // Get room ID
        roomId = getIntent().getLongExtra("room_id", -1L);
        if (roomId == -1L) {
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

    private void initializeBackend() {
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);
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

        // Video Preview Components
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

        // Media Actions
        if (btnViewImages != null) {
            btnViewImages.setOnClickListener(v -> viewAllImages());
        }

        // Video Play Listeners
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
        apiInterface.getRoomById(roomId).enqueue(new Callback<Room>() {
            @Override
            public void onResponse(Call<Room> call, Response<Room> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(RoomDetailsActivity.this, "Failed to load room", Toast.LENGTH_SHORT).show();
                    if (btnBookNow != null) {
                        btnBookNow.setEnabled(false);
                        btnBookNow.setText("Error Loading");
                    }
                    finish();
                    return;
                }

                currentRoom = response.body();
                isRoomLoaded = true;
                roomLat = currentRoom.getLatitude();
                roomLng = currentRoom.getLongitude();

                // Extract media data
                videoUrl = currentRoom.getVideoUrl();
                contractUrl = currentRoom.getContractUrl();
                hasVideo = videoUrl != null && !videoUrl.isEmpty();
                hasContract = contractUrl != null && !contractUrl.isEmpty();

                // FIX: Extract images and convert to full URLs
                imageUrls = new ArrayList<>();
                if (currentRoom.getImages() != null && !currentRoom.getImages().isEmpty()) {
                    List<String> rawImages = currentRoom.getImages();
                    String baseUrl = "https://roomify-backend-2.onrender.com";

                    for (String imgPath : rawImages) {
                        if (imgPath != null && !imgPath.isEmpty()) {
                            String fullUrl;
                            if (imgPath.startsWith("http")) {
                                fullUrl = imgPath;
                            } else if (imgPath.startsWith("/")) {
                                fullUrl = baseUrl + imgPath;
                            } else {
                                fullUrl = baseUrl + "/" + imgPath;
                            }
                            imageUrls.add(fullUrl);
                            Log.d(TAG, "Image URL: " + fullUrl);
                        }
                    }
                    setupImagePager();
                }

                Log.d(TAG, "Has Video: " + hasVideo);
                Log.d(TAG, "Has Contract: " + hasContract);
                Log.d(TAG, "Images count: " + imageUrls.size());

                displayBasicInfo(currentRoom);
                displayOwnerInfo(currentRoom);
                displayAmenities(currentRoom);
                setupMapPreview(currentRoom);
                updateRoomStatus(currentRoom);
                updateBookButtonState();
                updateMediaButtonsVisibility();
            }

            @Override
            public void onFailure(Call<Room> call, Throwable t) {
                Toast.makeText(RoomDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, t.getMessage());
                if (btnBookNow != null) {
                    btnBookNow.setEnabled(false);
                    btnBookNow.setText("Network Error");
                }
            }
        });
    }

    // ==================== DISPLAY METHODS ====================

    private void displayBasicInfo(Room room) {
        if (tvTitle != null) tvTitle.setText(safe(room.getTitle()));
        if (tvPrice != null) tvPrice.setText("$" + room.getPrice() + "/month");
        if (tvAddress != null) tvAddress.setText(safe(room.getAddress()));
        if (tvDescription != null) tvDescription.setText(safe(room.getDescription()));

        if (tvBedrooms != null) {
            tvBedrooms.setText(room.getRoomsCount() + " " + (room.getRoomsCount() == 1 ? "Bedroom" : "Bedrooms"));
        }
        if (tvBathrooms != null) {
            tvBathrooms.setText(room.getBathroomsCount() + " " + (room.getBathroomsCount() == 1 ? "Bathroom" : "Bathrooms"));
        }
        if (tvArea != null && room.getArea() > 0) {
            tvArea.setText(room.getArea() + " m²");
            tvArea.setVisibility(View.VISIBLE);
        } else if (tvArea != null) {
            tvArea.setVisibility(View.GONE);
        }

        if (tvPostedDate != null) {
            String formattedDate = formatDate(room.getCreatedAt());
            tvPostedDate.setText("Posted: " + formattedDate);
        }
    }

    private void displayOwnerInfo(Room room) {
        if (tvOwnerName != null) {
            tvOwnerName.setText(safe(room.getOwnerName()));
        }

        // Set default values for rating and member since (can be enhanced later)
        if (tvOwnerRating != null) tvOwnerRating.setText("★ 4.8 (24 reviews)");
        if (tvMemberSince != null) tvMemberSince.setText("Member since 2023");
        if (ivOwnerProfile != null) {
            ivOwnerProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void displayAmenities(Room room) {
        List<String> amenities = room.getAmenities();
        if (amenities != null && !amenities.isEmpty() && rvAmenities != null) {
            amenitiesList = amenities;
            AmenitiesAdapter adapter = new AmenitiesAdapter(amenitiesList);
            rvAmenities.setAdapter(adapter);
            rvAmenities.setVisibility(View.VISIBLE);
        } else if (rvAmenities != null) {
            rvAmenities.setVisibility(View.GONE);
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

    // ==================== MEDIA METHODS ====================

    private void setupImagePager() {
        if (imageUrls == null || imageUrls.isEmpty() || viewPagerImages == null) {
            if (viewPagerImages != null) viewPagerImages.setVisibility(View.GONE);
            if (imageIndicator != null) imageIndicator.setVisibility(View.GONE);
            if (btnViewImages != null) btnViewImages.setVisibility(View.GONE);
            return;
        }

        // Set fixed height for ViewPager2
        ViewGroup.LayoutParams params = viewPagerImages.getLayoutParams();
        if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT || params.height <= 0) {
            int heightInDp = 250;
            int heightInPx = (int) (heightInDp * getResources().getDisplayMetrics().density);
            params.height = heightInPx;
            viewPagerImages.setLayoutParams(params);
        }

        viewPagerImages.setVisibility(View.VISIBLE);

        // Pass TokenManager to adapter
        ImagePagerAdapter adapter = new ImagePagerAdapter(imageUrls, tokenManager);
        viewPagerImages.setAdapter(adapter);
        viewPagerImages.setOffscreenPageLimit(1);

        setupImageIndicator();

        if (btnViewImages != null) {
            btnViewImages.setVisibility(View.VISIBLE);
        }
    }

    private String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        if (imagePath.startsWith("http")) return imagePath;
        String baseUrl = "https://roomify-backend-2.onrender.com";
        if (imagePath.startsWith("/")) {
            return baseUrl + imagePath;
        }
        return baseUrl + "/" + imagePath;
    }
    private void setupImageIndicator() {
        if (imageIndicator == null || imageUrls.isEmpty()) return;

        imageIndicator.removeAllViews();

        for (int i = 0; i < imageUrls.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
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
                    dot.setBackgroundResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
                }
            }
        });
    }

    private void updateMediaButtonsVisibility() {
        boolean hasValidVideo = hasVideo && videoUrl != null && !videoUrl.isEmpty();
        boolean hasValidContract = hasContract && contractUrl != null && !contractUrl.isEmpty();
        boolean hasImages = !imageUrls.isEmpty();
        boolean hasAnyMedia = hasValidVideo || hasValidContract || hasImages;

        Log.d(TAG, "Media Visibility - Video: " + hasValidVideo + ", Contract: " + hasValidContract + ", Images: " + hasImages);

        // Video Preview Section
        if (videoPreviewSection != null) {
            videoPreviewSection.setVisibility(hasValidVideo ? View.VISIBLE : View.GONE);
            if (hasValidVideo) loadVideoThumbnail(videoUrl);
        }

        // Video Actions Row
        if (videoActionsRow != null) videoActionsRow.setVisibility(hasValidVideo ? View.VISIBLE : View.GONE);

        // Contract Actions Row
        if (contractActionsRow != null) contractActionsRow.setVisibility(hasValidContract ? View.VISIBLE : View.GONE);

        // Individual buttons
        if (btnPlayVideo != null) btnPlayVideo.setVisibility(hasValidVideo ? View.VISIBLE : View.GONE);
        if (btnDownloadVideo != null) btnDownloadVideo.setVisibility(hasValidVideo ? View.VISIBLE : View.GONE);
        if (btnPlayVideoFull != null) btnPlayVideoFull.setVisibility(hasValidVideo ? View.VISIBLE : View.GONE);
        if (btnDownloadVideoFull != null) btnDownloadVideoFull.setVisibility(hasValidVideo ? View.VISIBLE : View.GONE);
        if (btnViewContractDoc != null) btnViewContractDoc.setVisibility(hasValidContract ? View.VISIBLE : View.GONE);
        if (btnDownloadContract != null) btnDownloadContract.setVisibility(hasValidContract ? View.VISIBLE : View.GONE);

        // Hide View Images button if no images
        if (btnViewImages != null && !hasImages) btnViewImages.setVisibility(View.GONE);

        // Hide entire media card if no media
        if (mediaActionsLayout != null) {
            mediaActionsLayout.setVisibility(hasAnyMedia ? View.VISIBLE : View.GONE);
        }
    }

    private void loadVideoThumbnail(String videoUrl) {
        if (ivVideoThumbnail == null) return;

        // Set default placeholder
        ivVideoThumbnail.setImageResource(android.R.drawable.ic_media_play);

        if (videoUrl != null && !videoUrl.isEmpty()) {
            // Check if it's a local file URI or remote URL
            if (videoUrl.startsWith("content://") || videoUrl.startsWith("file://")) {
                // Local file - use Glide with local URI
                try {
                    Glide.with(this)
                            .load(Uri.parse(videoUrl))
                            .placeholder(android.R.drawable.ic_media_play)
                            .error(android.R.drawable.ic_media_play)
                            .into(ivVideoThumbnail);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load local video thumbnail: " + e.getMessage());
                    ivVideoThumbnail.setImageResource(android.R.drawable.ic_media_play);
                }
            } else {
                // Remote URL - try to load with Glide, but handle errors gracefully
                try {
                    Glide.with(this)
                            .load(videoUrl)
                            .placeholder(android.R.drawable.ic_media_play)
                            .error(android.R.drawable.ic_media_play)
                            .into(ivVideoThumbnail);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load remote video thumbnail: " + e.getMessage());
                    ivVideoThumbnail.setImageResource(android.R.drawable.ic_media_play);
                }
            }
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
            roomMarker.setTitle(safe(room.getTitle()));
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

    // ==================== MEDIA ACTION METHODS ====================

    private void viewAllImages() {
        if (imageUrls == null || imageUrls.isEmpty()) {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MediaViewerActivity.class);
        // FIXED: Pass as Integer, not String
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, MediaViewerActivity.MEDIA_TYPE_IMAGES);  // This is int
        intent.putStringArrayListExtra(MediaViewerActivity.EXTRA_MEDIA_URLS, new ArrayList<>(imageUrls));
        intent.putExtra(MediaViewerActivity.EXTRA_CURRENT_POSITION, viewPagerImages != null ? viewPagerImages.getCurrentItem() : 0);
        intent.putExtra(MediaViewerActivity.EXTRA_ROOM_TITLE, currentRoom != null ? currentRoom.getTitle() : "Room");
        startActivity(intent);
    }

    private void playVideo() {
        if (!hasVideo || videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "No video available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> videoUrls = new ArrayList<>();
        videoUrls.add(videoUrl);

        Intent intent = new Intent(this, MediaViewerActivity.class);
        // FIXED: Pass as Integer, not String
        intent.putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, MediaViewerActivity.MEDIA_TYPE_VIDEO);  // This is int
        intent.putStringArrayListExtra(MediaViewerActivity.EXTRA_MEDIA_URLS, new ArrayList<>(videoUrls));
        intent.putExtra(MediaViewerActivity.EXTRA_ROOM_TITLE, currentRoom.getTitle());
        startActivity(intent);
    }


    private void downloadVideo() {
        if (!hasVideo || videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "No video available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }

        String fileName = "room_video_" + roomId + "_" + System.currentTimeMillis() + ".mp4";
        downloadFile(videoUrl, fileName, "Video");
    }

    private void viewContract() {
        if (!hasContract || contractUrl == null || contractUrl.isEmpty()) {
            Toast.makeText(this, "No contract available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(contractUrl), "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open contract with"));
    }

    private void downloadContract() {
        if (!hasContract || contractUrl == null || contractUrl.isEmpty()) {
            Toast.makeText(this, "No contract available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }

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
                    if (!downloadDir.exists()) downloadDir.mkdirs();

                    File outputFile = new File(downloadDir, fileName);
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                    connection.disconnect();

                    runOnUiThread(() -> {
                        Toast.makeText(this, fileType + " downloaded to Downloads folder", Toast.LENGTH_LONG).show();
                        showDownloadCompleteDialog(fileName);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show());
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
            Uri fileUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                    FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file) :
                    Uri.fromFile(file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = fileName.endsWith(".pdf") ? "application/pdf" : "video/mp4";
            intent.setDataAndType(fileUri, mimeType);
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

    // ==================== BOOKING & FAVORITE METHODS ====================

    private void updateBookButtonState() {
        if (currentRoom == null) return;

        if (currentUserId != -1 && currentUserId == currentRoom.getPostedBy()) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Your Room");
        } else if (alreadyRequested) {
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Already Requested");
        } else {
            btnBookNow.setEnabled(true);
            btnBookNow.setText("Book Now");
        }
    }

    private void checkIfAlreadyRequested() {
        if (currentUserId == -1) return;

        apiInterface.checkUserBooking(currentUserId, roomId).enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BookingResponse> bookings = response.body().getData();
                    alreadyRequested = bookings != null && !bookings.isEmpty();
                    if (alreadyRequested && btnBookNow != null) {
                        btnBookNow.setEnabled(false);
                        btnBookNow.setText("Already Requested");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                Log.e(TAG, "Failed to check bookings", t);
            }
        });
    }

    private void checkIfFavorite() {
        if (currentUserId == -1 || btnFavorite == null) return;

        apiInterface.isFavorite(currentUserId, roomId).enqueue(new Callback<ApiResponse<Boolean>>() {
            @Override
            public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null && btnFavorite != null) {
                    Boolean isFav = response.body().getData();
                    if (isFav != null) {
                        btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                Log.e(TAG, "Failed to check favorite", t);
            }
        });
    }

    private void toggleFavorite() {
        if (currentUserId == -1) {
            Toast.makeText(this, "Please login to save favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        apiInterface.toggleFavorite(currentUserId, roomId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && btnFavorite != null) {
                    boolean isNowFavorite = response.body().getMessage() != null &&
                            response.body().getMessage().contains("added");
                    btnFavorite.setImageResource(isNowFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
                    Toast.makeText(RoomDetailsActivity.this,
                            isNowFavorite ? "Added to favorites" : "Removed from favorites",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Failed to toggle favorite", t);
                Toast.makeText(RoomDetailsActivity.this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestRoomBooking() {
        if (currentRoom == null) {
            Toast.makeText(this, "Room not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBookNow.setEnabled(false);
        btnBookNow.setText("Sending...");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(new Date());
        String endDate = sdf.format(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));

        BookingRequest booking = new BookingRequest();
        booking.setRoomId(roomId);
        booking.setUserId(currentUserId);
        booking.setStatus("PENDING");
        booking.setTotalPrice(currentRoom.getPrice());
        booking.setNumberOfGuests(1);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setBookingDate(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));

        Log.d(TAG, "Sending booking request - RoomId: " + roomId + ", UserId: " + currentUserId);

        apiInterface.createBooking(booking).enqueue(new Callback<ApiResponse<BookingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingResponse>> call,
                                   Response<ApiResponse<BookingResponse>> response) {
                btnBookNow.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<BookingResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(RoomDetailsActivity.this, "Booking request sent!", Toast.LENGTH_SHORT).show();
                        alreadyRequested = true;
                        updateBookButtonState();
                    } else {
                        Toast.makeText(RoomDetailsActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RoomDetailsActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                }
                btnBookNow.setText("Book Now");
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingResponse>> call, Throwable t) {
                btnBookNow.setEnabled(true);
                btnBookNow.setText("Book Now");
                Toast.makeText(RoomDetailsActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== CONTACT & NAVIGATION METHODS ====================

    private void openDirections() {
        if (currentRoom == null || (currentRoom.getLatitude() == 0 && currentRoom.getLongitude() == 0)) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
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
        if (phone != null && !phone.isEmpty() && !"null".equals(phone)) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Owner phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void messageRoomOwner() {
        if (currentRoom == null) return;
        String email = currentRoom.getContactEmail();
        if (email != null && !email.isEmpty() && !"null".equals(email)) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Room: " + currentRoom.getTitle());
            startActivity(Intent.createChooser(intent, "Send Email"));
        } else {
            Toast.makeText(this, "Owner email not available", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== HELPER METHODS ====================

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Recently";
        }
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = isoFormat.parse(dateString);
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return dateString.length() > 10 ? dateString.substring(0, 10) : dateString;
        }
    }

    private String safe(String s) {
        return s != null ? s : "";
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