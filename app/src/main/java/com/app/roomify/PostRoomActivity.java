package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
    private List<String> savedImagePaths = new ArrayList<>();

    // Property types
    private String[] propertyTypes = {"Apartment", "Single Room", "Studio", "House", "Shared Room", "Commercial"};

    // Amenities list
    private String[] amenitiesList = {"WiFi", "Parking", "AC", "Security", "Water", "Electricity",
            "Furnished", "Kitchen", "Balcony", "Gym", "Pool", "Elevator"};

    // Firebase
    private FirebaseFirestore db;
    private Marker locationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

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
        savedImagePaths.clear();

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
            // Simple validation - only essential fields
            String title = etRoomTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String phone = etContactPhone.getText().toString().trim();

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

            String propertyType = propertyTypes[spinnerPropertyType.getSelectedItemPosition()];
            String email = etContactEmail.getText().toString().trim();

            showLoading(true);

            // Save directly to Firestore without file uploads
            saveRoomToFirestore(title, description, price, propertyType, phone, email);

        } catch (Exception e) {
            Log.e(TAG, "Error in validateAndSubmit: " + e.getMessage(), e);
            showError("Error: " + e.getMessage());
            showLoading(false);
        }
    }

    private void saveRoomToFirestore(String title, String description, double price,
                                     String propertyType, String phone, String email) {

        // ADD THIS CHECK
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select location on map", Toast.LENGTH_SHORT).show();
            return;
        }

        String roomId = db.collection("rooms").document().getId();

        // Create a simple map with only essential data
        Map<String, Object> room = new HashMap<>();
        room.put("id", roomId);
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
        room.put("imageCount", selectedImageUris.size()); // Store only count, not actual images
        room.put("hasVideo", selectedVideoUri != null);
        room.put("hasContract", selectedContractUri != null);
        room.put("createdAt", System.currentTimeMillis());
        room.put("isAvailable", true);

        Log.d(TAG, "Saving room with ID: " + roomId);
        Log.d(TAG, "Data size: " + room.toString().length() + " characters");

        db.collection("rooms").document(roomId)
                .set(room)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "Room saved successfully!");

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Success!")
                            .setMessage("Your property has been posted successfully!\n\n" +
                                    "Note: Images will be added in a future update.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                Intent intent = new Intent();
                                intent.putExtra("room_posted", true);
                                setResult(RESULT_OK, intent);
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Failed to post: " + e.getMessage());
                    Log.e(TAG, "Firestore error: " + e.getMessage(), e);
                });
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
                btnSubmitRoom.setEnabled(!show);
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap();
        }
    }
}