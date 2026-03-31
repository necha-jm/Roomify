package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostRoomActivity extends AppCompatActivity {

    private static final String TAG = "PostRoomActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_PICK_REQUEST_CODE = 200;
    private static final int VIDEO_PICK_REQUEST_CODE = 300;
    private static final int CONTRACT_PICK_REQUEST_CODE = 400;
    private static final int MAX_IMAGES = 5;

    // Views
    private MapView mapViewPost;
    private TextView tvSelectedAddress, tvImageCount, tvSelectedFiles;
    private TextInputEditText etRoomTitle, etDescription, etPrice, etRoomsCount, etBathroomsCount, etArea;
    private TextInputEditText etRules, etContactPhone, etContactEmail;
    private Spinner spinnerPropertyType;
    private ChipGroup chipGroupAmenities;
    private MaterialButton btnAddImages, btnAddVideo, btnAddContract, btnSubmitRoom;
    private HorizontalScrollView imagePreviewScroll;
    private LinearLayout imagePreviewContainer;
    private View overlayView;
    private CircularProgressIndicator progressIndicator;

    // Data
    private GeoPoint selectedLocation;
    private String selectedAddress = "";
    private List<String> selectedAmenities = new ArrayList<>();
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri;
    private Uri selectedContractUri;
    private String videoFileName = "";
    private String contractFileName = "";

    // Property types
    private String[] propertyTypes = {"Apartment", "Single Room", "Studio", "House", "Shared Room", "Commercial"};

    // Amenities list
    private String[] amenitiesList = {"WiFi", "Parking", "AC", "Security", "Water", "Electricity",
            "Furnished", "Kitchen", "Balcony", "Gym", "Pool", "Elevator"};

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Marker locationMarker;
    private String currentRoomId;

    // ✅ Track if user is authenticated
    private boolean isUserAuthenticated = false;
    private String authenticatedUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ✅ CRITICAL: Check if user is logged in before anything else
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to post a room", Toast.LENGTH_LONG).show();
            // Redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // ✅ Store authenticated user ID
        authenticatedUserId = currentUser.getUid();
        isUserAuthenticated = true;
        Log.d(TAG, "User authenticated with ID: " + authenticatedUserId);

        // Subscribe to notifications
        FirebaseMessaging.getInstance().subscribeToTopic("rooms")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to rooms topic");
                    } else {
                        Log.e("FCM", "Failed to subscribe to topic");
                    }
                });

        // Initialize OSMDroid
        try {
            Configuration.getInstance().load(
                    getApplicationContext(),
                    getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
            );
        } catch (Exception e) {
            Log.w(TAG, "OSMDroid init warning: " + e.getMessage());
        }

        setContentView(R.layout.activity_post_room);

        // Initialize views
        initializeViews();

        // Setup map
        setupMap();

        // Setup property type spinner
        setupPropertyTypeSpinner();

        // Setup amenities chips
        setupAmenitiesChips();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        try {
            mapViewPost = findViewById(R.id.mapViewPost);
            tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
            tvImageCount = findViewById(R.id.tvImageCount);
            tvSelectedFiles = findViewById(R.id.tvSelectedFiles);

            etRoomTitle = findViewById(R.id.etRoomTitle);
            etDescription = findViewById(R.id.etDescription);
            etPrice = findViewById(R.id.etPrice);
            etRoomsCount = findViewById(R.id.etRoomsCount);
            etBathroomsCount = findViewById(R.id.etBathroomsCount);
            etArea = findViewById(R.id.etArea);
            etRules = findViewById(R.id.etRules);
            etContactPhone = findViewById(R.id.etContactPhone);
            etContactEmail = findViewById(R.id.etContactEmail);

            spinnerPropertyType = findViewById(R.id.spinnerPropertyType);
            chipGroupAmenities = findViewById(R.id.chipGroupAmenities);

            btnAddImages = findViewById(R.id.btnAddImages);
            btnAddVideo = findViewById(R.id.btnAddVideo);
            btnAddContract = findViewById(R.id.btnAddContract);
            btnSubmitRoom = findViewById(R.id.btnSubmitRoom);

            imagePreviewScroll = findViewById(R.id.imagePreviewScroll);
            imagePreviewContainer = findViewById(R.id.imagePreviewContainer);

            overlayView = findViewById(R.id.overlayView);
            progressIndicator = findViewById(R.id.progressIndicator);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
        }
    }

    private void setupMap() {
        if (mapViewPost == null) return;

        try {
            mapViewPost.setMultiTouchControls(true);
            mapViewPost.getController().setZoom(15.0);

            GeoPoint defaultPoint = new GeoPoint(-6.7924, 39.2083);
            mapViewPost.getController().setCenter(defaultPoint);

            MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    selectLocation(p);
                    return true;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    selectLocation(p);
                    return true;
                }
            });
            mapViewPost.getOverlays().add(0, mapEventsOverlay);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(
                        new GpsMyLocationProvider(this), mapViewPost);
                myLocationOverlay.enableMyLocation();
                mapViewPost.getOverlays().add(myLocationOverlay);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up map: " + e.getMessage());
        }
    }

    private void selectLocation(GeoPoint point) {
        selectedLocation = point;

        if (locationMarker != null) {
            mapViewPost.getOverlays().remove(locationMarker);
        }

        locationMarker = new Marker(mapViewPost);
        locationMarker.setPosition(point);
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        locationMarker.setTitle("Selected Location");
        mapViewPost.getOverlays().add(locationMarker);
        mapViewPost.invalidate();

        getAddressFromLocation(point.getLatitude(), point.getLongitude());
    }

    private void getAddressFromLocation(double lat, double lon) {
        tvSelectedAddress.setText("Getting address...");

        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(
                        PostRoomActivity.this, Locale.getDefault());
                List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);

                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        android.location.Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i));
                            if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                        }
                        selectedAddress = sb.toString();
                        tvSelectedAddress.setText(selectedAddress);
                    } else {
                        selectedAddress = lat + ", " + lon;
                        tvSelectedAddress.setText(selectedAddress);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    selectedAddress = lat + ", " + lon;
                    tvSelectedAddress.setText(selectedAddress);
                });
            }
        }).start();
    }

    private void setupPropertyTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, propertyTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPropertyType.setAdapter(adapter);
    }

    private void setupAmenitiesChips() {
        try {
            chipGroupAmenities.removeAllViews();

            for (String amenity : amenitiesList) {
                Chip chip = new Chip(this);
                chip.setText(amenity);
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, android.R.color.holo_blue_dark)));
                chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                chip.setPadding(16, 8, 16, 8);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(8, 8, 8, 8);
                chip.setLayoutParams(params);

                final String currentAmenity = amenity;
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!selectedAmenities.contains(currentAmenity)) {
                            selectedAmenities.add(currentAmenity);
                        }
                        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(this, android.R.color.holo_orange_dark)));
                    } else {
                        selectedAmenities.remove(currentAmenity);
                        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(this, android.R.color.holo_blue_dark)));
                    }
                });

                chipGroupAmenities.addView(chip);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up amenities chips: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        btnAddImages.setOnClickListener(v -> pickImages());
        btnAddVideo.setOnClickListener(v -> pickVideo());
        btnAddContract.setOnClickListener(v -> pickContract());
        btnSubmitRoom.setOnClickListener(v -> validateAndSubmit());
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_PICK_REQUEST_CODE);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_PICK_REQUEST_CODE);
    }

    private void pickContract() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, CONTRACT_PICK_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {
                switch (requestCode) {
                    case IMAGE_PICK_REQUEST_CODE:
                        handleImageSelection(data);
                        break;
                    case VIDEO_PICK_REQUEST_CODE:
                        selectedVideoUri = data.getData();
                        if (selectedVideoUri != null) {
                            videoFileName = getFileName(selectedVideoUri);
                            updateSelectedFilesInfo();
                        }
                        break;
                    case CONTRACT_PICK_REQUEST_CODE:
                        selectedContractUri = data.getData();
                        if (selectedContractUri != null) {
                            contractFileName = getFileName(selectedContractUri);
                            updateSelectedFilesInfo();
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling activity result: " + e.getMessage());
                showError("Error processing selected file");
            }
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "File";
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
            if (documentFile != null && documentFile.getName() != null) {
                fileName = documentFile.getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting filename: " + e.getMessage());
        }
        return fileName;
    }

    private void handleImageSelection(Intent data) {
        selectedImageUris.clear();

        try {
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), MAX_IMAGES);
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    selectedImageUris.add(uri);
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }

            tvImageCount.setText(selectedImageUris.size() + " / " + MAX_IMAGES + " photos");

            if (!selectedImageUris.isEmpty()) {
                imagePreviewScroll.setVisibility(View.VISIBLE);
                imagePreviewContainer.removeAllViews();

                for (Uri imageUri : selectedImageUris) {
                    ImageView imageView = new ImageView(this);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(4, 4, 4, 4);

                    try {
                        Glide.with(this).load(imageUri).override(200, 200).centerCrop().into(imageView);
                    } catch (Exception e) {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }

                    imagePreviewContainer.addView(imageView);
                }
            }

            updateSelectedFilesInfo();
        } catch (Exception e) {
            Log.e(TAG, "Error handling image selection: " + e.getMessage());
            showError("Error loading images");
        }
    }

    private void updateSelectedFilesInfo() {
        StringBuilder info = new StringBuilder();

        if (!selectedImageUris.isEmpty()) {
            info.append(selectedImageUris.size()).append(" photo(s)");
        }

        if (selectedVideoUri != null) {
            if (info.length() > 0) info.append(", ");
            info.append("1 video");
        }

        if (selectedContractUri != null) {
            if (info.length() > 0) info.append(", ");
            info.append("1 contract");
        }

        if (info.length() > 0) {
            tvSelectedFiles.setVisibility(View.VISIBLE);
            tvSelectedFiles.setText("Selected: " + info.toString());
        } else {
            tvSelectedFiles.setVisibility(View.GONE);
        }
    }

    private void validateAndSubmit() {
        try {
            // ✅ CRITICAL: Verify user is still authenticated
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            // ✅ Update authenticated user ID in case it changed
            authenticatedUserId = currentUser.getUid();
            Log.d(TAG, "Posting room as user: " + authenticatedUserId);

            // Validate required fields
            String title = etRoomTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String phone = etContactPhone.getText().toString().trim();
            String roomsCountStr = etRoomsCount.getText().toString().trim();
            String bathroomsCountStr = etBathroomsCount.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                etRoomTitle.setError("Title is required");
                etRoomTitle.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(description)) {
                etDescription.setError("Description is required");
                etDescription.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(priceStr)) {
                etPrice.setError("Price is required");
                etPrice.requestFocus();
                return;
            }

            double price = Double.parseDouble(priceStr);

            if (TextUtils.isEmpty(phone)) {
                etContactPhone.setError("Contact phone is required");
                etContactPhone.requestFocus();
                return;
            }

            if (selectedLocation == null) {
                showError("Please select a location on the map");
                return;
            }

            if (selectedImageUris.isEmpty()) {
                showError("Please select at least one photo");
                return;
            }

            // Parse counts with defaults
            int roomsCount = 1;
            if (!TextUtils.isEmpty(roomsCountStr)) {
                roomsCount = Integer.parseInt(roomsCountStr);
            }

            int bathroomsCount = 1;
            if (!TextUtils.isEmpty(bathroomsCountStr)) {
                bathroomsCount = Integer.parseInt(bathroomsCountStr);
            }

            String propertyType = propertyTypes[spinnerPropertyType.getSelectedItemPosition()];
            String email = etContactEmail.getText().toString().trim();

            showLoading(true);
            saveRoomToFirestore(title, description, price, propertyType, phone, email,
                    roomsCount, bathroomsCount);

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for price, rooms, and bathrooms");
            Log.e(TAG, "Number format error: " + e.getMessage());
            showLoading(false);
        } catch (Exception e) {
            Log.e(TAG, "Error in validateAndSubmit: " + e.getMessage(), e);
            showError("Error: " + e.getMessage());
            showLoading(false);
        }
    }

    private void saveRoomToFirestore(String title, String description, double price,
                                     String propertyType, String phone, String email,
                                     int roomsCount, int bathroomsCount) {

        // ✅ Double-check location
        if (selectedLocation == null) {
            showError("Please select location on map");
            showLoading(false);
            return;
        }

        // ✅ Double-check user authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("You must be logged in to post a room");
            showLoading(false);
            return;
        }

        // ✅ Get the REAL user ID - NEVER use "anonymous"
        String currentUserId = currentUser.getUid();

        // ✅ Validate user ID is not empty or anonymous
        if (currentUserId == null || currentUserId.isEmpty()) {
            showError("Authentication error. Please login again.");
            showLoading(false);
            return;
        }

        // ✅ CRITICAL: Ensure we never use "anonymous"
        if ("anonymous".equals(currentUserId)) {
            showError("Invalid user session. Please login with a valid account.");
            showLoading(false);
            return;
        }

        // Generate room ID
        currentRoomId = db.collection("rooms").document().getId();

        Log.d(TAG, "=== CREATING NEW ROOM ===");
        Log.d(TAG, "Room ID: " + currentRoomId);
        Log.d(TAG, "Posted by (Owner ID): " + currentUserId);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Price: $" + price);

        // Create room data with ALL required fields
        Map<String, Object> room = new HashMap<>();
        room.put("id", currentRoomId);
        room.put("title", title);
        room.put("description", description);
        room.put("price", price);
        room.put("latitude", selectedLocation.getLatitude());
        room.put("longitude", selectedLocation.getLongitude());
        room.put("address", selectedAddress);
        room.put("propertyType", propertyType);
        room.put("contactPhone", phone);
        room.put("contactEmail", email.isEmpty() ? "" : email);
        room.put("amenities", selectedAmenities);
        room.put("imageCount", selectedImageUris.size());
        room.put("hasVideo", selectedVideoUri != null);
        room.put("hasContract", selectedContractUri != null);
        room.put("createdAt", System.currentTimeMillis());
        room.put("isAvailable", true);
        room.put("roomsCount", roomsCount);
        room.put("bathroomsCount", bathroomsCount);
        room.put("postedBy", currentUserId); // ✅ CRITICAL: This MUST be the real user ID
        room.put("status", "active");

        // ✅ Log the postedBy field to verify
        Log.d(TAG, "Room data - postedBy: " + room.get("postedBy"));
        Log.d(TAG, "Room data - owner ID type: " + room.get("postedBy").getClass().getSimpleName());

        // Save to Firestore
        db.collection("rooms").document(currentRoomId)
                .set(room)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room saved successfully with ID: " + currentRoomId);

                    // ✅ Verify the document was saved correctly
                    db.collection("rooms").document(currentRoomId).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String savedPostedBy = doc.getString("postedBy");
                                    Log.d(TAG, "✅ VERIFICATION: postedBy field saved as: '" + savedPostedBy + "'");

                                    if (savedPostedBy == null || savedPostedBy.isEmpty()) {
                                        Log.e(TAG, "❌ CRITICAL ERROR: postedBy field was not saved!");
                                        showError("Error saving owner information. Please try again.");
                                    } else if ("anonymous".equals(savedPostedBy)) {
                                        Log.e(TAG, "❌ CRITICAL ERROR: postedBy is still 'anonymous'!");
                                        showError("Authentication error. Please login again.");
                                    } else {
                                        Log.d(TAG, "✅ Room posted successfully with owner ID: " + savedPostedBy);
                                        showSuccess();
                                    }
                                } else {
                                    Log.e(TAG, "❌ Document not found after save!");
                                    showError("Error saving room. Please try again.");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to verify saved document", e);
                                showSuccess(); // Still show success but log error
                            });

                    // Send notifications
                    sendNewRoomNotification();
                    notifyBackendForNewRoom();

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Failed to post: " + e.getMessage());
                    Log.e(TAG, "❌ Firestore error: " + e.getMessage(), e);
                });
    }

    private void showSuccess() {
        runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Success!")
                    .setMessage("Your property has been posted successfully!\n\n" +
                            "Note: Images will be added in a future update.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        Intent intent = new Intent();
                        intent.putExtra("room_posted", true);
                        intent.putExtra("room_id", currentRoomId);
                        setResult(RESULT_OK, intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private void sendNewRoomNotification() {
        String url = "https://fcm.googleapis.com/fcm/send";

        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                org.json.JSONObject json = new org.json.JSONObject();
                json.put("to", "/topics/rooms");

                org.json.JSONObject notification = new org.json.JSONObject();
                notification.put("title", "New Room Available 🏠");
                notification.put("body", "Check out the latest room posted!");

                json.put("notification", notification);

                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json")
                );

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Authorization", "key=YOUR_SERVER_KEY")
                        .build();

                client.newCall(request).execute();

            } catch (Exception e) {
                Log.e("FCM", "Error sending notification: " + e.getMessage());
            }
        }).start();
    }

    private void notifyBackendForNewRoom() {
        if (currentRoomId == null || currentRoomId.isEmpty()) {
            Log.e("BackendNotify", "Cannot notify: roomId is null");
            return;
        }

        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("http://10.0.2.2:8080/notify-new-room?roomId=" + currentRoomId)
                        .post(okhttp3.RequestBody.create(new byte[0]))
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                Log.d("BackendNotify", "Response: " + response.body().string());

            } catch (Exception e) {
                Log.e("BackendNotify", "Error notifying backend: " + e.getMessage());
            }
        }).start();
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            try {
                if (overlayView != null) {
                    overlayView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (progressIndicator != null) {
                    progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (btnSubmitRoom != null) {
                    btnSubmitRoom.setEnabled(!show);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating loading UI: " + e.getMessage());
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            try {
                if (mapViewPost != null) {
                    Snackbar.make(mapViewPost, message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
                            .show();
                } else {
                    Toast.makeText(PostRoomActivity.this, message, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing error: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewPost != null) mapViewPost.onResume();

        // ✅ Re-check authentication when activity resumes
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewPost != null) mapViewPost.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap();
        }
    }
}