package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OwnerDashboard extends AppCompatActivity {

    // Views
    private TextView tvOwnerName, tvTotalProperties, tvTotalBookings, tvTotalEarnings;
    private TextView tvThisMonthEarnings, tvLastMonthEarnings, tvViewAllRequests;
    private MaterialCardView cardAddProperty, cardMyProperties, cardBookings, cardAnalytics;
    private RecyclerView rvPendingRequests, rvProperties;
    private BottomNavigationView bottomNavigation;
    private ImageView ivNotifications, ivSettings;
    private View loadingOverlay;
    private ProgressBar progressBar, earningsProgress;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Adapters
    private PendingRequestAdapter pendingRequestAdapter;
    private PropertyAdapter propertyAdapter;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        initViews();
        setupFirebase();
        loadOwnerData();
        setupClickListeners();
        setupBottomNavigation();
        loadPendingRequests();
        loadProperties();
    }

    private void initViews() {
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvTotalProperties = findViewById(R.id.tvTotalProperties);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvThisMonthEarnings = findViewById(R.id.tvThisMonthEarnings);
        tvLastMonthEarnings = findViewById(R.id.tvLastMonthEarnings);
        tvViewAllRequests = findViewById(R.id.tvViewAllRequests);

        cardAddProperty = findViewById(R.id.cardAddProperty);
        cardMyProperties = findViewById(R.id.cardMyProperties);
        cardBookings = findViewById(R.id.cardBookings);
        cardAnalytics = findViewById(R.id.cardAnalytics);

        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        rvProperties = findViewById(R.id.rvProperties);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        ivNotifications = findViewById(R.id.ivNotifications);
        ivSettings = findViewById(R.id.ivSettings);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);
        earningsProgress = findViewById(R.id.earningsProgress);

        // Setup RecyclerViews
        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        rvProperties.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapters with empty lists
        pendingRequestAdapter = new PendingRequestAdapter(new ArrayList<>(), this::onRequestAction);
        propertyAdapter = new PropertyAdapter(new ArrayList<>(), this::onPropertyClick);

        rvPendingRequests.setAdapter(pendingRequestAdapter);
        rvProperties.setAdapter(propertyAdapter);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadOwnerData() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvOwnerName.setText(name != null ? "Welcome, " + name.split(" ")[0] : "Property Owner");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });

        loadStatistics();
    }

    private void loadStatistics() {
        if (currentUserId == null) return;

        // Count properties
        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvTotalProperties.setText(String.valueOf(count));
                });

        // Count bookings and earnings
        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .get()
                .addOnSuccessListener(rooms -> {
                    int totalBookings = 0;
                    double totalEarnings = 0;

                    // You need to iterate through each room and count its bookings
                    // This is simplified - you'll need to implement actual counting

                    tvTotalBookings.setText(String.valueOf(totalBookings));
                    tvTotalEarnings.setText("$" + totalEarnings);
                    tvThisMonthEarnings.setText("$" + (totalEarnings * 0.3));
                    tvLastMonthEarnings.setText("$" + (totalEarnings * 0.2));
                });
    }

    private void setupClickListeners() {
        cardAddProperty.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboard.this, PostRoomActivity.class);
            startActivity(intent);
        });


        //modificatio required here
        cardMyProperties.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboard.this, MyPropertiesActivity.class);
            startActivity(intent);
        });

        cardBookings.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboard.this, BookingRequestsActivity.class);
            intent.putExtra("role", "owner");
            startActivity(intent);
        });

        cardAnalytics.setOnClickListener(v -> {
            Toast.makeText(this, "Analytics feature coming soon", Toast.LENGTH_SHORT).show();
        });

        ivNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show();
        });

        ivSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
        });

        tvViewAllRequests.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboard.this, BookingRequestsActivity.class);
            intent.putExtra("role", "owner");
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(OwnerDashboard.this, BookingRequestsActivity.class);
                intent.putExtra("role", "owner");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.tab_profile) {
                startActivity(new Intent(OwnerDashboard.this, ProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    private void loadPendingRequests() {
        if (currentUserId == null) return;

        showLoading(true);

        // Create a temporary list to collect all requests
        List<BookingRequest> allPendingRequests = new ArrayList<>();

        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .get()
                .addOnSuccessListener(rooms -> {
                    if (rooms.isEmpty()) {
                        showLoading(false);
                        pendingRequestAdapter.setRequests(allPendingRequests);
                        return;
                    }

                    // Track how many rooms have been processed
                    final int[] processedRooms = {0};
                    final int totalRooms = rooms.size();

                    for (QueryDocumentSnapshot room : rooms) {
                        String roomId = room.getId();
                        String roomTitle = room.getString("title");
                        if (roomTitle == null) roomTitle = "Unknown Room";

                        String finalRoomTitle = roomTitle;
                        db.collection("rooms")
                                .document(roomId)
                                .collection("bookings")
                                .whereEqualTo("status", "pending")
                                .get()
                                .addOnSuccessListener(bookings -> {
                                    for (QueryDocumentSnapshot booking : bookings) {
                                        BookingRequest request = booking.toObject(BookingRequest.class);
                                        if (request != null) {
                                            request.setId(booking.getId());
                                            request.setRoomId(roomId);
                                            request.setRoomTitle(finalRoomTitle);
                                            allPendingRequests.add(request);
                                        }
                                    }

                                    processedRooms[0]++;

                                    // Update adapter after processing all rooms
                                    if (processedRooms[0] == totalRooms) {
                                        pendingRequestAdapter.setRequests(allPendingRequests);
                                        showLoading(false);

                                        // Show empty state if no requests
                                        if (allPendingRequests.isEmpty()) {
                                            Toast.makeText(this, "No pending requests", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processedRooms[0]++;
                                    if (processedRooms[0] == totalRooms) {
                                        pendingRequestAdapter.setRequests(allPendingRequests);
                                        showLoading(false);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProperties() {
        if (currentUserId == null) return;

        showLoading(true);

        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .get()
                .addOnSuccessListener(rooms -> {
                    List<Room> propertyList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : rooms) {
                        Room room = document.toObject(Room.class);
                        if (room != null) {
                            room.setId(document.getId());
                            propertyList.add(room);
                        }
                    }

                    propertyAdapter.setRooms(propertyList);
                    showLoading(false);

                    if (propertyList.isEmpty()) {
                        Toast.makeText(this, "No properties found. Add your first property!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading properties: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onRequestAction(BookingRequest request, String action) {
        if ("accept".equals(action)) {
            updateBookingStatus(request, "approved");
        } else if ("reject".equals(action)) {
            updateBookingStatus(request, "rejected");
        }
    }

    // When a booking is approved or created
    private void updateBookingStatus(BookingRequest request, String status) {
        showLoading(true);

        // Update booking status
        db.collection("rooms")
                .document(request.getRoomId())
                .collection("bookings")
                .document(request.getId())
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    // Also update the room's bookings count
                    updateRoomBookingsCount(request.getRoomId());

                    showLoading(false);
                    Toast.makeText(this, "Booking " + status + "d", Toast.LENGTH_SHORT).show();
                    loadPendingRequests();
                    loadStatistics();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to update room's bookings count
    private void updateRoomBookingsCount(String roomId) {
        // Count all approved bookings for this room
        db.collection("rooms")
                .document(roomId)
                .collection("bookings")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(bookings -> {
                    int count = bookings.size();

                    // Update the room document with the count
                    db.collection("rooms")
                            .document(roomId)
                            .update("bookingsCount", count)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("OwnerDashboard", "Updated bookings count for room: " + roomId + " to " + count);
                            });
                });
    }

    private void onPropertyClick(Room room) {
        Intent intent = new Intent(OwnerDashboard.this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}