
package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

    private double roomLat = 0, roomLng = 0;
    private String roomId, ownerId;

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Get roomId safely
        roomId = getIntent().getStringExtra("room_id");
        Log.d("ROOM_DETAILS_DEBUG", "Received room_id: " + roomId); // ✅ DEBUG

        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Room ID missing!", Toast.LENGTH_SHORT).show();
            finish(); // close activity safely
            return;
        }


        // Initialize views
        tvTitle = findViewById(R.id.tvRoomTitle);
        tvPrice = findViewById(R.id.tvRoomPrice);
        tvAddress = findViewById(R.id.tvRoomAddress);
        tvDescription = findViewById(R.id.tvRoomDescription);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnContactOwner = findViewById(R.id.btnCallOwner);

        // Firebase & location client
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load data
        loadRoomDetails();

        // Button listeners
        btnGetDirections.setOnClickListener(v -> openDirections());
        btnContactOwner.setOnClickListener(v -> contactRoomOwner());
    }

    private void loadRoomDetails() {
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Room no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Room room = doc.toObject(Room.class);
                    if (room == null) return;

                    tvTitle.setText(room.getTitle());
                    tvPrice.setText("$" + room.getPrice() + "/month");
                    tvAddress.setText(room.getAddress());
                    tvDescription.setText(room.getDescription());

                    roomLat = room.getLatitude();
                    roomLng = room.getLongitude();
                    ownerId = room.getPostedBy();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load room: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void openDirections() {
        if (roomLat == 0 || roomLng == 0) {
            Toast.makeText(this, "Room location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        String uri = "http://maps.google.com/maps?daddr=" +
                                roomLat + "," + roomLng +
                                "&saddr=" + location.getLatitude() + "," + location.getLongitude();

                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        mapIntent.setPackage("com.google.android.apps.maps");

                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
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
        if (ownerId == null || ownerId.isEmpty()) {
            Toast.makeText(this, "Owner info not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(ownerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Owner details not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String phone = doc.getString("phone");
                    String email = doc.getString("email");

                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } else if (email != null && !email.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Room: " + tvTitle.getText());
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Owner contact not available", Toast.LENGTH_SHORT).show();
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load owner info: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
