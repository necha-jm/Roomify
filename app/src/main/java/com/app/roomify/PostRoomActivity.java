package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostRoomActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PostRoomActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_PICK_REQUEST_CODE = 200;
    private static final int VIDEO_PICK_REQUEST_CODE = 300;
    private static final int CONTRACT_PICK_REQUEST_CODE = 400;
    private static final int MAX_IMAGES = 5;

    // Views
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private TextView tvSelectedAddress, tvImageCount, tvSelectedFiles;
    private TextInputEditText etRoomTitle, etDescription, etPrice, etRoomsCount, etBathroomsCount, etArea;
    private TextInputEditText etRules, etContactPhone, etContactEmail, etOwnerName;
    private TextInputEditText etManualAddress, etLatitude, etLongitude;
    private Spinner spinnerPropertyType;
    private ChipGroup chipGroupAmenities;
    private MaterialButton btnAddImages, btnAddVideo, btnAddContract, btnSubmitRoom, btnSearchAddress;
    private MaterialButtonToggleGroup locationToggleGroup;
    private View mapContainer, manualLocationLayout;
    private HorizontalScrollView imagePreviewScroll;
    private LinearLayout imagePreviewContainer;
    private View overlayView;
    private CircularProgressIndicator progressIndicator;

    // Data
    private LatLng selectedLatLng;
    private String selectedAddress = "";
    private List<String> selectedAmenities = new ArrayList<>();
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedVideoUri;
    private Uri selectedContractUri;
    private String videoFileName = "";
    private String contractFileName = "";
    private Marker locationMarker;

    // Property types
    private String[] propertyTypes = {"Apartment", "Single Room", "Studio", "House", "Shared Room", "Commercial"};

    // Amenities list
    private String[] amenitiesList = {"WiFi", "Parking", "AC", "Security", "Water", "Electricity",
            "Furnished", "Kitchen", "Balcony", "Gym", "Pool", "Elevator"};

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentRoomId;
    private String authenticatedUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to post a room", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        authenticatedUserId = currentUser.getUid();
        Log.d(TAG, "User authenticated with ID: " + authenticatedUserId);

        // Subscribe to notifications
        FirebaseMessaging.getInstance().subscribeToTopic("rooms")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to rooms topic");
                    }
                });

        setContentView(R.layout.activity_post_room);

        initializeViews();
        setupPropertyTypeSpinner();
        setupAmenitiesChips();
        setupClickListeners();
        setupLocationToggle();

        // Initialize Google Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapViewPost);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initializeViews() {
        mapContainer = findViewById(R.id.mapContainer);
        manualLocationLayout = findViewById(R.id.manualLocationLayout);
        locationToggleGroup = findViewById(R.id.locationToggleGroup);
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
        etOwnerName = findViewById(R.id.etOwnerName);
        etManualAddress = findViewById(R.id.etManualAddress);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);

        spinnerPropertyType = findViewById(R.id.spinnerPropertyType);
        chipGroupAmenities = findViewById(R.id.chipGroupAmenities);

        btnAddImages = findViewById(R.id.btnAddImages);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        btnAddContract = findViewById(R.id.btnAddContract);
        btnSubmitRoom = findViewById(R.id.btnSubmitRoom);
        btnSearchAddress = findViewById(R.id.btnSearchAddress);

        imagePreviewScroll = findViewById(R.id.imagePreviewScroll);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);

        overlayView = findViewById(R.id.overlayView);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupLocationToggle() {
        locationToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btnMapLocation) {
                mapContainer.setVisibility(View.VISIBLE);
                manualLocationLayout.setVisibility(View.GONE);
                if (selectedLatLng != null) {
                    updateMapLocation(selectedLatLng);
                }
            } else if (checkedId == R.id.btnManualLocation) {
                mapContainer.setVisibility(View.GONE);
                manualLocationLayout.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(etManualAddress.getText())) {
                    searchAddress();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set default location to Dar es Salaam
        LatLng darEsSalaam = new LatLng(-6.792354, 39.208328);
        selectedLatLng = darEsSalaam;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(darEsSalaam, 12f));

        // Add marker for Dar es Salaam
        addMarker(darEsSalaam, "Dar es Salaam");

        // Enable location if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Set map click listener
        googleMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            addMarker(latLng, "Selected Location");
            getAddressFromLocation(latLng);
        });
    }

    private void addMarker(LatLng latLng, String title) {
        if (locationMarker != null) {
            locationMarker.remove();
        }
        locationMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        locationMarker.showInfoWindow();
    }

    private void updateMapLocation(LatLng latLng) {
        if (googleMap != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            addMarker(latLng, "Selected Location");
        }
    }

    private void getAddressFromLocation(LatLng latLng) {
        tvSelectedAddress.setText("Getting address...");

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(PostRoomActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i));
                            if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                        }
                        selectedAddress = sb.toString();
                        tvSelectedAddress.setText(selectedAddress);
                    } else {
                        selectedAddress = latLng.latitude + ", " + latLng.longitude;
                        tvSelectedAddress.setText(selectedAddress);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    selectedAddress = latLng.latitude + ", " + latLng.longitude;
                    tvSelectedAddress.setText(selectedAddress);
                });
            }
        }).start();
    }

    private void searchAddress() {
        String address = etManualAddress.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            etManualAddress.setError("Please enter an address");
            return;
        }

        showLoading(true);
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(PostRoomActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(address, 1);

                runOnUiThread(() -> {
                    showLoading(false);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        double lat = addr.getLatitude();
                        double lng = addr.getLongitude();

                        selectedLatLng = new LatLng(lat, lng);
                        selectedAddress = address;

                        etLatitude.setText(String.valueOf(lat));
                        etLongitude.setText(String.valueOf(lng));
                        tvSelectedAddress.setText(address);

                        Toast.makeText(this, "Location found!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error searching address", Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Geocoder error: " + e.getMessage());
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
        chipGroupAmenities.removeAllViews();

        for (String amenity : amenitiesList) {
            Chip chip = new Chip(this);
            chip.setText(amenity);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.holo_blue_dark)));
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));

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
    }

    private void setupClickListeners() {
        btnAddImages.setOnClickListener(v -> pickImages());
        btnAddVideo.setOnClickListener(v -> pickVideo());
        btnAddContract.setOnClickListener(v -> pickContract());
        btnSubmitRoom.setOnClickListener(v -> validateAndSubmit());
        btnSearchAddress.setOnClickListener(v -> searchAddress());
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

                Glide.with(this).load(imageUri).override(200, 200).centerCrop().into(imageView);
                imagePreviewContainer.addView(imageView);
            }
        }

        updateSelectedFilesInfo();
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
        // Verify user is still authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        authenticatedUserId = currentUser.getUid();

        // Validate required fields
        String title = etRoomTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String phone = etContactPhone.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();

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

        if (TextUtils.isEmpty(phone)) {
            etContactPhone.setError("Contact phone is required");
            etContactPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(ownerName)) {
            etOwnerName.setError("Owner name is required");
            etOwnerName.requestFocus();
            return;
        }

        // Get location based on selected mode
        if (locationToggleGroup.getCheckedButtonId() == R.id.btnMapLocation) {
            if (selectedLatLng == null) {
                showError("Please select a location on the map");
                return;
            }
        } else {
            // Manual mode - get coordinates from inputs
            String latStr = etLatitude.getText().toString().trim();
            String lngStr = etLongitude.getText().toString().trim();

            if (TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lngStr)) {
                showError("Please enter valid latitude and longitude coordinates");
                return;
            }

            try {
                double lat = Double.parseDouble(latStr);
                double lng = Double.parseDouble(lngStr);
                selectedLatLng = new LatLng(lat, lng);

                // If address is empty, try to get it from coordinates
                if (TextUtils.isEmpty(selectedAddress)) {
                    getAddressFromLocation(selectedLatLng);
                }
            } catch (NumberFormatException e) {
                showError("Invalid coordinates format");
                return;
            }
        }

        if (selectedImageUris.isEmpty()) {
            showError("Please select at least one photo");
            return;
        }

        double price = Double.parseDouble(priceStr);
        int roomsCount = 1;
        if (!TextUtils.isEmpty(etRoomsCount.getText().toString())) {
            roomsCount = Integer.parseInt(etRoomsCount.getText().toString());
        }

        int bathroomsCount = 1;
        if (!TextUtils.isEmpty(etBathroomsCount.getText().toString())) {
            bathroomsCount = Integer.parseInt(etBathroomsCount.getText().toString());
        }

        String propertyType = propertyTypes[spinnerPropertyType.getSelectedItemPosition()];
        String email = etContactEmail.getText().toString().trim();

        showLoading(true);
        saveRoomToFirestore(title, description, price, propertyType, phone, email,
                roomsCount, bathroomsCount, ownerName);
    }

    private void saveRoomToFirestore(String title, String description, double price,
                                     String propertyType, String phone, String email,
                                     int roomsCount, int bathroomsCount, String ownerName) {
        currentRoomId = db.collection("rooms").document().getId();

        Map<String, Object> room = new HashMap<>();
        room.put("id", currentRoomId);
        room.put("title", title);
        room.put("description", description);
        room.put("price", price);
        room.put("latitude", selectedLatLng.latitude);
        room.put("longitude", selectedLatLng.longitude);
        room.put("address", selectedAddress);
        room.put("propertyType", propertyType);
        room.put("contactPhone", phone);
        room.put("contactEmail", email.isEmpty() ? "" : email);
        room.put("ownerName", ownerName);
        room.put("amenities", selectedAmenities);
        room.put("imageCount", selectedImageUris.size());
        room.put("hasVideo", selectedVideoUri != null);
        room.put("hasContract", selectedContractUri != null);
        room.put("createdAt", System.currentTimeMillis());
        room.put("isAvailable", true);
        room.put("roomsCount", roomsCount);
        room.put("bathroomsCount", bathroomsCount);
        room.put("postedBy", authenticatedUserId);
        room.put("status", "active");

        // Optional fields
        if (!TextUtils.isEmpty(etArea.getText().toString())) {
            room.put("area", Double.parseDouble(etArea.getText().toString()));
        }
        if (!TextUtils.isEmpty(etRules.getText().toString())) {
            room.put("rules", etRules.getText().toString().trim());
        }

        db.collection("rooms").document(currentRoomId)
                .set(room)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Room saved successfully with ID: " + currentRoomId);
                    sendNewRoomNotification();
                    showSuccess();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Failed to post: " + e.getMessage());
                    Log.e(TAG, "Firestore error: " + e.getMessage(), e);
                });
    }

    private void showSuccess() {
        runOnUiThread(() -> {
            showLoading(false);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Success!")
                    .setMessage("Your property has been posted successfully!")
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
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("to", "/topics/rooms");

                org.json.JSONObject notification = new org.json.JSONObject();
                notification.put("title", "New Room Available ");
                notification.put("body", "Check out the latest room posted!");

                json.put("notification", notification);

                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                        json.toString(),
                        okhttp3.MediaType.parse("application/json")
                );

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("https://fcm.googleapis.com/fcm/send")
                        .post(body)
                        .addHeader("Authorization", "key=YOUR_SERVER_KEY")
                        .build();

                client.newCall(request).execute();
            } catch (Exception e) {
                Log.e("FCM", "Error sending notification: " + e.getMessage());
            }
        }).start();
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            overlayView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            btnSubmitRoom.setEnabled(!show);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    .setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    .show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED && googleMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(false);
        }
    }
}