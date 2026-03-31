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
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class RoomDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RoomDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;

    private TextView tvTitle, tvPrice, tvAddress, tvDescription, tvBedrooms, tvPostedDate;
    private MaterialButton btnGetDirections, btnContactOwner;
    private Button btnBookNow;

    private double roomLat = 0, roomLng = 0;
    private String roomId;
    private Room currentRoom;
    private boolean alreadyRequested = false;
    private boolean isRoomLoaded = false;  // ✅ ADD THIS LINE
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Get roomId
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
        tvBedrooms = findViewById(R.id.tvBedrooms);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnContactOwner = findViewById(R.id.btnCallOwner);
        btnBookNow = findViewById(R.id.btnBookNow);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Disable book button while loading
        btnBookNow.setEnabled(false);
        btnBookNow.setText("Loading...");

        // Load room details
        loadRoomDetails();

        // Button listeners
        btnGetDirections.setOnClickListener(v -> openDirections());
        btnContactOwner.setOnClickListener(v -> contactRoomOwner());
        btnBookNow.setOnClickListener(v -> {
            if (alreadyRequested) {
                Toast.makeText(this, "You already requested this room", Toast.LENGTH_SHORT).show();
            } else {
                requestRoomBooking();
            }
        });

        checkIfAlreadyRequested();
    }

    private void loadRoomDetails() {
        FirebaseUtils.getRoom(roomId, task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, "Failed to load room", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Firestore room load failed", task.getException());
                btnBookNow.setEnabled(false);
                btnBookNow.setText("Error Loading");
                return;
            }

            DocumentSnapshot doc = task.getResult();
            Log.d(TAG, "Room document data: " + doc.getData());

            // Log all fields for debugging
            Log.d(TAG, "All document fields: " + doc.getData().keySet());

            // Check specifically for postedBy field
            if (doc.contains("postedBy")) {
                Log.d(TAG, "postedBy field exists with value: " + doc.getString("postedBy"));
            } else {
                Log.e(TAG, "postedBy field MISSING from Firestore document!");
            }

            Room room = doc.toObject(Room.class);
            if (room == null) {
                Toast.makeText(this, "Room data not found", Toast.LENGTH_SHORT).show();
                btnBookNow.setEnabled(false);
                btnBookNow.setText("Data Error");
                return;
            }

            currentRoom = room;
            isRoomLoaded = true;  // ✅ SET LOADED FLAG
            roomLat = room.getLatitude();
            roomLng = room.getLongitude();

            tvTitle.setText(room.getTitle());
            tvPrice.setText("$" + room.getPrice() + "/month");
            tvAddress.setText(room.getAddress());
            tvDescription.setText(room.getDescription());
            tvBedrooms.setText(String.valueOf(room.getRoomsCount()));

            long timestamp = room.getCreatedAt();
            String formattedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(timestamp));
            tvPostedDate.setText(formattedDate);

            // Geocode if coordinates missing
            if ((roomLat == 0 || roomLng == 0) && !room.getAddress().isEmpty()) {
                geocodeAddress(room.getAddress());
            }

            // Log room details for debugging
            Log.d(TAG, "Room ownerId=" + room.getPostedBy() +
                    ", phone=" + room.getContactPhone() +
                    ", email=" + room.getContactEmail());

            // Enable book button only if owner info exists and not already requested
            String ownerId = room.getPostedBy();
            if (ownerId != null && !ownerId.isEmpty()) {
                if (!alreadyRequested) {
                    btnBookNow.setEnabled(true);
                    btnBookNow.setText("Book Now");
                } else {
                    btnBookNow.setEnabled(false);
                    btnBookNow.setText("Already Requested");
                }
            } else {
                btnBookNow.setEnabled(false);
                btnBookNow.setText("Owner Info Missing");
                Log.e(TAG, "Room owner info is missing or empty for roomId: " + roomId);
            }
        });
    }

    private void checkIfAlreadyRequested() {
        String userId = FirebaseUtils.getCurrentUserId();
        if (userId == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("bookings")
                .whereEqualTo("roomId", roomId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        alreadyRequested = true;
                        btnBookNow.setEnabled(false);
                        btnBookNow.setText("Already Requested");
                    } else {
                        // If room is loaded and not requested, enable button
                        if (isRoomLoaded && currentRoom != null) {
                            String ownerId = currentRoom.getPostedBy();
                            if (ownerId != null && !ownerId.isEmpty()) {
                                btnBookNow.setEnabled(true);
                                btnBookNow.setText("Book Now");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing bookings", e);
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
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
        }
    }

    private void requestRoomBooking() {
        // Check if room is loaded
        if (!isRoomLoaded || currentRoom == null) {
            Toast.makeText(this, "Loading room info, please wait...", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Room not loaded yet when trying to book");
            return;
        }

        // Get owner ID with null safety
        String ownerId = currentRoom.getPostedBy();
        if (ownerId == null || ownerId.trim().isEmpty()) {
            String errorMsg = "Cannot request booking: owner info missing";
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            Log.e(TAG, errorMsg + " - Room ID: " + currentRoom.getId() +
                    ", PostedBy: '" + ownerId + "'");

            // Log full room details for debugging
            Log.e(TAG, "Full room data: " +
                    "Title=" + currentRoom.getTitle() +
                    ", Address=" + currentRoom.getAddress() +
                    ", Owner=" + ownerId +
                    ", Phone=" + currentRoom.getContactPhone() +
                    ", Email=" + currentRoom.getContactEmail());

            // Disable book button since owner info is missing
            btnBookNow.setEnabled(false);
            btnBookNow.setText("Owner Info Missing");
            return;
        }

        String currentUserId = FirebaseUtils.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "You must be logged in to request a room", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is trying to book their own room
        if (ownerId.equals(currentUserId)) {
            Toast.makeText(this, "You cannot request your own room", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button during booking process
        btnBookNow.setEnabled(false);
        btnBookNow.setText("Sending Request...");

        // First, fetch current user details
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String userName = userDoc.getString("name");
                    if (userName == null || userName.isEmpty()) {
                        userName = userDoc.getString("fullName");
                    }
                    if (userName == null || userName.isEmpty()) {
                        userName = "User";
                    }

                    String userPhone = userDoc.getString("phone");
                    if (userPhone == null || userPhone.isEmpty()) {
                        userPhone = userDoc.getString("contactPhone");
                    }
                    if (userPhone == null) {
                        userPhone = "";
                    }

                    // Create booking data with all required fields
                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("userId", currentUserId);
                    bookingData.put("userName", userName);
                    bookingData.put("userPhone", userPhone);
                    bookingData.put("roomId", currentRoom.getId());
                    bookingData.put("roomTitle", currentRoom.getTitle());
                    bookingData.put("ownerId", ownerId);
                    bookingData.put("timestamp", System.currentTimeMillis());
                    bookingData.put("status", "pending");
                    bookingData.put("bookingDate", new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(new Date()));

                    Log.d(TAG, "Creating booking with data: " + bookingData);

                    // Save to ROOM's bookings subcollection first
                    String finalUserName = userName;
                    FirebaseUtils.getRoomBookingsCollection(currentRoom.getId())
                            .add(bookingData)
                            .addOnSuccessListener(docRef -> {
                                String bookingId = docRef.getId();
                                Log.d(TAG, "Booking saved to room with ID: " + bookingId);

                                // Save to USER's bookings subcollection with same ID
                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(currentUserId)
                                        .collection("bookings")
                                        .document(bookingId)
                                        .set(bookingData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Booking request sent successfully", Toast.LENGTH_SHORT).show();
                                            Log.d(TAG, "Booking saved to user with ID: " + bookingId);

                                            // Send notification to owner
                                            FirebaseUtils.sendNotificationToUser(
                                                    ownerId,
                                                    "New Booking Request",
                                                    finalUserName + " wants to book " + currentRoom.getTitle(),
                                                    bookingId
                                            );

                                            alreadyRequested = true;
                                            btnBookNow.setEnabled(false);
                                            btnBookNow.setText("Already Requested");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to save to user bookings", e);
                                            Toast.makeText(this, "Failed to save booking", Toast.LENGTH_SHORT).show();
                                            btnBookNow.setEnabled(true);
                                            btnBookNow.setText("Book Now");
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to send booking request", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Booking error", e);
                                btnBookNow.setEnabled(true);
                                btnBookNow.setText("Book Now");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user details", e);
                    Toast.makeText(this, "Failed to get user details", Toast.LENGTH_SHORT).show();
                    btnBookNow.setEnabled(true);
                    btnBookNow.setText("Book Now");
                });
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
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double originLat = 0, originLng = 0;
            if (location != null) {
                originLat = location.getLatitude();
                originLng = location.getLongitude();
            }

            String uri = "https://www.google.com/maps/dir/?api=1" +
                    "&origin=" + originLat + "," + originLng +
                    "&destination=" + roomLat + "," + roomLng +
                    "&travelmode=walking";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });
    }

    private void contactRoomOwner() {
        if (currentRoom == null) {
            Toast.makeText(this, "Room info not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = currentRoom.getContactPhone();
        String email = currentRoom.getContactEmail();

        if (phone != null && !phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No calling app available", Toast.LENGTH_SHORT).show();
            }
        } else if (email != null && !email.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Room: " + currentRoom.getTitle());
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No email app available", Toast.LENGTH_SHORT).show();
            }
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