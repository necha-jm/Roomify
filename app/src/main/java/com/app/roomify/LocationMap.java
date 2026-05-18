package com.app.roomify;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.app.roomify.database.RoomEntity;
import com.app.roomify.database.RoomifyDatabase;
import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.BookingResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.app.roomify.sync.OfflineSyncManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_FINE_CODE = 100;
    private static final int REQUEST_CHECK_SETTINGS = 101;
    private static final int LOCATION_TIMEOUT = 10000;
    private static final String TAG = "LocationMap";

    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleSignInClient googleSignInClient;

    private boolean isFirstLocationUpdate = true;
    private boolean isMapReady = false;
    private boolean isLocationRequestActive = false;
    private boolean hasShownLocationDialog = false;

    // Backend Components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private Long currentUserId = null;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    // UI Elements
    private MaterialCardView searchCard;
    private EditText searchEditText;
    private FloatingActionButton fabLocation;
    private MaterialCardView apartmentCard;
    private MaterialCardView addRoomCard;
    private MaterialCardView languageCard;
    private LinearLayout languageSelection, tabProfile, tabSelect;
    private Chip chipEnglish, chipSwahili;
    private ImageView btnSettings;
    private LinearLayout bottomSheet, tabMenu;
    private com.google.android.material.bottomsheet.BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private com.google.android.material.button.MaterialButton btnSearchAddress;
    private View overlayView;
    private com.google.android.material.progressindicator.CircularProgressIndicator progressIndicator;

    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Map markers
    private Marker currentLocationMarker;
    private Marker searchMarker;

    // Data holders
    private final List<Room> roomsList = new ArrayList<>();
    private final Map<Marker, Room> markerRoomMap = new ConcurrentHashMap<>();

    // Track booking status for each room
    private final Map<Long, String> roomBookingStatus = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        // Initialize backend components
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Get current user ID
        User currentUser = tokenManager.getUser();

        if (!tokenManager.isLoggedIn() || currentUser == null) {
            Log.e(TAG, "User not logged in or session expired");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = currentUser.getId();
        Log.d(TAG, "Logged in user ID: " + currentUserId);

        // Initialize Google Sign-In
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        Log.d(TAG, "onCreate started");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupClickListeners();
        setupSearch();
        setupSearchButton();


        // Check location and permissions
        if (!isLocationEnabled()) {
            promptEnableLocation();
        } else {
            requestLocationPermission();
        }

        Log.d(TAG, "onCreate completed");
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");
        try {
            searchCard = findViewById(R.id.search_card);
            searchEditText = findViewById(R.id.search_edittext);
            fabLocation = findViewById(R.id.fab_location);
            tabSelect = findViewById(R.id.tab_SELECT);

            btnSearchAddress = findViewById(R.id.btn_search_address);
            overlayView = findViewById(R.id.overlayView);
            progressIndicator = findViewById(R.id.progressIndicator);

            bottomSheet = findViewById(R.id.bottom_sheet);
            if (bottomSheet != null) {
                bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                bottomSheetBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetBehavior.setPeekHeight(300);
                bottomSheetBehavior.setHideable(false);
            }

            apartmentCard = findViewById(R.id.apartment_card);
            languageCard = findViewById(R.id.language_card);
            tabProfile = findViewById(R.id.tab_profile);
            tabMenu = findViewById(R.id.tab_menu);

            languageSelection = findViewById(R.id.language_selection);
            chipEnglish = findViewById(R.id.chip_english);
            chipSwahili = findViewById(R.id.chip_swahili);
            btnSettings = findViewById(R.id.btn_settings);

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateBasedOnRole() {
        if (!tokenManager.isLoggedIn()) {
            Log.e(TAG, "User not logged in");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        User user = tokenManager.getUser();

        if (user == null) {
            Log.e(TAG, "User object is null");
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        String role = user.getRole();
        Log.d(TAG, "User role: " + role);
        Log.d(TAG, "User name: " + user.getName());
        Log.d(TAG, "User email: " + user.getEmail());

        if (role != null && role.equalsIgnoreCase("owner")) {
            Log.d(TAG, "Navigating to OwnerDashboard");
            startActivity(new Intent(this, OwnerDashboard.class));
        } else if (role != null && role.equalsIgnoreCase("tenant")) {
            Log.d(TAG, "Navigating to DashboardActivity");
            startActivity(new Intent(this, DashboardActivity.class));
        } else {
            // Default fallback - check if role is missing or unknown
            Log.w(TAG, "Unknown or missing role: " + role + ", defaulting to DashboardActivity");
            Toast.makeText(this, "Role not recognized, redirecting to tenant dashboard", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
        }
    }
    private void setupClickListeners() {
        try {
            // Language selection
            if (chipEnglish != null) {
                chipEnglish.setOnClickListener(v -> {
                    changeLanguage("en");
                    chipEnglish.setChecked(true);
                    if (chipSwahili != null) chipSwahili.setChecked(false);
                    if (languageSelection != null) languageSelection.setVisibility(View.GONE);
                });
            }

            if (chipSwahili != null) {
                chipSwahili.setOnClickListener(v -> {
                    changeLanguage("sw");
                    chipSwahili.setChecked(true);
                    if (chipEnglish != null) chipEnglish.setChecked(false);
                    if (languageSelection != null) languageSelection.setVisibility(View.GONE);
                });
            }

            // Apartment card click - show all apartments
            if (apartmentCard != null) {
                apartmentCard.setOnClickListener(v -> {
                    if (roomsList.isEmpty()) {
                        Toast.makeText(this, "No rooms available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showAllRoomsOnMap();
                });
            }

            // SELECT tab navigation
            if (tabSelect != null) {
                tabSelect.setOnClickListener(v -> navigateBasedOnRole());
            }

            // Home tab
            if (tabMenu != null) {
                tabMenu.setOnClickListener(v -> {
                    Intent intent = new Intent(LocationMap.this, LocationMap.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }

            // Language card click
            if (languageCard != null) {
                languageCard.setOnClickListener(v -> {
                    if (languageSelection != null) {
                        languageSelection.setVisibility(
                                languageSelection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
                        );
                    }
                });
            }

            // My location FAB
            if (fabLocation != null) {
                fabLocation.setOnClickListener(v -> {
                    if (currentLocation != null && myMap != null) {
                        LatLng latLng = new LatLng(
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude());
                        animateToLocation(latLng, 16f);
                    } else {
                        getLastLocationWithTimeout();
                    }
                });
            }

            // Profile tab
            if (tabProfile != null) {
                tabProfile.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    startActivity(intent);
                });
            }

            // Settings button
            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
            }

            Log.d(TAG, "Click listeners setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage());
        }
    }

    private void animateToLocation(LatLng latLng, float zoom) {
        if (myMap == null || latLng == null) return;

        myMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, zoom),
                1000,
                null
        );
    }

    private void changeLanguage(String lang) {
        LocaleHelper.setLocale(this, lang);
        Intent intent = new Intent(this, LocationMap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }



    // Add this method to search for rooms in a specific area
    private void searchRoomsByArea(String areaName) {
        if (areaName.isEmpty() || myMap == null) {
            Toast.makeText(this, "Please enter an area to search", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(areaName, 1);

                runOnUiThread(() -> {
                    showLoading(false);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng searchLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                        // Filter rooms by distance from search location
                        List<Room> nearbyRooms = filterRoomsByDistance(searchLatLng, 5000); // 5km radius

                        if (nearbyRooms.isEmpty()) {
                            Toast.makeText(this, "No rooms found in " + areaName, Toast.LENGTH_SHORT).show();
                            // Still animate to the location
                            animateToLocation(searchLatLng, 14f);
                        } else {
                            Toast.makeText(this, "Found " + nearbyRooms.size() + " rooms in " + areaName, Toast.LENGTH_SHORT).show();
                            updateMapWithFilteredRooms(nearbyRooms);

                            // Animate to show all found rooms
                            animateToRoomBounds(nearbyRooms, searchLatLng);
                        }

                        // Add search marker
                        updateSearchMarker(searchLatLng, areaName, getAddressString(address));

                    } else {
                        Toast.makeText(this, "Location not found: " + areaName, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error searching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Geocoder error: " + e.getMessage());
            }
        }).start();
    }

    // Filter rooms by distance from a location
    private List<Room> filterRoomsByDistance(LatLng center, double radiusInMeters) {
        List<Room> nearbyRooms = new ArrayList<>();

        for (Room room : roomsList) {
            if (room.getLatitude() == 0 || room.getLongitude() == 0) continue;

            LatLng roomLocation = new LatLng(room.getLatitude(), room.getLongitude());
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    center.latitude, center.longitude,
                    roomLocation.latitude, roomLocation.longitude,
                    results
            );

            float distanceInMeters = results[0];

            // Check if room is within radius and is available
            if (distanceInMeters <= radiusInMeters) {
                String status = roomBookingStatus.get(room.getId());
                if (status == null || !"BOOKED".equalsIgnoreCase(status)) {
                    nearbyRooms.add(room);
                }
            }
        }

        return nearbyRooms;
    }

    // Update map with filtered rooms
    private void updateMapWithFilteredRooms(List<Room> filteredRooms) {
        if (myMap == null) return;

        // Clear existing room markers (keep current location marker)
        for (Marker marker : markerRoomMap.keySet()) {
            marker.remove();
        }
        markerRoomMap.clear();

        // Add filtered rooms to map
        for (Room room : filteredRooms) {
            if (room.getLatitude() == 0 || room.getLongitude() == 0) continue;

            LatLng roomLocation = new LatLng(room.getLatitude(), room.getLongitude());
            String status = roomBookingStatus.get(room.getId());
            boolean isAvailable = (status == null || "AVAILABLE".equalsIgnoreCase(status));

            float markerColor = isAvailable ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_RED;
            String snippet = isAvailable ? "Available - $" + room.getPrice() : "Not Available";

            Marker marker = myMap.addMarker(
                    new MarkerOptions()
                            .position(roomLocation)
                            .title(room.getTitle())
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            );

            if (marker != null) {
                marker.setTag(room.getId());
                markerRoomMap.put(marker, room);
            }
        }

        Log.d(TAG, "Updated map with " + filteredRooms.size() + " filtered rooms");
    }

    // Animate camera to show all filtered rooms
    private void animateToRoomBounds(List<Room> rooms, LatLng searchLocation) {
        if (myMap == null || rooms.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Add search location
        if (searchLocation != null) {
            builder.include(searchLocation);
        }

        // Add all room locations
        for (Room room : rooms) {
            builder.include(new LatLng(room.getLatitude(), room.getLongitude()));
        }

        // Add current location if available
        if (currentLocation != null) {
            builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }

        LatLngBounds bounds = builder.build();
        int padding = 100; // pixels
        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }


    private void setupSearchButton() {
        if (btnSearchAddress != null) {
            btnSearchAddress.setOnClickListener(v -> {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    Log.d(TAG, "Search button clicked for: " + query);
                    hideKeyboard();
                    performSearch(query);
                } else {
                    Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Update or add search marker
    private void updateSearchMarker(LatLng position, String title, String snippet) {
        if (searchMarker == null) {
            searchMarker = myMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        } else {
            searchMarker.setPosition(position);
            searchMarker.setTitle(title);
            searchMarker.setSnippet(snippet);
        }

        if (searchMarker != null) {
            searchMarker.showInfoWindow();
        }
    }




    // Search rooms near current location
    private void searchByCurrentLocation(double radiusKm) {
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        double radiusMeters = radiusKm * 1000;

        List<Room> nearbyRooms = filterRoomsByDistance(currentLatLng, radiusMeters);

        if (nearbyRooms.isEmpty()) {
            Toast.makeText(this, "No rooms found within " + radiusKm + " km", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Found " + nearbyRooms.size() + " rooms within " + radiusKm + " km", Toast.LENGTH_SHORT).show();
            updateMapWithFilteredRooms(nearbyRooms);
            animateToRoomBounds(nearbyRooms, currentLatLng);
        }
    }

    // Reset map to show all rooms
    private void resetToAllRooms() {
        if (roomsList.isEmpty()) {
            Toast.makeText(this, "No rooms available", Toast.LENGTH_SHORT).show();
            return;
        }

        updateMapWithRooms(roomsList);
        showAllRoomsOnMap();
        clearSearchMarker();
        Toast.makeText(this, "Showing all rooms", Toast.LENGTH_SHORT).show();
    }

    private void performSearch(String query) {
        if (query.isEmpty() || myMap == null) {
            Toast.makeText(this, "Cannot perform search", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 5); // Get up to 5 results

                runOnUiThread(() -> {
                    showLoading(false);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng searchLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                        // Animate camera to searched location
                        animateToLocation(searchLatLng, 14f);

                        // Add search marker
                        String addressString = getAddressString(address);
                        updateSearchMarker(searchLatLng, query, addressString);

                        // Search for rooms near this area
                        searchRoomsInArea(searchLatLng, query, addressString);

                        Toast.makeText(this, "Showing rooms near: " + query, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Location not found: " + query, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error searching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Geocoder error: " + e.getMessage());
            }
        }).start();
    }


    private void searchRoomsInArea(LatLng searchLocation, String areaName, String fullAddress) {
        if (roomsList.isEmpty()) {
            Toast.makeText(this, "No rooms available to search", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search radius in meters (5km by default)
        double searchRadius = 5000; // 5km

        // Filter rooms within the search radius
        List<Room> nearbyRooms = new ArrayList<>();

        for (Room room : roomsList) {
            if (room.getLatitude() == 0 || room.getLongitude() == 0) continue;

            LatLng roomLocation = new LatLng(room.getLatitude(), room.getLongitude());
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    searchLocation.latitude, searchLocation.longitude,
                    roomLocation.latitude, roomLocation.longitude,
                    results
            );

            float distanceInMeters = results[0];

            // Check if room is within radius
            if (distanceInMeters <= searchRadius) {
                nearbyRooms.add(room);
                Log.d(TAG, "Room " + room.getTitle() + " is " + distanceInMeters + " meters away");
            }
        }

        if (nearbyRooms.isEmpty()) {
            Toast.makeText(this, "No rooms found within 5km of " + areaName, Toast.LENGTH_LONG).show();
            // Still show all rooms but zoomed to area
            updateMapWithFilteredRooms(roomsList);
        } else {
            Toast.makeText(this, "Found " + nearbyRooms.size() + " rooms near " + areaName, Toast.LENGTH_SHORT).show();
            updateMapWithFilteredRooms(nearbyRooms);

            // Animate to show all found rooms within bounds
            animateToRoomBounds(nearbyRooms, searchLocation);
        }
    }

    private void searchRoomsWithRadius(LatLng searchLocation, double radiusKm, String areaName) {
        if (roomsList.isEmpty()) {
            Toast.makeText(this, "No rooms available to search", Toast.LENGTH_SHORT).show();
            return;
        }

        double radiusMeters = radiusKm * 1000;
        List<Room> nearbyRooms = new ArrayList<>();

        for (Room room : roomsList) {
            if (room.getLatitude() == 0 || room.getLongitude() == 0) continue;

            LatLng roomLocation = new LatLng(room.getLatitude(), room.getLongitude());
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    searchLocation.latitude, searchLocation.longitude,
                    roomLocation.latitude, roomLocation.longitude,
                    results
            );

            if (results[0] <= radiusMeters) {
                nearbyRooms.add(room);
            }
        }

        if (nearbyRooms.isEmpty()) {
            Toast.makeText(this, "No rooms found within " + radiusKm + " km of " + areaName, Toast.LENGTH_LONG).show();
            updateMapWithFilteredRooms(roomsList);
        } else {
            Toast.makeText(this, "Found " + nearbyRooms.size() + " rooms within " + radiusKm + " km", Toast.LENGTH_SHORT).show();
            updateMapWithFilteredRooms(nearbyRooms);
            animateToRoomBounds(nearbyRooms, searchLocation);
        }
    }

    private void showRadiusSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search by Radius");

        // Create custom layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText areaInput = new EditText(this);
        areaInput.setHint("Enter area name (e.g., Mwanza)");
        layout.addView(areaInput);

        final EditText radiusInput = new EditText(this);
        radiusInput.setHint("Enter radius in km (e.g., 5)");
        radiusInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(radiusInput);

        builder.setView(layout);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String area = areaInput.getText().toString().trim();
            String radiusText = radiusInput.getText().toString().trim();

            if (area.isEmpty()) {
                Toast.makeText(this, "Please enter an area", Toast.LENGTH_SHORT).show();
                return;
            }

            double radiusKm = 5.0; // Default radius
            if (!radiusText.isEmpty()) {
                radiusKm = Double.parseDouble(radiusText);
            }

            performRadiusSearch(area, radiusKm);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performRadiusSearch(String areaName, double radiusKm) {
        showLoading(true);

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(areaName, 1);

                runOnUiThread(() -> {
                    showLoading(false);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng searchLocation = new LatLng(address.getLatitude(), address.getLongitude());

                        animateToLocation(searchLocation, 14f);
                        updateSearchMarker(searchLocation, areaName, getAddressString(address));
                        searchRoomsWithRadius(searchLocation, radiusKm, areaName);
                    } else {
                        Toast.makeText(this, "Location not found: " + areaName, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error searching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void addClearSearchButton() {
        // Add a clear button to your search card or FAB
        // You can reuse the existing clear functionality in the search EditText
        // The right drawable already clears the search
    }

    // Call this to reset to show all rooms
    private void clearSearchAndShowAllRooms() {
        clearSearchMarker();

        if (roomsList.isEmpty()) {
            loadRoomsFromBackend();
        } else {
            updateMapWithRooms(roomsList);
            showAllRoomsOnMap();
        }

        Toast.makeText(this, "Showing all rooms", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSearch() {
        if (searchEditText == null) {
            Log.e(TAG, "searchEditText is null");
            return;
        }

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    Log.d(TAG, "Keyboard search for: " + query);
                    performSearch(query);
                    hideKeyboard();
                } else {
                    Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // Clear button functionality
        searchEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableRight = searchEditText.getCompoundDrawables()[2];
                if (drawableRight != null) {
                    int drawableWidth = drawableRight.getBounds().width();
                    if (event.getX() >= (searchEditText.getWidth() - drawableWidth
                            - searchEditText.getPaddingRight())) {
                        // Clear the search
                        searchEditText.setText("");
                        clearSearchAndShowAllRooms(); // Show all rooms again
                        return true;
                    }
                }
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboard();
        });

        Log.d(TAG, "Search setup completed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_radius_search) {
            showRadiusSearchDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getAddressString(Address address) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            sb.append(address.getAddressLine(i));
            if (i < address.getMaxAddressLineIndex()) sb.append(", ");
        }
        return sb.toString();
    }

    private void clearSearchMarker() {
        if (searchMarker != null) {
            searchMarker.remove();
            searchMarker = null;
        }
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
            } catch (Exception e) {
                Log.e(TAG, "Error showing loading: " + e.getMessage());
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && searchEditText != null && searchEditText.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_CODE
            );
        } else {
            getLastLocationWithTimeout();
        }
    }

    private void getLastLocationWithTimeout() {
        isLocationRequestActive = true;
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] locationReceived = {false};

        handler.postDelayed(() -> {
            isLocationRequestActive = false;
            if (!locationReceived[0]) {
                Log.d(TAG, "Location timeout. Using default location.");
                Toast.makeText(this, "Location timeout. Using default location.", Toast.LENGTH_SHORT).show();
                initMap();
            }
        }, LOCATION_TIMEOUT);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    locationReceived[0] = true;
                    isLocationRequestActive = false;
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        initMap();
                    } else {
                        requestNewLocationWithTimeout();
                    }
                })
                .addOnFailureListener(e -> {
                    locationReceived[0] = true;
                    isLocationRequestActive = false;
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    initMap();
                });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationWithTimeout() {
        isLocationRequestActive = true;
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] locationReceived = {false};

        handler.postDelayed(() -> {
            isLocationRequestActive = false;
            if (!locationReceived[0]) {
                Log.d(TAG, "New location timeout");
                initMap();
            }
        }, LOCATION_TIMEOUT);

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    locationReceived[0] = true;
                    isLocationRequestActive = false;
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "New location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        if (myMap != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            animateToLocation(latLng, 16f);
                            updateCurrentLocationMarker(location);
                        } else {
                            initMap();
                        }
                    } else {
                        initMap();
                    }
                })
                .addOnFailureListener(e -> {
                    locationReceived[0] = true;
                    isLocationRequestActive = false;
                    Log.e(TAG, "Failed to get new location: " + e.getMessage());
                    initMap();
                });
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found");
            Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void promptEnableLocation() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Location helps you find nearby rooms faster. You can continue without it.")
                .setCancelable(true)
                .setPositiveButton("Turn On", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Continue Without Location", (dialog, which) -> {
                    Toast.makeText(this,
                            "Showing default location (Dar es Salaam)",
                            Toast.LENGTH_SHORT).show();
                    loadMapWithoutLocation();
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    loadMapWithoutLocation();
                })
                .show();
    }

    private void loadMapWithoutLocation() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void startLocationUpdates() {
        try {
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setSmallestDisplacement(10);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        currentLocation = location;
                        updateCurrentLocationMarker(location);
                    }
                }
            };

            checkLocationSettingsAndStartUpdates();
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
        }
    }

    private void updateCurrentLocationMarker(Location location) {
        if (myMap == null) return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationMarker = myMap.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title("my location")
                        .icon(getResizedMarker(R.drawable.ic_location, 80, 80))
        );

        myMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                .center(latLng)
                .radius(50)
                .strokeWidth(2f)
                .strokeColor(android.graphics.Color.BLUE)
                .fillColor(0x220000FF)
        );

        if (isFirstLocationUpdate) {
            animateToLocation(latLng, 14f);
            isFirstLocationUpdate = false;
        }
    }

    private void showRoomPreview(Room room) {
        if (bottomSheetBehavior == null) return;

        try {
            bottomSheetBehavior.setState(
                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            );

            Toast.makeText(this,
                    room.getTitle() + " - $" + room.getPrice(),
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing room preview: " + e.getMessage());
        }
    }

    private void checkLocationSettingsAndStartUpdates() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                );
            }
        });

        task.addOnFailureListener(this, e -> {
            int statusCode = ((ApiException) e).getStatusCode();
            if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(LocationMap.this, REQUEST_CHECK_SETTINGS);
                } catch (Exception sendEx) {
                    Log.e(TAG, "Error showing location settings dialog: " + sendEx.getMessage());
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Map is ready");
        myMap = googleMap;
        isMapReady = true;

        applyMapStyle();
        applyDarkModeIfNeeded();

        myMap.getUiSettings().setZoomControlsEnabled(true);
        myMap.getUiSettings().setCompassEnabled(true);
        myMap.getUiSettings().setMyLocationButtonEnabled(false);
        myMap.getUiSettings().setMapToolbarEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
        }

        LatLng defaultLocation;
        if (currentLocation != null) {
            defaultLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            updateCurrentLocationMarker(currentLocation);
        } else {
            defaultLocation = new LatLng(-6.7924, 39.2083);
        }

        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14f));

        loadRoomsFromBackend();

        myMap.setOnMarkerClickListener(marker -> {
            if ((currentLocationMarker != null && currentLocationMarker.equals(marker)) ||
                    (searchMarker != null && searchMarker.equals(marker))) {
                return false;
            }

            Room room = markerRoomMap.get(marker);
            if (room == null) {
                Log.e(TAG, "No room found for marker");
                return true;
            }

            try {
                Intent intent = new Intent(LocationMap.this, RoomDetailsActivity.class);
                intent.putExtra("room_id", room.getId());
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting RoomDetailsActivity: " + e.getMessage());
                Toast.makeText(this, "Error opening room", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }




    private void loadRoomsFromBackend() {
        if (apiInterface == null || myMap == null) {
            Log.d(TAG, "Cannot load rooms");
            return;
        }

        showLoading(true);
        Log.d(TAG, "Loading rooms from backend...");

        // Use unwrapped response (direct List)
        Call<List<Room>> call = apiInterface.getAllRooms();
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                Log.d(TAG, "API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<Room> rooms = response.body();
                    if (rooms != null && !rooms.isEmpty()) {
                        Log.d(TAG, "✅ Loaded " + rooms.size() + " rooms from API");
                        roomsList.clear();
                        roomsList.addAll(rooms);
                        saveRoomsToLocalDatabase(rooms);
                        fetchBookingStatusForRooms(rooms);
                    } else {
                        Log.w(TAG, "API returned empty room list");
                        showLoading(false);
                        loadRoomsFromLocalDatabase();
                    }
                } else {
                    Log.e(TAG, "Response error: " + response.code());
                    showLoading(false);
                    loadRoomsFromLocalDatabase();
                    Toast.makeText(LocationMap.this, "Using cached data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading rooms: " + t.getMessage());
                loadRoomsFromLocalDatabase();
                Toast.makeText(LocationMap.this, "Network error, using cached data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private BitmapDescriptor getResizedMarker(int drawableRes, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), drawableRes);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private void applyDarkModeIfNeeded() {
        int nightModeFlags = getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            try {
                myMap.setMapStyle(
                        com.google.android.gms.maps.model.MapStyleOptions
                                .loadRawResourceStyle(this, R.raw.map_style)
                );
            } catch (Exception e) {
                Log.e(TAG, "Dark mode style error", e);
            }
        }
    }

    private void loadRoomsFromLocalDatabase() {
        ensureExecutor();
        executorService.execute(() -> {
            try {
                RoomifyDatabase db = RoomifyDatabase.getInstance(this);
                List<RoomEntity> roomEntities = db.roomDao().getAllAvailableRooms();

                runOnUiThread(() -> {
                    if (roomEntities != null && !roomEntities.isEmpty()) {
                        List<Room> rooms = new ArrayList<>();
                        for (RoomEntity entity : roomEntities) {
                            Room room = OfflineSyncManager.convertToRoom(entity);
                            rooms.add(room);
                        }
                        roomsList.clear();
                        roomsList.addAll(rooms);
                        roomBookingStatus.clear();
                        updateMapWithRooms(rooms);
                        Log.d(TAG, "Loaded " + rooms.size() + " rooms from local database");
                    } else {
                        Log.d(TAG, "No cached rooms available");
                        updateMapWithRooms(new ArrayList<>());
                        Toast.makeText(this, "No rooms available offline. Please connect to internet.", Toast.LENGTH_LONG).show();
                    }
                    showLoading(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading from local database: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    updateMapWithRooms(new ArrayList<>());
                });
            }
        });
    }

    private void saveRoomsToLocalDatabase(List<Room> rooms) {
        ensureExecutor();
        executorService.execute(() -> {
            try {
                RoomifyDatabase db = RoomifyDatabase.getInstance(this);
                db.roomDao().deleteAllRooms();

                for (Room room : rooms) {
                    RoomEntity entity = OfflineSyncManager.convertToEntity(room);
                    entity.setSynced(true);
                    db.roomDao().insertRoom(entity);
                }
                Log.d(TAG, "Saved " + rooms.size() + " rooms to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving to local database: " + e.getMessage());
            }
        });
    }

    private void fetchBookingStatusForRooms(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            showLoading(false);
            updateMapWithRooms(new ArrayList<>());
            return;
        }

        final int totalRooms = rooms.size();
        final AtomicInteger completed = new AtomicInteger(0);

        roomBookingStatus.clear();

        for (Room room : rooms) {
            final Long roomId = room.getId();

            if (currentUserId != null) {
                fetchUserBookingForRoom(roomId, rooms, totalRooms, completed);
            } else {
                roomBookingStatus.put(roomId, "AVAILABLE");
                if (completed.incrementAndGet() == totalRooms) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    });
                }
            }
        }
    }

    private void fetchUserBookingForRoom(Long roomId, List<Room> rooms, int totalRooms, AtomicInteger completed) {
        Call<ApiResponse<List<BookingResponse>>> call = apiInterface.checkUserBooking(currentUserId, roomId);

        call.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {

                String status = "AVAILABLE";

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<BookingResponse> bookings = response.body().getData();
                    if (bookings != null && !bookings.isEmpty()) {
                        String bookingStatus = bookings.get(0).getStatus();
                        if (bookingStatus != null) {
                            status = bookingStatus.toUpperCase();
                            Log.d(TAG, "Room " + roomId + " - User booking status: " + status);
                        } else {
                            status = "BOOKED";
                            Log.d(TAG, "Room " + roomId + " - Booked by user (no status)");
                        }
                    } else {
                        checkRoomBookingStatus(roomId, status, rooms, totalRooms, completed);
                        return;
                    }
                } else {
                    Log.w(TAG, "Failed to get user booking for room " + roomId);
                }

                roomBookingStatus.put(roomId, status);
                if (completed.incrementAndGet() == totalRooms) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                Log.e(TAG, "Error fetching user booking for room " + roomId + ": " + t.getMessage());
                roomBookingStatus.put(roomId, "AVAILABLE");
                if (completed.incrementAndGet() == totalRooms) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    });
                }
            }
        });
    }

    private void checkRoomBookingStatus(Long roomId, String currentStatus, List<Room> rooms,
                                        int totalRooms, AtomicInteger completed) {
        Call<ApiResponse<Integer>> countCall = apiInterface.getBookingsCountByRoom(roomId);

        countCall.enqueue(new Callback<ApiResponse<Integer>>() {
            @Override
            public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                String finalStatus = currentStatus;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Integer count = response.body().getData();
                    if (count != null && count > 0) {
                        finalStatus = "BOOKED";
                        Log.d(TAG, "Room " + roomId + " - Booked by others (count: " + count + ")");
                    }
                }

                roomBookingStatus.put(roomId, finalStatus);
                if (completed.incrementAndGet() == totalRooms) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                Log.e(TAG, "Error checking room booking count for " + roomId + ": " + t.getMessage());
                roomBookingStatus.put(roomId, currentStatus);
                if (completed.incrementAndGet() == totalRooms) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    });
                }
            }
        });
    }

    private void updateMapWithRooms(List<Room> rooms) {
        if (myMap == null) return;

        LatLng currentLocLatLng = null;
        if (currentLocation != null) {
            currentLocLatLng = new LatLng(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()
            );
        }

        for (Marker marker : markerRoomMap.keySet()) {
            marker.remove();
        }

        roomsList.clear();
        markerRoomMap.clear();

        if (currentLocLatLng != null && currentLocationMarker == null) {
            currentLocationMarker = myMap.addMarker(
                    new MarkerOptions()
                            .position(currentLocLatLng)
                            .title("My Location")
                            .icon(getResizedMarker(R.drawable.ic_location, 80, 80))
            );
        } else if (currentLocLatLng != null && currentLocationMarker != null) {
            currentLocationMarker.setPosition(currentLocLatLng);
        }

        for (Room room : rooms) {
            if (room.getLatitude() == 0 || room.getLongitude() == 0) continue;

            roomsList.add(room);

            LatLng roomLocation = new LatLng(
                    room.getLatitude(),
                    room.getLongitude()
            );

            String rawStatus = roomBookingStatus.get(room.getId());
            String status = (rawStatus == null) ? "AVAILABLE" : rawStatus.toUpperCase().trim();

            Log.d(TAG, "Room " + room.getId() + " (" + room.getTitle() + ") - Final Status: " + status);

            float markerColor;
            String snippet;

            switch (status) {
                case "PENDING":
                    markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                    snippet = "Pending Request - $" + room.getPrice();
                    break;
                case "ACCEPTED":
                case "CONFIRMED":
                    markerColor = BitmapDescriptorFactory.HUE_GREEN;
                    snippet = "Booking Approved - $" + room.getPrice();
                    break;
                case "REJECTED":
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                    snippet = "Request Rejected - $" + room.getPrice();
                    break;
                case "CANCELLED":
                    markerColor = BitmapDescriptorFactory.HUE_VIOLET;
                    snippet = "Cancelled - $" + room.getPrice();
                    break;
                case "BOOKED":
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                    snippet = "Already Booked";
                    break;
                case "AVAILABLE":
                default:
                    markerColor = BitmapDescriptorFactory.HUE_BLUE;
                    snippet = "Available - $" + room.getPrice();
                    break;
            }

            Marker marker = myMap.addMarker(
                    new MarkerOptions()
                            .position(roomLocation)
                            .title(room.getTitle())
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            );

            if (marker != null) {
                marker.setTag(room.getId());
                markerRoomMap.put(marker, room);
            }
        }

        Log.d(TAG, "✅ Updated map with " + roomsList.size() + " room markers");
        Toast.makeText(this, "Found " + roomsList.size() + " rooms", Toast.LENGTH_SHORT).show();
    }

    private void showAllRoomsOnMap() {
        if (roomsList.isEmpty()) {
            Toast.makeText(this, "No rooms available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (myMap != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (Room room : roomsList) {
                builder.include(new LatLng(room.getLatitude(), room.getLongitude()));
            }

            if (currentLocation != null) {
                builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            }

            LatLngBounds bounds = builder.build();
            int padding = 100;
            myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    private void ensureExecutor() {
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    private void applyMapStyle() {
        if (myMap == null) return;

        try {
            boolean success = myMap.setMapStyle(
                    com.google.android.gms.maps.model.MapStyleOptions
                            .loadRawResourceStyle(this, R.raw.map_style)
            );

            if (!success) {
                Log.e(TAG, "Map style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Map style not found.", e);
        }
    }

    public void saveSingleRoomOffline(Room room) {
        ensureExecutor();
        executorService.execute(() -> {
            try {
                RoomifyDatabase db = RoomifyDatabase.getInstance(this);
                RoomEntity entity = OfflineSyncManager.convertToEntity(room);
                entity.setSynced(false);
                db.roomDao().insertRoom(entity);
                Log.d(TAG, "Room saved offline: " + room.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error saving room offline: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isLocationEnabled()) {
            if (!hasShownLocationDialog) {
                promptEnableLocation();
                hasShownLocationDialog = true;
            }
            loadMapWithoutLocation();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (currentLocation == null) {
                getLastLocationWithTimeout();
            }
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }

        if (myMap != null) {
            // Clear local database cache to force fresh load
            executorService.execute(() -> {
                RoomifyDatabase db = RoomifyDatabase.getInstance(this);
                db.roomDao().deleteAllRooms();
                Log.d(TAG, "Cache cleared on resume");
            });

            // Reload rooms from backend
            loadRoomsFromBackend();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationCallback != null && fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null && fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_FINE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                getLastLocationWithTimeout();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                loadMapWithoutLocation();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "User enabled location settings");
            startLocationUpdates();
        }
    }
}