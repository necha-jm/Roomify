package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
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

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostRoomActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PostRoomActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_PICK_REQUEST_CODE = 200;
    private static final int VIDEO_PICK_REQUEST_CODE = 300;
    private static final int CONTRACT_PICK_REQUEST_CODE = 400;
    private static final int MAX_IMAGES = 5;
    private static final int MAX_VIDEO_SIZE_MB = 10;

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
    private long createdRoomId = -1;

    // Backend Components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private Long currentUserId;

    // Property types
    private String[] propertyTypes = {
            // Residential - Shared
            "Single Room",
            "Shared Room",
            "Double Room",
            "Studio",
            "Dormitory",
            "Co-living Space",
            "Basement",
            "Garden Room",
            "Annex",

            // Residential - Whole Units
            "Apartment",
            "House",
            "Penthouse",
            "Villa",
            "Townhouse",
            "Cabin",
            "Guest House",
            "Serviced Apartment",

            // Commercial
            "Commercial",
            "Office Space",
            "Retail Space",
            "Warehouse"
    };

    // Amenities list
    private String[] amenitiesList = {"WiFi", "Parking", "AC", "Security", "Water", "Electricity",
            "Furnished", "Kitchen", "Balcony", "Gym", "Pool", "Elevator"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_room);

        // Initialize backend components
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Please login to post a room", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Get current user ID
        User currentUser = tokenManager.getUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        } else {
            currentUserId = tokenManager.getUserId();
        }

        Log.d(TAG, "Current User ID: " + currentUserId);

        if (currentUserId == null || currentUserId == -1) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

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
        if (locationToggleGroup != null) {
            locationToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;

                if (checkedId == R.id.btnMapLocation) {
                    mapContainer.setVisibility(View.VISIBLE);
                    manualLocationLayout.setVisibility(View.GONE);
                    if (selectedLatLng != null && googleMap != null) {
                        updateMapLocation(selectedLatLng);
                    }
                } else if (checkedId == R.id.btnManualLocation) {
                    mapContainer.setVisibility(View.GONE);
                    manualLocationLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        try {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            LatLng darEsSalaam = new LatLng(-6.792354, 39.208328);
            selectedLatLng = darEsSalaam;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(darEsSalaam, 12f));
            addMarker(darEsSalaam, "Dar es Salaam");

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }

            googleMap.setOnMapClickListener(latLng -> {
                selectedLatLng = latLng;
                addMarker(latLng, "Selected Location");
                getAddressFromLocation(latLng);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up map: " + e.getMessage());
        }
    }

    private void addMarker(LatLng latLng, String title) {
        if (googleMap == null) return;
        try {
            if (locationMarker != null) locationMarker.remove();
            locationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            if (locationMarker != null) locationMarker.showInfoWindow();
        } catch (Exception e) {
            Log.e(TAG, "Error adding marker: " + e.getMessage());
        }
    }

    private void updateMapLocation(LatLng latLng) {
        if (googleMap != null && latLng != null) {
            try {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                addMarker(latLng, "Selected Location");
            } catch (Exception e) {
                Log.e(TAG, "Error updating map location: " + e.getMessage());
            }
        }
    }

    private void getAddressFromLocation(LatLng latLng) {
        if (latLng == null) return;
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
        if (chipGroupAmenities == null) return;
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
        if (btnAddImages != null) btnAddImages.setOnClickListener(v -> pickImages());
        if (btnAddVideo != null) btnAddVideo.setOnClickListener(v -> pickVideo());
        if (btnAddContract != null) btnAddContract.setOnClickListener(v -> pickContract());
        if (btnSubmitRoom != null) btnSubmitRoom.setOnClickListener(v -> validateAndSubmit());
        if (btnSearchAddress != null) btnSearchAddress.setOnClickListener(v -> searchAddress());
    }

    private void pickImages() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(
                    Intent.createChooser(intent, "Select Images"),
                    IMAGE_PICK_REQUEST_CODE
            );

        } catch (Exception e) {
            Log.e("FilePicker", "Error opening image picker: " + e.getMessage());
            Toast.makeText(this, "Unable to open image picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickVideo() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(
                    Intent.createChooser(intent, "Select Video"),
                    VIDEO_PICK_REQUEST_CODE
            );

        } catch (Exception e) {
            Log.e("FilePicker", "Error opening video picker: " + e.getMessage());
            Toast.makeText(this, "Unable to open video picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickContract() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(
                    Intent.createChooser(intent, "Select PDF Contract"),
                    CONTRACT_PICK_REQUEST_CODE
            );

        } catch (Exception e) {
            Log.e("FilePicker", "Error opening PDF picker: " + e.getMessage());
            Toast.makeText(this, "Unable to open PDF picker", Toast.LENGTH_SHORT).show();
        }
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
                            long videoSize = getFileSize(selectedVideoUri);
                            if (videoSize > MAX_VIDEO_SIZE_MB * 1024 * 1024) {
                                Toast.makeText(this, "Video too large. Please select a video under " + MAX_VIDEO_SIZE_MB + "MB", Toast.LENGTH_LONG).show();
                                selectedVideoUri = null;
                                videoFileName = "";
                            } else {
                                updateSelectedFilesInfo();
                                Toast.makeText(this, "✓ Video selected: " + videoFileName, Toast.LENGTH_LONG).show();
                            }
                        }
                        break;
                    case CONTRACT_PICK_REQUEST_CODE:
                        selectedContractUri = data.getData();
                        if (selectedContractUri != null) {
                            contractFileName = getFileName(selectedContractUri);
                            updateSelectedFilesInfo();
                            Toast.makeText(this, "✓ Contract selected: " + contractFileName, Toast.LENGTH_LONG).show();
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
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                }
            } else {
                fileName = new File(uri.getPath()).getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting filename: " + e.getMessage());
        }
        return fileName;
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        try {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                        if (sizeIndex >= 0) {
                            size = cursor.getLong(sizeIndex);
                        }
                    }
                }
            } else {
                File file = new File(uri.getPath());
                size = file.length();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size: " + e.getMessage());
        }
        return size;
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

        if (tvImageCount != null) {
            tvImageCount.setText(selectedImageUris.size() + " / " + MAX_IMAGES + " photos");
        }

        if (!selectedImageUris.isEmpty() && imagePreviewScroll != null && imagePreviewContainer != null) {
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
    }

    private void updateSelectedFilesInfo() {
        if (tvSelectedFiles == null) return;

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
        // Validate required fields
        String title = etRoomTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String phone = etContactPhone.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etRoomTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Price is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etContactPhone.setError("Contact phone is required");
            return;
        }
        if (TextUtils.isEmpty(ownerName)) {
            etOwnerName.setError("Owner name is required");
            return;
        }

        // Verify user is still authenticated
        if (currentUserId == null || currentUserId == -1) {
            User user = tokenManager.getUser();
            if (user != null) {
                currentUserId = user.getId();
            }
            if (currentUserId == null || currentUserId == -1) {
                showError("Session expired. Please login again.");
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        }

        // Location validation
        int checkedId = locationToggleGroup != null ? locationToggleGroup.getCheckedButtonId() : R.id.btnMapLocation;
        if (checkedId == R.id.btnMapLocation) {
            if (selectedLatLng == null) {
                showError("Please select a location on the map");
                return;
            }
        } else {
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
                String manualAddress = etManualAddress.getText().toString().trim();
                if (!TextUtils.isEmpty(manualAddress)) {
                    selectedAddress = manualAddress;
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

        showLoading(true);

        // Create Room object (just to hold data)
        Room roomData = new Room();

        // Basic info
        roomData.setTitle(title);
        roomData.setDescription(description);
        roomData.setPrice(Double.parseDouble(priceStr));
        roomData.setPropertyType(propertyTypes[spinnerPropertyType.getSelectedItemPosition()]);

        // Location
        roomData.setLatitude(selectedLatLng.latitude);
        roomData.setLongitude(selectedLatLng.longitude);
        roomData.setAddress(TextUtils.isEmpty(selectedAddress) ? "Location set" : selectedAddress);

        // Contact info
        roomData.setContactPhone(phone);
        String email = etContactEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(email)) {
            roomData.setContactEmail(email);
        }
        roomData.setOwnerName(ownerName);

        // Amenities
        roomData.setAmenities(selectedAmenities);

        // Counts
        int roomsCount = 1;
        if (!TextUtils.isEmpty(etRoomsCount.getText().toString())) {
            try {
                roomsCount = Integer.parseInt(etRoomsCount.getText().toString());
            } catch (NumberFormatException e) {}
        }
        roomData.setRoomsCount(roomsCount);

        int bathroomsCount = 1;
        if (!TextUtils.isEmpty(etBathroomsCount.getText().toString())) {
            try {
                bathroomsCount = Integer.parseInt(etBathroomsCount.getText().toString());
            } catch (NumberFormatException e) {}
        }
        roomData.setBathroomsCount(bathroomsCount);

        // Area
        double area = 0;
        if (!TextUtils.isEmpty(etArea.getText().toString())) {
            try {
                area = Double.parseDouble(etArea.getText().toString());
            } catch (NumberFormatException e) {}
        }
        roomData.setArea(area);

        // Rules
        String rulesText = etRules.getText().toString().trim();
        if (!TextUtils.isEmpty(rulesText)) {
            List<String> rulesList = new ArrayList<>();
            if (rulesText.contains(",")) {
                for (String part : rulesText.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) rulesList.add(trimmed);
                }
            } else {
                rulesList.add(rulesText);
            }
            roomData.setRules(rulesList);
        }

        // CRITICAL: Set the owner ID
        roomData.setPostedBy(currentUserId);
        Log.d(TAG, "Setting postedBy to: " + currentUserId);

        // Media flags
        roomData.setHasVideo(selectedVideoUri != null);
        roomData.setHasContract(selectedContractUri != null);

        Log.d(TAG, "Sending room creation request with postedBy: " + roomData.getPostedBy());

        // ============ CREATE REQUEST OBJECT (ONLY SEND NECESSARY FIELDS) ============
        RoomCreateRequest request = new RoomCreateRequest(roomData);

        // Log the JSON being sent (for debugging)
        try {
            Gson gson = new Gson();
            String json = gson.toJson(request);
            Log.d(TAG, "=== REQUEST JSON ===");
            Log.d(TAG, json);
            Log.d(TAG, "===================");
        } catch (Exception e) {
            Log.e(TAG, "Error logging JSON", e);
        }

        // Create the room via API
        Call<Room> call = apiInterface.createRoom(request);
        call.enqueue(new Callback<Room>() {
            @Override
            public void onResponse(Call<Room> call, Response<Room> response) {
                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Room createdRoom = response.body();
                    createdRoomId = createdRoom.getId();
                    Log.d(TAG, "✅ Room created with ID: " + createdRoomId);

                    // Now upload media
                    uploadMedia(createdRoomId);
                } else {
                    showLoading(false);
                    String errorMsg = "Failed to create room";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Room> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }



    private void uploadMedia(long roomId) {

        int totalUploads =
                (selectedImageUris.isEmpty() ? 0 : 1) +   // images = 1 request
                        (selectedVideoUri != null ? 1 : 0) +
                        (selectedContractUri != null ? 1 : 0);

        Log.d(TAG, "Starting media uploads. Total: " + totalUploads);

        if (totalUploads == 0) {
            finishPosting();
            return;
        }

        AtomicInteger completedUploads = new AtomicInteger(0);
        AtomicInteger failedUploads = new AtomicInteger(0);

        Runnable checkCompletion = () -> {
            int completed = completedUploads.incrementAndGet();
            Log.d(TAG, "Upload progress: " + completed + "/" + totalUploads);

            if (completed == totalUploads) {
                runOnUiThread(() -> {
                    if (failedUploads.get() > 0) {
                        showLoading(false);
                        showError("Some files failed to upload. Room created but media upload incomplete.");
                    } else {
                        finishPosting();
                    }
                });
            }
        };

        // ==================== UPLOAD IMAGES (FIXED) ====================
        if (!selectedImageUris.isEmpty()) {
            try {
                List<MultipartBody.Part> parts = new ArrayList<>();

                for (int i = 0; i < selectedImageUris.size(); i++) {
                    Uri imageUri = selectedImageUris.get(i);

                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    byte[] imageBytes = getBytes(inputStream);

                    RequestBody requestFile = RequestBody.create(
                            MediaType.parse("image/*"),
                            imageBytes
                    );

                    MultipartBody.Part part = MultipartBody.Part.createFormData(
                            "images", // MUST match backend
                            "image_" + System.currentTimeMillis() + "_" + i + ".jpg",
                            requestFile
                    );

                    parts.add(part);
                }

                Call<ApiResponse<List<String>>> call = apiInterface.uploadRoomImages(roomId, parts);

                call.enqueue(new Callback<ApiResponse<List<String>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<String>>> call, Response<ApiResponse<List<String>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.d(TAG, "✅ Images uploaded successfully");
                        } else {
                            Log.e(TAG, "❌ Failed to upload images");
                            failedUploads.incrementAndGet();
                        }
                        checkCompletion.run();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<String>>> call, Throwable t) {
                        Log.e(TAG, "❌ Image upload failed: " + t.getMessage());
                        failedUploads.incrementAndGet();
                        checkCompletion.run();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing images: " + e.getMessage());
                failedUploads.incrementAndGet();
                checkCompletion.run();
            }
        }

        // ==================== UPLOAD VIDEO ====================
        if (selectedVideoUri != null) {
            try {
                long videoSize = getFileSize(selectedVideoUri);

                if (videoSize > MAX_VIDEO_SIZE_MB * 1024 * 1024) {
                    Log.e(TAG, "Video too large: " + (videoSize / (1024 * 1024)) + "MB");
                    failedUploads.incrementAndGet();
                    checkCompletion.run();
                } else {
                    InputStream inputStream = getContentResolver().openInputStream(selectedVideoUri);
                    byte[] videoBytes = getBytes(inputStream);

                    RequestBody requestFile = RequestBody.create(
                            MediaType.parse("video/mp4"),
                            videoBytes
                    );

                    MultipartBody.Part body = MultipartBody.Part.createFormData(
                            "video",
                            videoFileName,
                            requestFile
                    );

                    Call<ApiResponse<String>> call = apiInterface.uploadRoomVideo(roomId, body);

                    call.enqueue(new Callback<ApiResponse<String>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Log.d(TAG, "Video uploaded successfully");
                            } else {
                                Log.e(TAG, " Failed to upload video. Code: " + response.code());

                                if (response.code() == 413) {
                                    runOnUiThread(() ->
                                            showError("Video too large. Please select a smaller video (under 10MB)")
                                    );
                                }

                                failedUploads.incrementAndGet();
                            }
                            checkCompletion.run();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                            Log.e(TAG, "Video upload failed: " + t.getMessage());
                            failedUploads.incrementAndGet();
                            checkCompletion.run();
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error preparing video: " + e.getMessage());
                failedUploads.incrementAndGet();
                checkCompletion.run();
            }
        }

        // ==================== UPLOAD CONTRACT ====================
        if (selectedContractUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedContractUri);
                byte[] contractBytes = getBytes(inputStream);

                RequestBody requestFile = RequestBody.create(
                        MediaType.parse("application/pdf"),
                        contractBytes
                );

                MultipartBody.Part body = MultipartBody.Part.createFormData(
                        "contract",
                        contractFileName,
                        requestFile
                );

                Call<ApiResponse<String>> call = apiInterface.uploadRoomContract(roomId, body);

                call.enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.d(TAG, "✅ Contract uploaded successfully");
                        } else {
                            Log.e(TAG, "Failed to upload contract");
                            failedUploads.incrementAndGet();
                        }
                        checkCompletion.run();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                        Log.e(TAG, " Contract upload failed: " + t.getMessage());
                        failedUploads.incrementAndGet();
                        checkCompletion.run();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing contract: " + e.getMessage());
                failedUploads.incrementAndGet();
                checkCompletion.run();
            }
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void finishPosting() {
        runOnUiThread(() -> {
            showLoading(false);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Success!")
                    .setMessage("Your property has been posted successfully!")
                    .setPositiveButton("OK", (dialog, which) -> {
                        Intent intent = new Intent();
                        intent.putExtra("room_posted", true);
                        intent.putExtra("room_id", createdRoomId);
                        setResult(RESULT_OK, intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (overlayView != null) overlayView.setVisibility(show ? View.VISIBLE : View.GONE);
            if (progressIndicator != null) progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            if (btnSubmitRoom != null) btnSubmitRoom.setEnabled(!show);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            try {
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                        .setTextColor(ContextCompat.getColor(this, android.R.color.white))
                        .show();
            } catch (Exception e) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
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
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onPause: " + e.getMessage());
            }
        }
    }
}