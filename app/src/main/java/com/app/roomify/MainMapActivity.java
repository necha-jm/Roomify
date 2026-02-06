package com.app.roomify;

import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final GeoPoint DEFAULT_LOCATION = new GeoPoint(40.7128, -74.0060);

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Room> roomsList = new ArrayList<>();
    private FirebaseFirestore db;
    private boolean shouldRefreshRooms = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize configurations
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main_map);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views and map
        initViews();
        setupMap();

        // Check location permission and load data
        if (checkLocationPermission()) {
            loadRoomsFromFirebase();
        } else {
            requestLocationPermission();
        }
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);

        findViewById(R.id.btnAddRoom).setOnClickListener(v ->
                startActivity(new Intent(this, PostRoomActivity.class)));

        findViewById(R.id.btnMyLocation).setOnClickListener(v ->
                centerOnUserLocation());
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Add compass
        CompassOverlay compassOverlay = new CompassOverlay(this, mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // Set default view
        mapView.getController().setCenter(DEFAULT_LOCATION);
        mapView.getController().setZoom(12.0);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void loadRoomsFromFirebase() {
        db.collection("rooms")
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    roomsList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Room room = document.toObject(Room.class);
                        room.setId(document.getId());
                        roomsList.add(room);
                    }
                    addRoomMarkersToMap();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load rooms", e);
                    Toast.makeText(this, "Failed to load rooms", Toast.LENGTH_SHORT).show();
                });
    }

    private void addRoomMarkersToMap() {
        // Clear existing markers (except compass)
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker
                && !"Your Location".equals(((Marker) overlay).getTitle()));

        for (Room room : roomsList) {
            if (room.getLatitude() == 0.0 && room.getLongitude() == 0.0) continue;

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(room.getLatitude(), room.getLongitude()));
            marker.setTitle(room.getTitle());
            marker.setSnippet("$" + room.getPrice() + "/month\n" + room.getAddress());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(ContextCompat.getDrawable(this,
                    room.isAvailable() ? R.drawable.ic_room_available : R.drawable.ic_room_occupied));
            marker.setOnMarkerClickListener((m, mv) -> {
                showRoomDetails(room);
                return true;
            });

            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    private void showRoomDetails(Room room) {
        Intent intent = new Intent(this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        intent.putExtra("room_title", room.getTitle());
        intent.putExtra("room_price", room.getPrice());
        intent.putExtra("room_address", room.getAddress());
        intent.putExtra("room_lat", room.getLatitude());
        intent.putExtra("room_lng", room.getLongitude());
        intent.putExtra("room_description", room.getDescription());
        startActivity(intent);
    }

    private void centerOnUserLocation() {
        if (!checkLocationPermission()) {
            requestLocationPermission();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().animateTo(userLocation);
                            mapView.getController().setZoom(15.0);
                            addUserMarker(userLocation);
                        } else {
                            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting location", e);
                        Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
        }
    }

    private void addUserMarker(GeoPoint location) {
        // Remove previous user markers
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker
                && "Your Location".equals(((Marker) overlay).getTitle()));

        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(location);
        userMarker.setTitle("Your Location");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_user_location));

        mapView.getOverlays().add(userMarker);
        mapView.invalidate();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadRoomsFromFirebase();
                centerOnUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (shouldRefreshRooms) {
            loadRoomsFromFirebase();
            shouldRefreshRooms = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    public void setShouldRefreshRooms(boolean shouldRefresh) {
        this.shouldRefreshRooms = shouldRefresh;
    }
}