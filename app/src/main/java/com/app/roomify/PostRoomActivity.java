package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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

    private MapView mapView;
    private EditText etTitle, etDescription, etPrice;
    private TextView tvSelectedAddress;
    private Button btnSubmit;

    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_room);

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
        btnSubmit = findViewById(R.id.btnSubmitRoom);

        // Setup map
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
                GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels(
                        (int) e.getX(), (int) e.getY());

                if (geoPoint != null) {
                    selectedLatitude = geoPoint.getLatitude();
                    selectedLongitude = geoPoint.getLongitude();

                    // Clear previous markers
                    mapView.getOverlays().removeIf(overlay ->
                            overlay instanceof Marker &&
                                    ((Marker) overlay).getTitle().equals("Selected Location"));

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
                return true;
            }
        });

        // Submit button listener
        btnSubmit.setOnClickListener(v -> submitRoom());
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
                e.printStackTrace();
            }
            return "Address not found";
        }

        @Override
        protected void onPostExecute(String address) {
            tvSelectedAddress.setText(address);
        }
    }

    private void submitRoom() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
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

        if (selectedLatitude == 0.0 || selectedLongitude == 0.0) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPrice.setError("Invalid price");
            return;
        }

        String currentUser = auth.getCurrentUser() != null ?
                auth.getCurrentUser().getUid() : "anonymous";

        // Create Room object
        Room room = new Room();
        room.setId(UUID.randomUUID().toString());
        room.setTitle(title);
        room.setDescription(description);
        room.setPrice(price);
        room.setLatitude(selectedLatitude);
        room.setLongitude(selectedLongitude);
        room.setAddress(tvSelectedAddress.getText().toString());
        room.setPostedBy(currentUser);
        room.setAvailable(true);

        // Save to Firebase
        db.collection("rooms")
                .document(room.getId())
                .set(room)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PostRoomActivity.this,
                            "Room posted successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to map
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PostRoomActivity.this,
                            "Failed to post room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}