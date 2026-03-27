package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RoomDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;

    private TextView tvTitle, tvPrice, tvAddress, tvDescription,tvBedrooms,tvPostedDate;
    private MaterialButton btnGetDirections, btnContactOwner;

    private double roomLat = 0, roomLng = 0;
    private String roomId, ownerId, ownerPhone, ownerEmail, roomAddress;

    private FusedLocationProviderClient fusedLocationClient;

    private Button btnBookNow;

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
        btnBookNow = findViewById(R.id.btnBookNow);


        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load room details
        loadRoomDetails();

        // Button listeners
        btnGetDirections.setOnClickListener(v -> openDirections());
        btnContactOwner.setOnClickListener(v -> contactRoomOwner());
        btnBookNow.setOnClickListener(v -> requestRoomBooking());
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
            tvBedrooms.setText(String.valueOf(room.getRoomsCount()));

            long timestamp = room.getCreatedAt();
            String formattedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(timestamp));
            tvPostedDate.setText(formattedDate);

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

    private void requestRoomBooking() {
        // Get current user ID
        String userId = FirebaseUtils.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please login to request booking", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        Toast.makeText(this, "Sending booking request...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting booking request for room: " + roomId);

        // Get current user's name
        FirebaseUtils.getCurrentUserName(userName -> {
            if (userName == null) {
                userName = "Unknown User";
                Log.w(TAG, "User name not found, using default");
            }

            // Get current user's phone
            String finalUserName = userName;
            FirebaseUtils.getCurrentUserPhone(userPhone -> {
                if (userPhone == null) {
                    userPhone = "Not provided";
                    Log.w(TAG, "User phone not found, using default");
                }

                String finalUserPhone = userPhone;

                // Get room title first (important for display)
                FirebaseUtils.getRoom(roomId, roomTask -> {
                    if (!roomTask.isSuccessful() || roomTask.getResult() == null) {
                        Log.e(TAG, "Failed to get room title");
                        Toast.makeText(this, "Error getting room details", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Room room = roomTask.getResult().toObject(Room.class);
                    String roomTitle = room != null && room.getTitle() != null ? room.getTitle() : "Unknown Room";

                    // Create formatted date
                    String formattedDate = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                            .format(new Date());

                    // Generate a unique booking ID (using timestamp + userId + roomId)
                    final String bookingId = System.currentTimeMillis() + "_" + userId + "_" + roomId;

                    // Prepare complete booking data
                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("id", bookingId);
                    bookingData.put("userId", userId);
                    bookingData.put("userName", finalUserName);
                    bookingData.put("userPhone", finalUserPhone);
                    bookingData.put("roomId", roomId);
                    bookingData.put("roomTitle", roomTitle);
                    bookingData.put("status", "pending");
                    bookingData.put("timestamp", System.currentTimeMillis());
                    bookingData.put("bookingDate", formattedDate);

                    Log.d(TAG, "Saving booking data with ID: " + bookingId);
                    Log.d(TAG, "Booking data: " + bookingData.toString());

                    // Save booking to BOTH locations

                    // 1. Save to ROOM's bookings subcollection
                    FirebaseUtils.getRoomBookingsCollection(roomId)
                            .document(bookingId)
                            .set(bookingData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Booking saved to ROOM: rooms/" + roomId + "/bookings/" + bookingId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to save booking to ROOM", e);
                            });

                    // 2. Save to USER's bookings subcollection
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("bookings")
                            .document(bookingId)
                            .set(bookingData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Booking saved to USER: users/" + userId + "/bookings/" + bookingId);

                                Toast.makeText(RoomDetailsActivity.this,
                                        "Booking request sent successfully!",
                                        Toast.LENGTH_LONG).show();

                                // Notify room owner
                                sendOwnerNotification(bookingId, finalUserName, roomTitle);

                                // Optionally disable book button to prevent duplicate
                                btnBookNow.setEnabled(false);
                                btnBookNow.setText("Request Sent");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to save booking to USER", e);
                                Log.e(TAG, "Error message: " + e.getMessage());
                                Toast.makeText(RoomDetailsActivity.this,
                                        "Failed to save booking: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
            });
        });
    }

    // Updated sendOwnerNotification with room title
    private void sendOwnerNotification(String bookingId, String userName, String roomTitle) {
        String notificationTitle = "New Booking Request";
        String notificationMessage = userName + " requested to book: " + roomTitle;

        Log.d(TAG, "Sending notification to owner: " + ownerId);
        FirebaseUtils.sendNotificationToUser(ownerId, notificationTitle, notificationMessage, bookingId);
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