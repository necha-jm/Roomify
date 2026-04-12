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

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.BookingResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerDashboard extends AppCompatActivity {

    private static final String TAG = "OwnerDashboard";

    // Views
    private TextView tvOwnerName, tvTotalProperties, tvTotalBookings, tvTotalEarnings;
    private TextView tvThisMonthEarnings, tvLastMonthEarnings, tvViewAllRequests;
    private MaterialCardView cardAddProperty, cardMyProperties, cardBookings, cardAnalytics;
    private RecyclerView rvPendingRequests, rvProperties;
    private BottomNavigationView bottomNavigation;
    private ImageView ivNotifications, ivSettings;
    private View loadingOverlay;
    private ProgressBar progressBar, earningsProgress;

    // MySQL Backend components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private User currentUser;
    private Long currentUserId;

    // Adapters
    private PendingRequestAdapter pendingRequestAdapter;
    private PropertyAdapter propertyAdapter;

    private List<Room> allProperties;
    private List<BookingResponse> allPendingRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        initViews();
        setupBackend();
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

        // Initialize lists and adapters
        allProperties = new ArrayList<>();
        allPendingRequests = new ArrayList<>();

        // Create adapter with BookingResponse
        pendingRequestAdapter = new PendingRequestAdapter(new ArrayList<BookingResponse>(),
                new PendingRequestAdapter.OnRequestActionListener() {
                    @Override
                    public void onAction(BookingResponse request, String action) {
                        if ("accept".equals(action)) {
                            updateBookingStatus(request, "ACCEPTED");
                        } else if ("reject".equals(action)) {
                            updateBookingStatus(request, "REJECTED");
                        }
                    }
                });

        propertyAdapter = new PropertyAdapter(new ArrayList<>(), this::onPropertyClick);

        rvPendingRequests.setAdapter(pendingRequestAdapter);
        rvProperties.setAdapter(propertyAdapter);
    }

    private void setupBackend() {
        tokenManager = new TokenManager(this);

        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUser = tokenManager.getUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);
    }

    private void loadOwnerData() {
        if (currentUser != null) {
            String name = currentUser.getName();
            tvOwnerName.setText(name != null ? "Welcome, " + name.split(" ")[0] : "Property Owner");
        }

        loadStatistics();
    }

    private void loadStatistics() {
        if (currentUserId == null) return;

        showLoading(true);

        // Get properties count
        Call<List<Room>> roomsCall = apiInterface.getRoomsByOwner(currentUserId);
        roomsCall.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvTotalProperties.setText(String.valueOf(response.body().size()));
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                Log.e(TAG, "Failed to load properties count", t);
            }
        });

        // Get all bookings for this owner
        Call<ApiResponse<List<BookingResponse>>> bookingsCall = apiInterface.getOwnerBookings(currentUserId);
        bookingsCall.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<BookingResponse> allBookings = response.body().getData();
                    calculateStatisticsFromBookings(allBookings);
                } else {
                    Log.e(TAG, "Failed to load bookings for statistics");
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                Log.e(TAG, "Network error loading bookings", t);
                showLoading(false);
            }
        });
    }

    private void calculateStatisticsFromBookings(List<BookingResponse> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            runOnUiThread(() -> {
                tvTotalBookings.setText("0");
                tvTotalEarnings.setText("$0");
                tvThisMonthEarnings.setText("$0");
                tvLastMonthEarnings.setText("$0");
                showLoading(false);
            });
            return;
        }

        int totalBookings = 0;
        double totalEarnings = 0;

        for (BookingResponse booking : bookings) {
            if ("ACCEPTED".equals(booking.getStatus()) || "CONFIRMED".equals(booking.getStatus())) {
                totalBookings++;
                totalEarnings += booking.getTotalPrice();
            }
        }

        final int finalTotalBookings = totalBookings;
        final double finalTotalEarnings = totalEarnings;

        runOnUiThread(() -> {
            tvTotalBookings.setText(String.valueOf(finalTotalBookings));
            tvTotalEarnings.setText(String.format("$%.2f", finalTotalEarnings));
            tvThisMonthEarnings.setText("$0.00");
            tvLastMonthEarnings.setText("$0.00");
            showLoading(false);
        });
    }

    private void setupClickListeners() {
        cardAddProperty.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboard.this, PostRoomActivity.class);
            startActivity(intent);
        });

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

            if (itemId == R.id.tab_menu) {
                Intent intent = new Intent(OwnerDashboard.this, LocationMap.class);
                startActivity(intent);
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
        allPendingRequests.clear();

        Call<ApiResponse<List<BookingResponse>>> call = apiInterface.getOwnerBookings(currentUserId);
        call.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<BookingResponse> allBookings = response.body().getData();

                    if (allBookings != null) {
                        // Filter only PENDING bookings
                        for (BookingResponse booking : allBookings) {
                            if ("PENDING".equals(booking.getStatus())) {
                                allPendingRequests.add(booking);
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        pendingRequestAdapter.setRequests(allPendingRequests);
                        showLoading(false);

                        if (allPendingRequests.isEmpty()) {
                            Toast.makeText(OwnerDashboard.this, "No pending requests", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    showLoading(false);
                    Log.e(TAG, "Failed to load pending requests");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading pending requests", t);
                Toast.makeText(OwnerDashboard.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProperties() {
        if (currentUserId == null) return;

        showLoading(true);

        Call<List<Room>> call = apiInterface.getRoomsByOwner(currentUserId);
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Room> properties = response.body();
                    propertyAdapter.setRooms(properties);

                    if (properties.isEmpty()) {
                        Toast.makeText(OwnerDashboard.this, "No properties found. Add your first property!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(OwnerDashboard.this, "Error loading properties", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading properties", t);
                Toast.makeText(OwnerDashboard.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBookingStatus(BookingResponse request, String status) {
        showLoading(true);

        Call<ApiResponse<Void>> call;
        if ("ACCEPTED".equals(status)) {
            call = apiInterface.acceptBooking(request.getId());
        } else {
            call = apiInterface.rejectBooking(request.getId());
        }

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OwnerDashboard.this, "Booking " + status.toLowerCase(), Toast.LENGTH_SHORT).show();
                    loadPendingRequests(); // Refresh pending requests
                    loadStatistics(); // Refresh statistics
                } else {
                    String message = response.body() != null ? response.body().getMessage() : "Unknown error";
                    Toast.makeText(OwnerDashboard.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error updating booking status", t);
                Toast.makeText(OwnerDashboard.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onPropertyClick(Room room) {
        Intent intent = new Intent(OwnerDashboard.this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}