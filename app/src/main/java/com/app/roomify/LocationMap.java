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
import com.app.roomify.models.BookingResponse;  // ADDED: Import BookingResponse
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
            addRoomCard = findViewById(R.id.addRoom_card);
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
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        User user = tokenManager.getUser();

        if (user == null) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if ("owner".equals(user.getRole())) {
            startActivity(new Intent(this, OwnerDashboard.class));
        } else {
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

    private void setupSearchButton() {
        if (btnSearchAddress != null) {
            btnSearchAddress.setOnClickListener(v -> {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    Log.d(TAG, "Search button clicked for: " + query);
                    hideKeyboard();
                    performImprovedSearch(query);
                } else {
                    Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
                    performImprovedSearch(query);
                    hideKeyboard();
                } else {
                    Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
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
                        if (currentLocation != null && myMap != null) {
                            LatLng latLng  = new LatLng(
                                    currentLocation.getLatitude(),
                                    currentLocation.getLongitude()
                            );
                            animateToLocation(latLng, 16f);
                            Toast.makeText(this, "Returned to your location", Toast.LENGTH_SHORT).show();
                        }
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

    private void performImprovedSearch(String query) {

        if (query.isEmpty() || myMap == null) {
            Toast.makeText(this, "Cannot perform search", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 1);

                runOnUiThread(() -> {
                    showLoading(false);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        animateToLocation(latLng, 16f);
                        if (searchMarker == null) {
                            searchMarker = myMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(query)
                                    .snippet(getAddressString(address))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
                        } else {
                            searchMarker.setPosition(latLng);
                            searchMarker.setTitle(query);
                            searchMarker.setSnippet(getAddressString(address));
                        }

                        if (searchMarker != null) {
                            searchMarker.showInfoWindow();
                        }
                        Toast.makeText(this, "Found: " + query, Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(this, "Location not found. Please try a different address.", Toast.LENGTH_LONG).show();
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

        // Remove old marker
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        // Add custom marker
        currentLocationMarker = myMap.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title("my locatio")
                        .icon(getResizedMarker(R.drawable.ic_location, 80, 80))
        );

        // Add circle effect (modern look)
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

        // Load rooms from backend with offline support
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

    // ==================== OFFLINE SUPPORT METHODS ====================

    private void loadRoomsFromBackend() {
        if (apiInterface == null || myMap == null) {
            Log.d(TAG, "Cannot load rooms");
            return;
        }

        showLoading(true);

        // Try to load from server first
        Call<List<Room>> call = apiInterface.getAllRooms();
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Room> rooms = response.body();
                    if (rooms != null && !rooms.isEmpty()) {
                        roomsList.clear();
                        roomsList.addAll(rooms);

                        // Save to local database for offline use
                        saveRoomsToLocalDatabase(rooms);

                        // Fetch booking status
                        fetchBookingStatusForRooms(rooms);
                    } else {
                        showLoading(false);
                        Log.d(TAG, "No rooms available");
                        loadRoomsFromLocalDatabase();
                    }
                } else {
                    showLoading(false);
                    Log.e(TAG, "Failed to load rooms. Code: " + response.code());
                    loadRoomsFromLocalDatabase();
                    Toast.makeText(LocationMap.this, "Using cached data (offline mode)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading rooms: " + t.getMessage());
                loadRoomsFromLocalDatabase();
                Toast.makeText(LocationMap.this, "Using cached data (offline mode)", Toast.LENGTH_SHORT).show();
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

                        // For offline mode, clear booking status and show all as available
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

    // FIXED: Changed to use BookingResponse instead of BookingRequest
    private void fetchBookingStatusForRooms(List<Room> rooms) {
        if (currentUserId == null) {
            // User not logged in, just show all rooms as available
            showLoading(false);
            updateMapWithRooms(rooms);
            return;
        }

        final int[] processedCount = {0};
        final int totalRooms = rooms.size();

        if (totalRooms == 0) {
            showLoading(false);
            updateMapWithRooms(rooms);
            return;
        }

        // Clear previous status
        roomBookingStatus.clear();

        for (Room room : rooms) {
            // CHANGED: Use BookingResponse instead of BookingRequest
            Call<ApiResponse<List<BookingResponse>>> bookingCall = apiInterface.checkUserBooking(currentUserId, room.getId());
            final Long roomId = room.getId();

            bookingCall.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                       Response<ApiResponse<List<BookingResponse>>> response) {
                    processedCount[0]++;

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<BookingResponse> bookings = response.body().getData();
                        if (bookings != null && !bookings.isEmpty()) {
                            String status = bookings.get(0).getStatus();
                            roomBookingStatus.put(roomId, status);
                            Log.d(TAG, "Room " + roomId + " has booking status: " + status);
                        }
                    }

                    if (processedCount[0] == totalRooms) {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                    processedCount[0]++;
                    Log.e(TAG, "Failed to fetch booking status for room: " + roomId, t);

                    if (processedCount[0] == totalRooms) {
                        showLoading(false);
                        updateMapWithRooms(rooms);
                    }
                }
            });
        }
    }

    private void updateMapWithRooms(List<Room> rooms) {
        if (myMap == null) return;

        // Save current location
        LatLng currentLocLatLng = null;
        if (currentLocation != null) {
            currentLocLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }

        // Clear only room markers
        for (Marker marker : markerRoomMap.keySet()) {
            marker.remove();
        }

        roomsList.clear();
        markerRoomMap.clear();

        // Re-add current location marker
        if (currentLocLatLng != null) {
            if (currentLocationMarker == null) {
                currentLocationMarker = myMap.addMarker(
                        new MarkerOptions()
                                .position(currentLocLatLng)
                                .title("My Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                );
            } else {
                currentLocationMarker.setPosition(currentLocLatLng);
            }
        }

        // Add room markers with status-based colors
        for (Room room : rooms) {
            if (room.getLatitude() == 0 || room.getLongitude() == 0) continue;

            roomsList.add(room);
            LatLng roomLocation = new LatLng(room.getLatitude(), room.getLongitude());

            String status = roomBookingStatus.get(room.getId());
            float markerColor = BitmapDescriptorFactory.HUE_BLUE;
            String snippet = "Price: $" + room.getPrice();

            if (status != null) {
                switch (status.toUpperCase()) {
                    case "PENDING":
                        markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                        snippet = " Pending Request";
                        break;
                    case "ACCEPTED":
                    case "CONFIRMED":
                        markerColor = BitmapDescriptorFactory.HUE_GREEN;
                        snippet = "Booking Approved";
                        break;
                    case "REJECTED":
                        markerColor = BitmapDescriptorFactory.HUE_RED;
                        snippet = " Request Rejected";
                        break;
                    case "CANCELLED":
                        markerColor = BitmapDescriptorFactory.HUE_VIOLET;
                        snippet = "Cancelled";
                        break;
                    default:
                        markerColor = BitmapDescriptorFactory.HUE_BLUE;
                        snippet = "Available - $" + room.getPrice();
                        break;
                }
            } else {
                snippet = "Available - $" + room.getPrice();
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

        Log.d(TAG, "Updated map with " + roomsList.size() + " room markers");
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
        if (executorService == null
                || executorService.isShutdown()
                || executorService.isTerminated()) {
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

        // Refresh rooms when returning to map
        if (myMap != null) {
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