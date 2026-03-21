package com.app.roomify;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_FINE_CODE = 100;
    private static final int REQUEST_CHECK_SETTINGS = 101;
    private static final int LOCATION_TIMEOUT = 10000; // 10 seconds
    private static final String TAG = "LocationMap";

    private GoogleMap myMap;

    private BroadcastReceiver roomReceiver;
    private BroadcastReceiver gpsStatusReceiver;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db;

    private boolean isFirstLocationUpdate = true;
    private boolean isMapReady = false;
    private boolean shouldRefreshRooms = true;
    private boolean isLocationRequestActive = false;

    // UI Elements
    private MaterialCardView searchCard;
    private EditText searchEditText;
    private FloatingActionButton fabLocation;
    private MaterialCardView apartmentCard;
    private ImageView apartmentIcon;
    private MaterialCardView addRoomCard;
    private MaterialCardView languageCard;
    private LinearLayout languageSelection;
    private Chip chipEnglish;
    private Chip chipSwahili;
    private ImageView btnSettings;
    private LinearLayout bottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Map markers
    private Marker currentLocationMarker;
    private Marker searchMarker;

    // IMPORTANT DATA HOLDERS
    private final List<Room> roomsList = new ArrayList<>();
    private final Map<Marker, Room> markerRoomMap = new HashMap<>();

    TextView contact;

    private static final String ACTION_NEW_ROOM = "com.app.roomify.NEW_ROOM_ADDED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.loadLocale(this);
        setContentView(R.layout.activity_location_map);



        contact = findViewById(R.id.contact);

        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LocationMap.this, ContactActivity.class );
                startActivity(i);

            }
        });

        Log.d(TAG, "onCreate started");

        // Initialize receivers
        initializeReceivers();

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize UI
        initializeViews();
        setupClickListeners();
        setupSearch();

        // Check location and permissions
        if (!isLocationEnabled()) {
            promptEnableLocation();
        } else {
            requestLocationPermission();
        }

        Log.d(TAG, "onCreate completed");
    }

    private void initializeReceivers() {
        // Room receiver
        roomReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_NEW_ROOM.equals(intent.getAction())) {
                    Log.d(TAG, "New room added, refreshing map");
                    shouldRefreshRooms = true;
                    if (myMap != null) {
                        loadRoomsOnMap();
                    }
                }
            }
        };

        // GPS status receiver
        gpsStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                    boolean isGpsEnabled = isLocationEnabled();
                    Log.d(TAG, "GPS status changed: " + (isGpsEnabled ? "Enabled" : "Disabled"));

                    if (isGpsEnabled && currentLocation == null && !isLocationRequestActive) {
                        // GPS just turned on, request location
                        if (ActivityCompat.checkSelfPermission(LocationMap.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            getLastLocationWithTimeout();
                            startLocationUpdates();
                        }
                    }
                }
            }
        };

        // Register receivers
        IntentFilter roomFilter = new IntentFilter(ACTION_NEW_ROOM);
        ContextCompat.registerReceiver(this, roomReceiver, roomFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        IntentFilter gpsFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsStatusReceiver, gpsFilter);
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");
        try {
            // Search elements
            searchCard = findViewById(R.id.search_card);
            searchEditText = findViewById(R.id.search_edittext);
            fabLocation = findViewById(R.id.fab_location);

            // Bottom sheet
            bottomSheet = findViewById(R.id.bottom_sheet);
            if (bottomSheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetBehavior.setPeekHeight(300);
                bottomSheetBehavior.setHideable(false);
            }

            // Quick action cards
            apartmentCard = findViewById(R.id.apartment_card);
            apartmentIcon = findViewById(R.id.apartment);
            addRoomCard = findViewById(R.id.addRoom_card);
            languageCard = findViewById(R.id.language_card);

            // Language selection
            languageSelection = findViewById(R.id.language_selection);
            chipEnglish = findViewById(R.id.chip_english);
            chipSwahili = findViewById(R.id.chip_swahili);

            // Settings
            btnSettings = findViewById(R.id.btn_settings);

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        try {
            // Language selection
            if (chipEnglish != null) {
                chipEnglish.setOnClickListener(v -> {
                    changeLanguage("en");
                    chipEnglish.setChecked(true);
                    if (chipSwahili != null) {
                        chipSwahili.setChecked(false);
                    }
                    if (languageSelection != null) {
                        languageSelection.setVisibility(View.GONE);
                    }
                });
            }

            if (chipSwahili != null) {
                chipSwahili.setOnClickListener(v -> {
                    changeLanguage("sw");
                    chipSwahili.setChecked(true);
                    if (chipEnglish != null) {
                        chipEnglish.setChecked(false);
                    }
                    if (languageSelection != null) {
                        languageSelection.setVisibility(View.GONE);
                    }
                });
            }

            // Apartment card click
            if (apartmentCard != null) {
                apartmentCard.setOnClickListener(v -> {
                    if (roomsList.isEmpty()) {
                        Toast.makeText(this, "No rooms available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    filterApartmentsOnMap();
                });
            }

            // Add room card click
            if (addRoomCard != null) {
                addRoomCard.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(LocationMap.this, PostRoomActivity.class));
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting PostRoomActivity: " + e.getMessage());
                    }
                });
            }

            // Language card click - toggle language selection
            if (languageCard != null) {
                languageCard.setOnClickListener(v -> {
                    if (languageSelection != null) {
                        if (languageSelection.getVisibility() == View.VISIBLE) {
                            languageSelection.setVisibility(View.GONE);
                        } else {
                            languageSelection.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            // My location FAB
            if (fabLocation != null) {
                fabLocation.setOnClickListener(v -> {
                    if (currentLocation != null && myMap != null) {
                        LatLng myLocation = new LatLng(
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude());
                        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f));
                    } else {
                        getLastLocationWithTimeout();
                    }
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

    private void changeLanguage(String lang) {
        LocaleHelper.setLocale(this, lang);

        // Restart activity to apply language changes
        Intent intent = new Intent(this, LocationMap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSearch() {
        if (searchEditText == null) {
            Log.e(TAG, "searchEditText is null");
            return;
        }

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty() && myMap != null) {
                    performGeocodeSearch(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });

        searchEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableRight = searchEditText.getCompoundDrawables()[2];
                if (drawableRight != null) {
                    int drawableWidth = drawableRight.getBounds().width();
                    if (event.getX() >= (searchEditText.getWidth() - drawableWidth
                            - searchEditText.getPaddingRight())) {
                        searchEditText.setText("");
                        clearSearchMarker();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void performGeocodeSearch(String query) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            int retryCount = 0;
            int maxRetries = 3;

            while (retryCount < maxRetries) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addressList = geocoder.getFromLocationName(query, 1);

                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        runOnUiThread(() -> {
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                            addSearchResultMarker(latLng, address.getAddressLine(0));
                        });
                        return;
                    } else {
                        retryCount++;
                        if (retryCount < maxRetries) {
                            Thread.sleep(1000);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder error: " + e.getMessage());
                    retryCount++;
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            runOnUiThread(() ->
                    Toast.makeText(this, "Search failed after multiple attempts", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }
        return false;
    }

    private void addSearchResultMarker(LatLng latLng, String title) {
        if (myMap == null) return;

        clearSearchMarker();

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("Searched: " + title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

        searchMarker = myMap.addMarker(markerOptions);
        if (searchMarker != null) {
            searchMarker.showInfoWindow();
        }
    }

    private void clearSearchMarker() {
        if (searchMarker != null) {
            searchMarker.remove();
            searchMarker = null;
        }
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
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
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
                            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f));
                            updateCurrentLocationMarker(location);
                        } else {
                            initMap();
                        }
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
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
                .setMessage("Location is required to use this app. Please turn on GPS.")
                .setCancelable(false)
                .setPositiveButton("Turn On", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Location is required for map features", Toast.LENGTH_SHORT).show();
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
        if (locationManager == null) return false;

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void startLocationUpdates() {
        try {
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setSmallestDisplacement(10);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult == null) return;

                    for (Location location : locationResult.getLocations()) {
                        if (location == null) continue;

                        if (location.hasAccuracy() && location.getAccuracy() < 50) {
                            updateCurrentLocation(location);
                            break;
                        } else if (currentLocation == null) {
                            updateCurrentLocation(location);
                        }
                    }
                }
            };

            checkLocationSettingsAndStartUpdates();

        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
        }
    }

    private void updateCurrentLocation(Location location) {
        if (currentLocation == null || location.getTime() > currentLocation.getTime()) {
            currentLocation = location;
            runOnUiThread(() -> updateCurrentLocationMarker(location));
        }
    }

    private void updateCurrentLocationMarker(Location location) {
        if (myMap == null) return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentLocationMarker == null) {
            currentLocationMarker = myMap.addMarker(
                    new MarkerOptions()
                            .position(latLng)
                            .title("My Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            );
        } else {
            currentLocationMarker.setPosition(latLng);
        }

        if (isFirstLocationUpdate) {
            myMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 14f)
            );
            isFirstLocationUpdate = false;
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
            switch (statusCode) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(LocationMap.this, REQUEST_CHECK_SETTINGS);
                    } catch (Exception sendEx) {
                        Log.e(TAG, "Error showing location settings dialog: " + sendEx.getMessage());
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    Toast.makeText(this, "Location settings cannot be changed", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Map is ready");
        myMap = googleMap;
        isMapReady = true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        LatLng defaultLocation;
        if (currentLocation != null) {
            defaultLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            updateCurrentLocationMarker(currentLocation);
        } else {
            defaultLocation = new LatLng(-6.7924, 39.2083);
        }

        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14f));

        loadRoomsOnMap();

        // FIXED: Enhanced marker click listener with backup tag lookup
        myMap.setOnMarkerClickListener(marker -> {
            boolean isCurrentLocation = currentLocationMarker != null &&
                    currentLocationMarker.equals(marker);
            boolean isSearchMarker = searchMarker != null &&
                    searchMarker.equals(marker);

            if (isCurrentLocation || isSearchMarker) {
                return false;
            }

            // Try to get room from map first
            Room room = markerRoomMap.get(marker);

            // If not found, try tag backup
            if (room == null && marker.getTag() != null) {
                String roomId = (String) marker.getTag();
                Log.d(TAG, "Looking up room by tag ID: " + roomId);

                for (Room r : roomsList) {
                    if (r.getId().equals(roomId)) {
                        room = r;
                        Log.d(TAG, "Found room from tag: " + r.getTitle());
                        break;
                    }
                }
            }

            if (room == null) {
                Log.e(TAG, "No room found for marker at: " + marker.getPosition());
                Toast.makeText(this, "Error loading room details", Toast.LENGTH_SHORT).show();
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

    // FIXED: Properly managed room loading without interfering with location marker
    private void loadRoomsOnMap() {
        if (db == null) {
            Log.e(TAG, "Firestore db is null");
            return;
        }

        if (myMap == null) {
            Log.d(TAG, "Map not ready yet, will load rooms later");
            return;
        }

        db.collection("rooms")
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    Log.d(TAG, "Received " + snapshots.size() + " rooms from Firestore");

                    // FIX 1: Save current location position before clearing
                    LatLng currentLocLatLng = null;
                    if (currentLocation != null) {
                        currentLocLatLng = new LatLng(
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude()
                        );
                    }

                    // FIX 2: Clear ONLY room markers, preserve location marker
                    for (Marker marker : markerRoomMap.keySet()) {
                        marker.remove();
                    }

                    roomsList.clear();
                    markerRoomMap.clear();

                    // FIX 3: Handle location marker separately - DON'T recreate
                    if (currentLocLatLng != null) {
                        if (currentLocationMarker == null) {
                            // Create only if it doesn't exist
                            currentLocationMarker = myMap.addMarker(
                                    new MarkerOptions()
                                            .position(currentLocLatLng)
                                            .title("My Location")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            );
                        } else {
                            // Just update position of existing marker
                            currentLocationMarker.setPosition(currentLocLatLng);
                        }
                    }

                    // Add room markers
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Room room = doc.toObject(Room.class);
                            room.setId(doc.getId());

                            if (room.getLatitude() == 0 || room.getLongitude() == 0) {
                                Log.w(TAG, "Room " + room.getId() + " has invalid coordinates");
                                continue;
                            }

                            roomsList.add(room);

                            LatLng roomLocation = new LatLng(
                                    room.getLatitude(),
                                    room.getLongitude());

                            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_apartment);
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 40, 40, false);

                            Marker marker = myMap.addMarker(
                                    new MarkerOptions()
                                            .position(roomLocation)
                                            .title(room.getTitle())
                                            .snippet("Price: $" + room.getPrice())
                                            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap))
                            );

                            if (marker != null) {
                                // FIX 4: Add tag as backup identifier
                                marker.setTag(room.getId());
                                markerRoomMap.put(marker, room);
                                Log.d(TAG, "Added marker for room: " + room.getId() + " at " + roomLocation);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing room document: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "Loaded " + roomsList.size() + " rooms with " + markerRoomMap.size() + " markers");

                    // FIX 5: Debug verification
                    debugMarkerMapping();
                });
    }

    // FIX: Added debug method to track marker mapping
    private void debugMarkerMapping() {
        Log.d(TAG, "=== MARKER MAPPING DEBUG ===");
        Log.d(TAG, "Current Location Marker exists: " + (currentLocationMarker != null));
        if (currentLocationMarker != null) {
            Log.d(TAG, "Location Marker at: " + currentLocationMarker.getPosition());
        }
        Log.d(TAG, "Total rooms in list: " + roomsList.size());
        Log.d(TAG, "Total markers in map: " + markerRoomMap.size());

        // Check for rooms without markers
        for (Room room : roomsList) {
            boolean found = false;
            for (Room mappedRoom : markerRoomMap.values()) {
                if (mappedRoom.getId().equals(room.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Log.e(TAG, "WARNING: Room " + room.getId() + " - " + room.getTitle() + " has no marker!");
            }
        }

        // Check if location marker is accidentally in room map
        for (Marker marker : markerRoomMap.keySet()) {
            if (marker.equals(currentLocationMarker)) {
                Log.e(TAG, "ERROR: Current location marker found in room map!");
            }
        }
        Log.d(TAG, "=== END DEBUG ===");
    }

    private void filterApartmentsOnMap() {
      Intent intent = new Intent(LocationMap.this, RoomDetailsActivity.class);


        if (myMap != null && !roomsList.isEmpty()) {
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

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        if (isLocationEnabled()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                if (currentLocation == null) {
                    getLastLocationWithTimeout();
                }

                startLocationUpdates();

            } else {
                requestLocationPermission();
            }
        } else {
            promptEnableLocation();
        }

        if (myMap != null && shouldRefreshRooms) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadRoomsOnMap();
                shouldRefreshRooms = false;
            }, 500);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");

        shouldRefreshRooms = true;

        if (locationCallback != null && fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        try {
            if (roomReceiver != null) {
                unregisterReceiver(roomReceiver);
            }
            if (gpsStatusReceiver != null) {
                unregisterReceiver(gpsStatusReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receivers: " + e.getMessage());
        }

        if (locationCallback != null && fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
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

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "User enabled location settings");
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location is required for accurate features", Toast.LENGTH_SHORT).show();
            }
        }
    }
}