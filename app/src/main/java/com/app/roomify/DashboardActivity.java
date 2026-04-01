package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvActiveBookings, tvTotalSpent, tvSaved;
    private MaterialCardView cardFindRoom, cardMyBookings, cardPayment, cardSupport;
    private RecyclerView rvRecentBookings, rvRecommendations;
    private BottomNavigationView bottomNavigation;
    private View loadingOverlay;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BookingAdapter bookingAdapter;
    private RoomAdapter recommendationAdapter;

    private ArrayList<Room> rooms;

    private ArrayList<BookingRequest> requests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        setupFirebase();
        loadUserData();
        setupClickListeners();
        setupBottomNavigation();
        loadRecentBookings();
        loadRecommendations();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvActiveBookings = findViewById(R.id.tvActiveBookings);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvSaved = findViewById(R.id.tvSaved);

        cardFindRoom = findViewById(R.id.cardFindRoom);
        cardMyBookings = findViewById(R.id.cardMyBookings);
        cardPayment = findViewById(R.id.cardPayment);
        cardSupport = findViewById(R.id.cardSupport);

        rvRecentBookings = findViewById(R.id.rvRecentBookings);
        rvRecommendations = findViewById(R.id.rvRecommendations);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerViews
        rvRecentBookings.setLayoutManager(new LinearLayoutManager(this));
        rvRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // ✅ INITIALIZE adapters FIRST
        rooms = new ArrayList<>();
        requests = new ArrayList<>();
        recommendationAdapter = new RoomAdapter(rooms);

        bookingAdapter = new BookingAdapter(new ArrayList<>(), (request, action) -> {
            // Handle actions here
            switch (action) {
                case "accept":
                    Toast.makeText(this, "Accepted", Toast.LENGTH_SHORT).show();
                    break;
                case "reject":
                    Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show();
                    break;
                case "cancel":
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                    break;
                case "delete":
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        rvRecentBookings.setAdapter(bookingAdapter);
        rvRecommendations.setAdapter(recommendationAdapter);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            tvUserName.setText(name != null ? "Welcome back, " + name.split(" ")[0] : "Welcome back");
                        }
                    });
        }

        // Set sample stats (replace with actual data from Firestore)
        tvActiveBookings.setText("2");
        tvTotalSpent.setText("$1,200");
        tvSaved.setText("$350");
    }

    private void setupClickListeners() {
        cardFindRoom.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LocationMap.class);
            startActivity(intent);
        });

        cardMyBookings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, BookingRequestsActivity.class);
            intent.putExtra("role", "tenant");
            startActivity(intent);
        });

        cardPayment.setOnClickListener(v -> {
            Toast.makeText(this, "Payment feature coming soon", Toast.LENGTH_SHORT).show();
        });

        cardSupport.setOnClickListener(v -> {
            Toast.makeText(this, "Contact support: support@roomify.com", Toast.LENGTH_SHORT).show();
        });

        TextView tvViewAll = findViewById(R.id.tvViewAll);
        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, BookingRequestsActivity.class);
            intent.putExtra("role", "tenant");
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.tab_menu) {
                startActivity(new Intent(DashboardActivity.this, LocationMap.class));
                return true;
            } else if (itemId == R.id.nav_bookings) {
                Intent intent = new Intent(DashboardActivity.this, BookingRequestsActivity.class);
                intent.putExtra("role", "tenant");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.tab_profile) {
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
                return true;
            }

            return false;
        });
    }

    private void loadRecentBookings() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) return;

        showLoading(true);

        db.collection("users")
                .document(userId)
                .collection("bookings")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    List<BookingRequest> bookings = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BookingRequest booking = document.toObject(BookingRequest.class);
                        booking.setId(document.getId());
                        bookings.add(booking);
                    }

                    bookingAdapter.setRequests(bookings);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRecommendations() {
        db.collection("rooms")
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Room> rooms = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Room room = document.toObject(Room.class);
                        room.setId(document.getId());
                        rooms.add(room);
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading recommendations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        recommendationAdapter.setRooms(rooms);
    }

    private void onBookingClick(BookingRequest booking) {
        Intent intent = new Intent(this, BookingRequestsActivity.class);
        intent.putExtra("booking_id", booking.getId());
        startActivity(intent);
    }

    private void onRoomClick(Room room) {
        Intent intent = new Intent(this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}