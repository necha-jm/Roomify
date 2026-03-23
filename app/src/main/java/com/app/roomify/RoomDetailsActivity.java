package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RoomDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RoomDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;

    private TextView tvTitle, tvPrice, tvAddress, tvDescription,tvBedrooms,tvPostedDate;
    private MaterialButton btnGetDirections, btnContactOwner;

    private double roomLat = 0, roomLng = 0;
    private String roomId, ownerId, ownerPhone, ownerEmail, roomAddress;

    private FusedLocationProviderClient fusedLocationClient;

    private RecyclerView Amenities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Get roomId safely
        roomId = getIntent().getStringExtra("room_id");
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Room ID missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tvTitle = findViewById(R.id.tvRoomTitle);
        tvPrice = findViewById(R.id.tvRoomPrice);
        tvAddress = findViewById(R.id.tvRoomAddress);
        tvDescription = findViewById(R.id.tvRoomDescription);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnContactOwner = findViewById(R.id.btnCallOwner);
        tvBedrooms = findViewById(R.id.tvBedrooms);
        tvPostedDate = findViewById(R.id.tvPostedDate);


        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load room details
        loadRoomDetails();

        // Button listeners
        btnGetDirections.setOnClickListener(v -> openDirections());
        btnContactOwner.setOnClickListener(v -> contactRoomOwner());
    }

    private void loadRoomDetails() {
        FirebaseUtils.getRoom(roomId, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, "Failed to load room", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Firestore room load failed", task.getException());
                return;
            }

            DocumentSnapshot doc = task.getResult();
            Room room = doc.toObject(Room.class);
            if (room == null) {
                Toast.makeText(this, "Room data not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set UI
            tvTitle.setText(room.getTitle() != null ? room.getTitle() : "No title");
            tvPrice.setText("$" + room.getPrice() + "/month");
            roomAddress = room.getAddress();
            tvAddress.setText(roomAddress != null ? roomAddress : "Address not available");
            tvDescription.setText(room.getDescription() != null ? room.getDescription() : "");

            // Save location
            roomLat = room.getLatitude();
            roomLng = room.getLongitude();
            ownerId = room.getPostedBy();
            ownerPhone = room.getContactPhone();
            ownerEmail = room.getContactEmail();

            // If lat/lng missing, try geocoding
            if ((roomLat == 0 || roomLng == 0) && roomAddress != null) {
                geocodeAddress(roomAddress);
            }
        });
    }

    private void geocodeAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                roomLat = location.getLatitude();
                roomLng = location.getLongitude();
                Log.d(TAG, "Geocoded address to: " + roomLat + ", " + roomLng);
            } else {
                Toast.makeText(this, "Could not find location for this address", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
        }
    }

    private void openDirections() {
        if (roomLat == 0 || roomLng == 0) {
            Toast.makeText(this, "Room location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double originLat = 0, originLng = 0;
            if (location != null) {
                originLat = location.getLatitude();
                originLng = location.getLongitude();
            }

            // Construct Google Maps directions URI
            String uri = "https://www.google.com/maps/dir/?api=1" +
                    "&origin=" + originLat + "," + originLng +
                    "&destination=" + roomLat + "," + roomLng +
                    "&travelmode=walking";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // fallback to browser
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });
    }

    private void contactRoomOwner() {
        if (ownerPhone != null && !ownerPhone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + ownerPhone));
            startActivity(intent);
        } else if (ownerEmail != null && !ownerEmail.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + ownerEmail));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Room: " + tvTitle.getText());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Owner contact not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openDirections();
            } else {
                Toast.makeText(this, "Location permission required for directions", Toast.LENGTH_SHORT).show();
            }
        }
    }
}