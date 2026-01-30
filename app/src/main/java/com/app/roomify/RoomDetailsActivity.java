package com.app.roomify;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoomDetailsActivity extends AppCompatActivity {

    private TextView tvTitle, tvPrice, tvAddress, tvDescription;
    private MaterialButton btnGetDirections, btnContactOwner;

    private double roomLat, roomLng;
    private String roomId, ownerId;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Get room data from intent
        Intent intent = getIntent();
        roomId = intent.getStringExtra("room_id");
        String roomTitle = intent.getStringExtra("room_title");
        double roomPrice = intent.getDoubleExtra("room_price", 0);
        String roomAddress = intent.getStringExtra("room_address");
        roomLat = intent.getDoubleExtra("room_lat", 0);
        roomLng = intent.getDoubleExtra("room_lng", 0);
        String roomDescription = intent.getStringExtra("room_description");

        // Initialize views
        tvTitle = findViewById(R.id.tvRoomTitle);
        tvPrice = findViewById(R.id.tvRoomPrice);
        tvAddress = findViewById(R.id.tvRoomAddress);
        tvDescription = findViewById(R.id.tvRoomDescription);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnContactOwner = findViewById(R.id.btnCallOwner);

        // Set room details
        tvTitle.setText(roomTitle);
        tvPrice.setText("$" + roomPrice + "/month");
        tvAddress.setText(roomAddress);
        tvDescription.setText(roomDescription);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        // Load owner details
        loadOwnerDetails();

        // Button listeners
        btnGetDirections.setOnClickListener(v -> openDirections());

        btnContactOwner.setOnClickListener(v -> contactRoomOwner());
    }

    private void loadOwnerDetails() {
        if (roomId != null) {
            db.collection("rooms").document(roomId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Room room = documentSnapshot.toObject(Room.class);
                            if (room != null) {
                                ownerId = room.getPostedBy();
                            }
                        }
                    });
        }
    }

    private void openDirections() {
        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
            return;
        }

        // Get user's current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Open in Google Maps app
                        String uri = "http://maps.google.com/maps?daddr=" +
                                roomLat + "," + roomLng +
                                "&saddr=" + location.getLatitude() + "," + location.getLongitude();

                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        mapIntent.setPackage("com.google.android.apps.maps");

                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
                            // Fallback: Open in browser
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                                            roomLat + "," + roomLng));
                            startActivity(browserIntent);
                        }
                    } else {
                        Toast.makeText(this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void contactRoomOwner() {
        if (ownerId != null) {
            // Fetch owner contact details from users collection
            db.collection("users").document(ownerId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String phone = documentSnapshot.getString("phone");
                            String email = documentSnapshot.getString("email");

                            // Open contact dialog or start call/email
                            if (phone != null && !phone.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + phone));
                                startActivity(intent);
                            } else if (email != null && !email.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_SENDTO);
                                intent.setData(Uri.parse("mailto:" + email));
                                intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Room: " + tvTitle.getText());
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Owner contact not available", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "Owner information not found", Toast.LENGTH_SHORT).show();
        }
    }
}