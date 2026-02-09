package com.app.roomify;

import android.os.Bundle;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PostRoomActivity extends AppCompatActivity {

    private static final String TAG = "PostRoomActivity";
    private MapView mapView;
    private EditText etTitle, etDescription, etPrice;
    private TextView tvSelectedAddress;

    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_room);

        Log.d(TAG, "onCreate called");

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize OSMdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Initialize views
        mapView = findViewById(R.id.mapViewPost);
        etTitle = findViewById(R.id.etRoomTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        Button btnSubmit = findViewById(R.id.btnSubmitRoom);

        // Setup map
        setupMap();

        // Submit button listener
        btnSubmit.setOnClickListener(v -> {
            Log.d(TAG, "Submit button clicked");
            submitRoom();
        });

        // Add a test button for debugging
        Button testButton = new Button(this);
        testButton.setText("Test Direct Navigation");
        testButton.setOnClickListener(v -> {
            Log.d(TAG, "Test button clicked");
            // Test if RoomDetailsActivity can be opened directly
            Room testRoom = new Room();
            testRoom.setId("test-id");
            testRoom.setTitle("Test Room");
            testRoom.setPrice(1000);
            testRoom.setAddress("Test Address");
            testRoom.setLatitude(40.7128);
            testRoom.setLongitude(-74.0060);
            testRoom.setDescription("Test Description");
            navigateToRoomDetails(testRoom.getId());
        });
    }

    private void setupMap() {
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);

            // Set initial view
            GeoPoint defaultLocation = new GeoPoint(40.7128, -74.0060);
            mapView.getController().setCenter(defaultLocation);
            mapView.getController().setZoom(14.0);

            // Add map tap listener for location selection
            mapView.getOverlays().add(new org.osmdroid.views.overlay.Overlay() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                    Log.d(TAG, "Map tapped");
                    try {
                        GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels(
                                (int) e.getX(), (int) e.getY());

                        if (geoPoint != null) {
                            selectedLatitude = geoPoint.getLatitude();
                            selectedLongitude = geoPoint.getLongitude();
                            Log.d(TAG, "Location selected: " + selectedLatitude + ", " + selectedLongitude);

                            // Clear previous markers
                            mapView.getOverlays().removeIf(overlay ->
                                    overlay instanceof Marker &&
                                            "Selected Location".equals(((Marker) overlay).getTitle()));

                            // Add new marker
                            Marker locationMarker = new Marker(mapView);
                            locationMarker.setPosition(geoPoint);
                            locationMarker.setTitle("Selected Location");
                            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            mapView.getOverlays().add(locationMarker);
                            mapView.invalidate();

                            // Get address from coordinates
                            new GeocodeTask().execute(geoPoint);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error handling map tap", ex);
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up map", e);
            Toast.makeText(this, "Error setting up map", Toast.LENGTH_SHORT).show();
        }
    }

    private class GeocodeTask extends AsyncTask<GeoPoint, Void, String> {
        @Override
        protected String doInBackground(GeoPoint... geoPoints) {
            if (geoPoints.length == 0) return "";

            GeoPoint geoPoint = geoPoints[0];
            Geocoder geocoder = new Geocoder(PostRoomActivity.this, Locale.getDefault());

            try {
                List<Address> addresses = geocoder.getFromLocation(
                        geoPoint.getLatitude(),
                        geoPoint.getLongitude(),
                        1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder addressBuilder = new StringBuilder();

                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressBuilder.append(address.getAddressLine(i));
                        if (i < address.getMaxAddressLineIndex()) {
                            addressBuilder.append(", ");
                        }
                    }
                    return addressBuilder.toString();
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error", e);
            }
            return "Address not found";
        }

        @Override
        protected void onPostExecute(String address) {
            Log.d(TAG, "Address found: " + address);
            tvSelectedAddress.setText(address);
        }
    }

    private void submitRoom() {
        Log.d(TAG, "submitRoom() called");

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Price: " + priceStr);
        Log.d(TAG, "Lat/Lng: " + selectedLatitude + ", " + selectedLongitude);

        // Validation
        if (TextUtils.isEmpty(title)) {
            Log.d(TAG, "Title validation failed");
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            Log.d(TAG, "Description validation failed");
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(priceStr)) {
            Log.d(TAG, "Price validation failed");
            etPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }

        if (selectedLatitude == 0.0 || selectedLongitude == 0.0) {
            Log.d(TAG, "Location validation failed");
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Log.d(TAG, "Price format error: " + e.getMessage());
            etPrice.setError("Invalid price");
            etPrice.requestFocus();
            return;
        }

        String currentUser = auth.getCurrentUser() != null ?
                auth.getCurrentUser().getUid() : "anonymous";
        Log.d(TAG, "Current user: " + currentUser);

        // Create Room object
        Room room = new Room();
        String roomId = UUID.randomUUID().toString();
        room.setId(roomId);
        room.setTitle(title);
        room.setDescription(description);
        room.setPrice(price);
        room.setLatitude(selectedLatitude);
        room.setLongitude(selectedLongitude);
        room.setAddress(tvSelectedAddress.getText().toString());
        room.setPostedBy(currentUser);
        room.setAvailable(true);
        room.setCreatedAt(System.currentTimeMillis());



        // Add timestamp if your Room class has it

        Log.d(TAG, "Saving room to Firebase...");

        // Save to Firebase
        db.collection("rooms")
                .document(roomId)
                .set(room)
                .addOnSuccessListener(aVoid -> {

                    Log.d(TAG, "Room saved successfully to Firebase");

                    Toast.makeText(
                            PostRoomActivity.this,
                            "Room posted successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    // ✅ Navigate using ONLY roomId
                    navigateToRoomDetails(roomId);
                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Failed to save room to Firebase", e);

                    Toast.makeText(
                            PostRoomActivity.this,
                            "Failed to post room. Try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });


        Log.d(TAG, "Firebase save operation initiated");
    }

    private void navigateToRoomDetails(String roomId) {
        Log.d(TAG, "Navigating to RoomDetailsActivity");
        Log.d(TAG, "Room ID: " + roomId);

        Intent intent = new Intent(PostRoomActivity.this, RoomDetailsActivity.class);
        intent.putExtra("room_id", roomId);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            try {
                mapView.onResume();
            } catch (Exception e) {
                Log.e(TAG, "Error in map onResume", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            try {
                mapView.onPause();
            } catch (Exception e) {
                Log.e(TAG, "Error in map onPause", e);
            }
        }
    }
}