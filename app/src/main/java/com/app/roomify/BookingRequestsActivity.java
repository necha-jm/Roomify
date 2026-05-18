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
import com.app.roomify.models.BookingResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingRequestsActivity extends AppCompatActivity {

    private static final String TAG = "BookingRequests";

    private RecyclerView recyclerView;
    private BookingResponseAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoRequests;
    private TextView tvErrorMessage;

    private long currentUserId = -1L;
    private String userRole;

    // Backend components
    private APIInterface apiInterface;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_requests);

        // Initialize backend
        tokenManager = TokenManager.getInstance(this);

        // Debug: Check authentication status
        Log.d(TAG, "=== AUTHENTICATION DEBUG ===");
        Log.d(TAG, "isLoggedIn: " + tokenManager.isLoggedIn());
        Log.d(TAG, "Token: " + (tokenManager.getToken() != null ? "Present (length: " + tokenManager.getToken().length() + ")" : "NULL"));

        User currentUser = tokenManager.getUser();
        if (currentUser != null) {
            Log.d(TAG, "User Email: " + currentUser.getEmail());
            Log.d(TAG, "User ID from object: " + currentUser.getId());
        } else {
            Log.d(TAG, "User object is NULL");
        }

        Long userIdFromManager = tokenManager.getUserId();
        Log.d(TAG, "User ID from manager: " + userIdFromManager);
        Log.d(TAG, "==========================");

        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Please login to view bookings", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Get user ID - try multiple methods
        if (userIdFromManager != null && userIdFromManager > 0) {
            currentUserId = userIdFromManager;
        } else if (currentUser != null && currentUser.getId() > 0) {
            currentUserId = currentUser.getId();
        } else {
            Log.e(TAG, "Cannot get valid user ID!");
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            tokenManager.clear();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "Final User ID: " + currentUserId);

        // Force reset client to ensure fresh token
        APIClient.resetClient();
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvNoRequests = findViewById(R.id.tvNoRequests);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);

        userRole = getIntent().getStringExtra("role");
        if (userRole == null) {
            userRole = "tenant";
        }

        Log.d(TAG, "Role: " + userRole);

        setupRecyclerView();
        loadBookingRequests();
    }

    private void setupRecyclerView() {
        adapter = new BookingResponseAdapter(new ArrayList<>(), this::onBookingAction);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadBookingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoRequests.setVisibility(View.GONE);

        if (tvErrorMessage != null) {
            tvErrorMessage.setVisibility(View.GONE);
        }

        if ("owner".equals(userRole)) {
            loadOwnerRequests();
        } else {
            loadUserRequests();
        }
    }

    private void loadUserRequests() {
        Log.d(TAG, "Loading user requests for user ID: " + currentUserId);

        Call<ApiResponse<List<BookingResponse>>> call =
                apiInterface.getUserBookings(currentUserId);

        call.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {

                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<BookingResponse>> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        List<BookingResponse> requests = apiResponse.getData();

                        if (requests != null && !requests.isEmpty()) {
                            Log.d(TAG, "Found " + requests.size() + " bookings for user");
                            adapter.setBookings(requests);
                        } else {
                            tvNoRequests.setVisibility(View.VISIBLE);
                            tvNoRequests.setText("No booking requests found");
                        }
                    } else {
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText(apiResponse.getMessage());
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    Log.e(TAG, "Authentication failed - Code: " + response.code());
                    tvNoRequests.setVisibility(View.VISIBLE);
                    tvNoRequests.setText("Session expired. Please login again.");
                    // Clear token and redirect to login
                    tokenManager.clear();
                    Intent intent = new Intent(BookingRequestsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    tvNoRequests.setVisibility(View.VISIBLE);
                    tvNoRequests.setText("Error loading bookings");
                    Log.e(TAG, "Response error - Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call,
                                  Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvNoRequests.setVisibility(View.VISIBLE);
                tvNoRequests.setText("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }

    private void loadOwnerRequests() {
        Log.d(TAG, "Loading owner requests for user ID: " + currentUserId);

        Call<ApiResponse<List<BookingResponse>>> call =
                apiInterface.getOwnerBookings(currentUserId);

        call.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                   Response<ApiResponse<List<BookingResponse>>> response) {

                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<BookingResponse>> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        List<BookingResponse> bookings = apiResponse.getData();

                        if (bookings != null && !bookings.isEmpty()) {
                            Log.d(TAG, "Found " + bookings.size() + " booking requests");
                            adapter.setBookings(bookings);
                        } else {
                            tvNoRequests.setVisibility(View.VISIBLE);
                            tvNoRequests.setText("No booking requests for your rooms");
                        }
                    } else {
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText(apiResponse.getMessage());
                        Log.e(TAG, "API Error: " + apiResponse.getMessage());
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    Log.e(TAG, "Authentication failed - Code: " + response.code());
                    tvNoRequests.setVisibility(View.VISIBLE);
                    tvNoRequests.setText("Session expired. Please login again.");
                    tokenManager.clear();
                    Intent intent = new Intent(BookingRequestsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    tvNoRequests.setVisibility(View.VISIBLE);
                    tvNoRequests.setText("Error loading bookings");
                    Log.e(TAG, "Response error - Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvNoRequests.setVisibility(View.VISIBLE);
                tvNoRequests.setText("Network error: " + t.getMessage());
                Log.e(TAG, "Network error", t);
            }
        });
    }

    private void onBookingAction(BookingResponse booking, String action) {
        if (booking == null || booking.getId() == null) {
            Log.e(TAG, "Invalid booking");
            Toast.makeText(this, "Error: Invalid booking", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "onBookingAction: " + action + " - Booking ID: " + booking.getId());

        switch (action) {
            case "accept":
                acceptBooking(booking);
                break;
            case "reject":
                rejectBooking(booking);
                break;
            case "cancel":
                cancelBooking(booking);
                break;
            case "delete":
                deleteBooking(booking);
                break;
        }
    }

    private void acceptBooking(BookingResponse booking) {
        Log.d(TAG, "Accepting booking - ID: " + booking.getId());

        // 🔒 Prevent double click
        booking.setStatus("PROCESSING");
        adapter.updateBooking(booking);

        Call<ApiResponse<Void>> call = apiInterface.acceptBooking(booking.getId());

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                    Log.d(TAG, "Booking accepted successfully");

                    booking.setStatus("ACCEPTED");
                    adapter.updateBooking(booking);

                    Toast.makeText(BookingRequestsActivity.this,
                            "Booking accepted!", Toast.LENGTH_SHORT).show();

                } else if (response.code() == 409) {
                    // 🔥 NEW: conflict = already booked
                    Toast.makeText(BookingRequestsActivity.this,
                            "Room already booked by another user", Toast.LENGTH_LONG).show();

                    adapter.removeBooking(booking);

                } else if (response.code() == 401 || response.code() == 403) {
                    handleSessionExpired();

                } else {
                    String errorMsg = response.body() != null ?
                            response.body().getMessage() : "Server error";

                    Toast.makeText(BookingRequestsActivity.this,
                            "Failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BookingRequestsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectBooking(BookingResponse booking) {
        Log.d(TAG, "Rejecting booking - ID: " + booking.getId());

        Call<ApiResponse<Void>> call = apiInterface.rejectBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Booking rejected successfully");
                    Toast.makeText(BookingRequestsActivity.this, "Booking rejected", Toast.LENGTH_SHORT).show();
                    booking.setStatus("REJECTED");
                    adapter.updateBooking(booking);
                } else if (response.code() == 401 || response.code() == 403) {
                    handleSessionExpired();
                } else {
                    Toast.makeText(BookingRequestsActivity.this, "Failed to reject booking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BookingRequestsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelBooking(BookingResponse booking) {
        Log.d(TAG, "Cancelling booking - ID: " + booking.getId());

        Call<ApiResponse<Void>> call = apiInterface.cancelBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Booking cancelled successfully");
                    Toast.makeText(BookingRequestsActivity.this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    adapter.removeBooking(booking);
                } else if (response.code() == 401 || response.code() == 403) {
                    handleSessionExpired();
                } else {
                    Toast.makeText(BookingRequestsActivity.this, "Failed to cancel booking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BookingRequestsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteBooking(BookingResponse booking) {
        Log.d(TAG, "Deleting booking - ID: " + booking.getId());

        Call<ApiResponse<Void>> call = apiInterface.deleteBooking(booking.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ Booking deleted successfully");
                    Toast.makeText(BookingRequestsActivity.this, "Booking deleted", Toast.LENGTH_SHORT).show();
                    adapter.removeBooking(booking);
                } else if (response.code() == 401 || response.code() == 403) {
                    handleSessionExpired();
                } else {
                    Toast.makeText(BookingRequestsActivity.this, "Failed to delete booking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(BookingRequestsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume: refreshing booking requests");

        loadBookingRequests(); // force refresh every time activity becomes visible
    }

    private void handleSessionExpired() {
        Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
        tokenManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}