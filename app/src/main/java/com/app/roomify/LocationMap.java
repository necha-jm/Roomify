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
import android.location.LocationRequest;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
    private static final String TAG = "LocationMap";

    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db;

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

    // Map markers
    private Marker currentLocationMarker;
    private Marker searchMarker;

    // IMPORTANT DATA HOLDERS
    private final List<Room> roomsList = new ArrayList<>();
    private final Map<Marker, Room> markerRoomMap = new HashMap<>();

    private boolean shouldRefreshRooms = true;
    private boolean isMapReady = false;

    private BroadcastReceiver roomReceiver;
    private static final String ACTION_NEW_ROOM = "com.app.roomify.NEW_ROOM_ADDED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.loadLocale(this);
        setContentView(R.layout.activity_location_map);


        Log.d(TAG, "onCreate started");

        //receiver registering
        RoomReceiver roomReceiver = new RoomReceiver();

        IntentFilter filter = new IntentFilter(RoomReceiver.ACTION_NEW_ROOM);
        ContextCompat.registerReceiver(this, roomReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

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
                        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14f));
                    } else {
                        getLastLocation();
                    }
                });
            }

            // Settings button - FIXED: Don't use Activity.class
            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> {
                    try {
                        // Replace with your actual SettingsActivity
                        // startActivity(new Intent(LocationMap.this, SettingsActivity.class));
                        Toast.makeText(this, "Settings will be implemented soon", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error with settings: " + e.getMessage());
                    }
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
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Geocoder service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(query, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(
                        address.getLatitude(),
                        address.getLongitude());

                if (myMap != null) {
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                    addSearchResultMarker(latLng, address.getAddressLine(0));
                }
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
            Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
        }
    }




    private void addSearchResultMarker(LatLng latLng, String title) {
        if (myMap == null) return;

        // Clear previous search marker
        if (searchMarker != null) {
            searchMarker.remove();
        }

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

    // REQUEST LOCATION PERMISSION
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_CODE
            );
        } else {
            getLastLocation();
        }
    }

    // GET USER LOCATION
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        initMap();
                    } else {
                        // Try to request new location
                        requestNewLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
                    initMap(); // Load map with default location
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

    @SuppressLint("MissingPermission")
    private void requestNewLocation() {
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "New location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        if (myMap != null) {
                            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14f));
                            if (currentLocationMarker != null) currentLocationMarker.remove();
                            currentLocationMarker = myMap.addMarker(new MarkerOptions()
                                    .position(myLocation)
                                    .title("My Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        }
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get new location: " + e.getMessage()));
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
                    // Still load map without location
                    loadMapWithoutLocation();
                })
                .show();
    }

    private void loadMapWithoutLocation() {
        // Load map with default location (Dar es Salaam)
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

    // MAP READY
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "Map is ready");
        myMap = googleMap;
        isMapReady = true;

        // Enable location layer if permission granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        // Set default location if currentLocation is null
        LatLng defaultLocation;
        if (currentLocation != null) {
            defaultLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            // Add custom user location marker
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }

            currentLocationMarker = myMap.addMarker(new MarkerOptions()
                    .position(defaultLocation)
                    .title("My Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            // Default to Dar es Salaam
            defaultLocation = new LatLng(-6.7924, 39.2083);
        }

        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14));

        // Load rooms
        loadRoomsOnMap();

        // MARKER CLICK → ROOM DETAILS
        myMap.setOnMarkerClickListener(marker -> {
            // Safely check marker equality
            boolean isCurrentLocation = currentLocationMarker != null &&
                    currentLocationMarker.equals(marker);
            boolean isSearchMarker = searchMarker != null &&
                    searchMarker.equals(marker);

            if (isCurrentLocation || isSearchMarker) {
                return false;
            }

            Room room = markerRoomMap.get(marker);
            if (room == null) return false;

            try {
                Intent intent = new Intent(LocationMap.this, RoomDetailsActivity.class);
                intent.putExtra("room_id", room.getId());
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting RoomDetailsActivity: " + e.getMessage());
            }
            return true;
        });
    }

    // Load rooms on map
    private void loadRoomsOnMap() {
        if (db == null) {
            Log.e(TAG, "Firestore db is null");
            return;
        }

        db.collection("rooms")
                .whereEqualTo("available", true)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null || myMap == null) {
                        return;
                    }

                    // Clear existing data
                    roomsList.clear();
                    markerRoomMap.clear();

                    // Clear only if map is ready
                    if (isMapReady) {
                        myMap.clear();

                        // Re-add user location marker
                        if (currentLocation != null) {
                            LatLng myLocation = new LatLng(
                                    currentLocation.getLatitude(),
                                    currentLocation.getLongitude());
                            currentLocationMarker = myMap.addMarker(new MarkerOptions()
                                    .position(myLocation)
                                    .title("My Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        }
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Room room = doc.toObject(Room.class);
                            room.setId(doc.getId());

                            if (room.getLatitude() == 0 || room.getLongitude() == 0) {
                                continue;
                            }

                            roomsList.add(room);

                            LatLng roomLocation = new LatLng(
                                    room.getLatitude(),
                                    room.getLongitude());

                            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_apartment);

                            // Resize (width, height in pixels)
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 40, 40, false);

                            Marker marker = myMap.addMarker(
                                    new MarkerOptions()
                                            .position(roomLocation)
                                            .title(room.getTitle())
                                            .snippet("Price: $" + room.getPrice())
                                            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap))

                            );

                            if (marker != null) {
                                markerRoomMap.put(marker, room);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing room document: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "Loaded " + roomsList.size() + " rooms");
                });
    }

    private void filterApartmentsOnMap() {
        Toast.makeText(this, "Showing all available apartments", Toast.LENGTH_SHORT).show();
    }

    // REFRESH MAP WHEN RETURNING
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        if (isLocationEnabled()) {
            requestLocationPermission();
        }

        if (myMap != null && shouldRefreshRooms) {
            loadRoomsOnMap();
            shouldRefreshRooms = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldRefreshRooms = true;
    }

    // PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_FINE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                // Still load map without location
                loadMapWithoutLocation();
            }
        }
    }
}