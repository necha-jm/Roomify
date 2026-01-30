package com.app.roomify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;

import java.util.ArrayList;
import java.util.List;

public class MainMapActivity extends AppCompatActivity {

    private static final String TAG = "MainMapActivity";
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Room> roomsList = new ArrayList<>();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FirebaseFirestore db;
    private boolean shouldRefreshRooms = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set default uncaught exception handler for debugging
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in thread: " + thread.getName(), throwable);
            throwable.printStackTrace();

            // Show error on UI thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(MainMapActivity.this,
                        "App crashed: " + throwable.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
        });

        try {
            // Initialize OSMdroid configuration BEFORE setContentView
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
            Configuration.getInstance().setUserAgentValue(getPackageName());

            // Initialize Firebase if not already initialized
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(this);
                }
                db = FirebaseFirestore.getInstance();
            } catch (Exception e) {
                Log.e(TAG, "Firebase initialization error", e);
                db = FirebaseFirestore.getInstance(); // Try again
            }

            setContentView(R.layout.activity_main_map);

            // Initialize views
            initializeViews();

            // Setup map
            setupMap();

            // Check and request location permission
            checkLocationPermission();

            // Load rooms from Firebase
            loadRoomsFromFirebase();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Try to recover by showing basic layout
            try {
                setContentView(R.layout.activity_main_map);
                Toast.makeText(this, "App loaded in safe mode", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Log.e(TAG, "Complete failure", ex);
                finish();
            }
        }
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        if (mapView == null) {
            throw new RuntimeException("MapView not found in layout. Check activity_main_map.xml");
        }

        Button btnAddRoom = findViewById(R.id.btnAddRoom);
        Button btnMyLocation = findViewById(R.id.btnMyLocation);

        if (btnAddRoom == null || btnMyLocation == null) {
            throw new RuntimeException("Buttons not found in layout. Check activity_main_map.xml");
        }

        // Setup click listeners
        btnAddRoom.setOnClickListener(v -> {
            Intent intent = new Intent(MainMapActivity.this, PostRoomActivity.class);
            startActivity(intent);
        });

        btnMyLocation.setOnClickListener(v -> centerOnUserLocation());
    }

    private void setupMap() {
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);

            // Set initial view (New York as default)
            GeoPoint defaultLocation = new GeoPoint(40.7128, -74.0060);
            mapView.getController().setCenter(defaultLocation);
            mapView.getController().setZoom(12.0);

            // Add compass
            CompassOverlay compassOverlay = new CompassOverlay(this, mapView);
            compassOverlay.enableCompass();
            mapView.getOverlays().add(compassOverlay);

            // Initialize location client
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up map", e);
            Toast.makeText(this, "Error setting up map", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRoomsFromFirebase() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        try {
            db.collection("rooms")
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            roomsList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Room room = document.toObject(Room.class);
                                    if (room != null) {
                                        room.setId(document.getId());
                                        roomsList.add(room);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing room document: " + e.getMessage());
                                }
                            }
                            addRoomMarkersToMap();
                        } else {
                            Log.e(TAG, "Failed to load rooms: " + (task.getException() != null ?
                                    task.getException().getMessage() : "Unknown error"));
                            Toast.makeText(MainMapActivity.this,
                                    "Failed to load rooms: " + (task.getException() != null ?
                                            task.getException().getLocalizedMessage() : "Network error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase error: ", e);
                        Toast.makeText(MainMapActivity.this,
                                "Connection error: " + e.getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadRoomsFromFirebase", e);
            Toast.makeText(this, "Error loading rooms", Toast.LENGTH_SHORT).show();
        }
    }

    private void addRoomMarkersToMap() {
        if (mapView == null || roomsList.isEmpty()) {
            return;
        }

        // Clear existing markers (keep compass overlay)
        List<org.osmdroid.views.overlay.Overlay> overlaysToRemove = new ArrayList<>();
        for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                overlaysToRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(overlaysToRemove);

        for (Room room : roomsList) {
            try {
                // Validate room coordinates
                if (room.getLatitude() == 0.0 && room.getLongitude() == 0.0) {
                    Log.w(TAG, "Room has invalid coordinates: " + room.getTitle());
                    continue;
                }

                GeoPoint roomLocation = new GeoPoint(room.getLatitude(), room.getLongitude());
                Marker roomMarker = new Marker(mapView);
                roomMarker.setPosition(roomLocation);
                roomMarker.setTitle(room.getTitle());
                roomMarker.setSnippet("$" + room.getPrice() + "/month\n" + room.getAddress());

                // Set marker icon
                try {
                    int iconRes = room.isAvailable() ?
                            R.drawable.ic_room_available : R.drawable.ic_room_occupied;
                    roomMarker.setIcon(ContextCompat.getDrawable(this, iconRes));
                } catch (Exception e) {
                    Log.e(TAG, "Error setting marker icon", e);
                }

                // Set anchor point
                roomMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Set click listener
                roomMarker.setOnMarkerClickListener((marker, mapView) -> {
                    showRoomDetails(room);
                    return true;
                });

                mapView.getOverlays().add(roomMarker);
            } catch (Exception e) {
                Log.e(TAG, "Error adding marker for room: " + room.getTitle(), e);
            }
        }

        // Refresh map display
        mapView.invalidate();
    }

    private void showRoomDetails(Room room) {
        try {
            Intent intent = new Intent(this, RoomDetailsActivity.class);
            intent.putExtra("room_id", room.getId());
            intent.putExtra("room_title", room.getTitle());
            intent.putExtra("room_price", room.getPrice());
            intent.putExtra("room_address", room.getAddress());
            intent.putExtra("room_lat", room.getLatitude());
            intent.putExtra("room_lng", room.getLongitude());
            intent.putExtra("room_description", room.getDescription());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error showing room details", e);
            Toast.makeText(this, "Error opening room details", Toast.LENGTH_SHORT).show();
        }
    }

    private void centerOnUserLocation() {
        if (!checkLocationPermission()) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            requestLocationPermission();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().animateTo(userLocation);
                            mapView.getController().setZoom(15.0);

                            // Add user location marker
                            try {
                                Marker userMarker = new Marker(mapView);
                                userMarker.setPosition(userLocation);
                                userMarker.setTitle("Your Location");
                                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                                int iconRes = R.drawable.ic_user_location;
                                if (iconRes != 0) {
                                    userMarker.setIcon(ContextCompat.getDrawable(this, iconRes));
                                }

                                // Remove previous user markers
                                List<org.osmdroid.views.overlay.Overlay> overlaysToRemove = new ArrayList<>();
                                for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
                                    if (overlay instanceof Marker) {
                                        Marker marker = (Marker) overlay;
                                        if ("Your Location".equals(marker.getTitle())) {
                                            overlaysToRemove.add(overlay);
                                        }
                                    }
                                }
                                mapView.getOverlays().removeAll(overlaysToRemove);

                                mapView.getOverlays().add(userMarker);
                                mapView.invalidate();
                            } catch (Exception e) {
                                Log.e(TAG, "Error adding user location marker", e);
                            }
                        } else {
                            Toast.makeText(this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting location", e);
                        Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in centerOnUserLocation", e);
            Toast.makeText(this, "Location service error", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, center on user location
                new Handler().postDelayed(() -> centerOnUserLocation(), 500);
            } else {
                Toast.makeText(this,
                        "Location permission denied. Some features may not work properly.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (mapView != null) {
                mapView.onResume();
            }

            // Refresh rooms only if needed (e.g., coming back from PostRoomActivity)
            if (shouldRefreshRooms) {
                new Handler().postDelayed(() -> loadRoomsFromFirebase(), 1000);
                shouldRefreshRooms = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mapView != null) {
                mapView.onPause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mapView != null) {
                mapView.onDetach();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    public void setShouldRefreshRooms(boolean shouldRefresh) {
        this.shouldRefreshRooms = shouldRefresh;
    }
}