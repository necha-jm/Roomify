package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.AuthResponse;
import com.app.roomify.models.BookingResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TextView tvUserName, tvActiveBookings, tvTotalSpent, tvSaved;
    private MaterialCardView cardFindRoom, cardMyBookings, cardPayment, cardSupport;
    private RecyclerView rvRecentBookings, rvRecommendations;
    private BottomNavigationView bottomNavigation;
    private View loadingOverlay;
    private ProgressBar progressBar;

    // MySQL Backend components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private User currentUser;
    private Long currentUserId = null;

    private BookingResponseAdapter bookingAdapter;
    private RoomAdapter recommendationAdapter;

    private ArrayList<Room> rooms;
    private ArrayList<BookingResponse> bookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        setupBackend();
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

        // Initialize adapters
        rooms = new ArrayList<>();
        bookings = new ArrayList<>();

        recommendationAdapter = new RoomAdapter(rooms);

        bookingAdapter = new BookingResponseAdapter(bookings, (booking, action) -> {
            switch (action) {
                case "accept":
                    handleAcceptBooking(booking);
                    break;
                case "reject":
                    handleRejectBooking(booking);
                    break;
                case "cancel":
                    handleCancelBooking(booking);
                    break;
                case "delete":
                    handleDeleteBooking(booking);
                    break;
            }
        });

        rvRecentBookings.setAdapter(bookingAdapter);
        rvRecommendations.setAdapter(recommendationAdapter);
    }

    private void setupBackend() {
        tokenManager = new TokenManager(this);

        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            // Navigate to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
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

    private void loadUserData() {
        if (currentUser != null) {
            String name = currentUser.getName();
            tvUserName.setText(name != null && !name.isEmpty() ? "Welcome back, " + name.split(" ")[0] : "Welcome back");
        } else {
            tvUserName.setText("Welcome back");
            // Fetch fresh user data from API
            fetchCurrentUser();
        }

        // Load dashboard statistics
        loadDashboardStats();
    }

    private void fetchCurrentUser() {
        showLoading(true);
        String token = tokenManager.getToken();
        if (token == null) {
            showLoading(false);
            return;
        }

        Call<AuthResponse> call = apiInterface.getCurrentUser("Bearer " + token);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.isSuccess() && authResponse.getUser() != null) {
                        currentUser = authResponse.getUser();
                        currentUserId = currentUser.getId();
                        tokenManager.saveUser(currentUser);

                        String name = currentUser.getName();
                        tvUserName.setText(name != null && !name.isEmpty() ? "Welcome back, " + name.split(" ")[0] : "Welcome back");
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Failed to fetch user", t);
            }
        });
    }

    private void loadDashboardStats() {
        if (currentUserId == null) {
            Log.d(TAG, "No user ID available for stats");
            tvActiveBookings.setText("0");
            tvTotalSpent.setText("$0");
            return;
        }

        Call<ApiResponse<List<BookingResponse>>> bookingsCall = apiInterface.getUserBookings(currentUserId);
        bookingsCall.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<BookingResponse> userBookings = response.body().getData();
                    if (userBookings != null && !userBookings.isEmpty()) {
                        // Count active bookings (pending or accepted)
                        long activeCount = userBookings.stream()
                                .filter(b -> "PENDING".equalsIgnoreCase(b.getStatus()) ||
                                        "ACCEPTED".equalsIgnoreCase(b.getStatus()) ||
                                        "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                                .count();
                        tvActiveBookings.setText(String.valueOf(activeCount));

                        // Calculate total spent (for accepted/confirmed bookings)
                        double totalSpent = userBookings.stream()
                                .filter(b -> "ACCEPTED".equalsIgnoreCase(b.getStatus()) ||
                                        "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                                .mapToDouble(BookingResponse::getTotalPrice)
                                .sum();
                        tvTotalSpent.setText("$" + String.format("%.2f", totalSpent));
                    } else {
                        tvActiveBookings.setText("0");
                        tvTotalSpent.setText("$0");
                    }
                } else {
                    tvActiveBookings.setText("0");
                    tvTotalSpent.setText("$0");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                Log.e(TAG, "Failed to load stats", t);
                tvActiveBookings.setText("0");
                tvTotalSpent.setText("$0");
            }
        });

        // Set saved amount
        tvSaved.setText("$0");
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
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, BookingRequestsActivity.class);
                intent.putExtra("role", "tenant");
                startActivity(intent);
            });
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.tab_menu) {
                startActivity(new Intent(DashboardActivity.this, LocationMap.class));
                finish();
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
        if (currentUserId == null) {
            Log.d(TAG, "No user ID available for recent bookings");
            showLoading(false);
            return;
        }

        showLoading(true);

        Call<ApiResponse<List<BookingResponse>>> call = apiInterface.getUserBookings(currentUserId);
        call.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<BookingResponse> allBookings = response.body().getData();
                    if (allBookings != null && !allBookings.isEmpty()) {
                        // Get only last 3 bookings
                        List<BookingResponse> recentBookings = allBookings.stream()
                                .limit(3)
                                .collect(Collectors.toList());
                        bookingAdapter.setBookings(recentBookings);
                    } else {
                        bookingAdapter.setBookings(new ArrayList<>());
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Unknown error";
                    Log.e(TAG, "Error loading bookings: " + errorMsg);
                    bookingAdapter.setBookings(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading bookings", t);
                bookingAdapter.setBookings(new ArrayList<>());
            }
        });
    }

    // ==================== ONLY THIS METHOD IS MODIFIED ====================
    private void loadRecommendations() {
        showLoading(true);

        // Use unwrapped response
        Call<List<Room>> call = apiInterface.getAllRooms();
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Room> allRooms = response.body();
                    if (allRooms != null && !allRooms.isEmpty()) {
                        List<Room> recommendedRooms = allRooms.stream()
                                .limit(5)
                                .collect(Collectors.toList());
                        recommendationAdapter.setRooms(recommendedRooms);
                    }
                } else {
                    Log.e(TAG, "Error loading recommendations");
                    recommendationAdapter.setRooms(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading recommendations", t);
                recommendationAdapter.setRooms(new ArrayList<>());
            }
        });
    }
    // ==================== END OF MODIFIED METHOD ====================

    private void handleAcceptBooking(BookingResponse booking) {
        if (booking.getId() == null) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<Void>> call = apiInterface.acceptBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(DashboardActivity.this, "Booking accepted", Toast.LENGTH_SHORT).show();
                    loadRecentBookings();
                    loadDashboardStats();
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Failed to accept booking";
                    Toast.makeText(DashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRejectBooking(BookingResponse booking) {
        if (booking.getId() == null) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<Void>> call = apiInterface.rejectBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(DashboardActivity.this, "Booking rejected", Toast.LENGTH_SHORT).show();
                    loadRecentBookings();
                    loadDashboardStats();
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Failed to reject booking";
                    Toast.makeText(DashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCancelBooking(BookingResponse booking) {
        if (booking.getId() == null) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<Void>> call = apiInterface.cancelBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(DashboardActivity.this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    loadRecentBookings();
                    loadDashboardStats();
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Failed to cancel booking";
                    Toast.makeText(DashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDeleteBooking(BookingResponse booking) {
        if (booking.getId() == null) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<Void>> call = apiInterface.deleteBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(DashboardActivity.this, "Booking deleted", Toast.LENGTH_SHORT).show();
                    loadRecentBookings();
                    loadDashboardStats();
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Failed to delete booking";
                    Toast.makeText(DashboardActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadRecentBookings();
            loadDashboardStats();
        }
    }
}