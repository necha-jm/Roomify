package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPropertiesActivity extends AppCompatActivity {

    private static final String TAG = "MyProperties";

    private RecyclerView rvProperties;
    private ProgressBar progressBar;
    private TextView tvNoProperties, tvErrorMessage;
    private FloatingActionButton fabAddProperty;
    private ImageView ivBack;
    private TextView tvTitle;

    // Backend Components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private MyPropertiesAdapter propertiesAdapter;
    private List<Room> propertyList;
    private Long currentUserId = null;  // FIXED: Use Long object instead of primitive long

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_properties);

        // Initialize backend components
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        initViews();
        setupUser();
        setupClickListeners();
        loadProperties();
    }

    private void initViews() {
        rvProperties = findViewById(R.id.rvProperties);
        progressBar = findViewById(R.id.progressBar);
        tvNoProperties = findViewById(R.id.tvNoProperties);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        fabAddProperty = findViewById(R.id.fabAddProperty);
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);

        rvProperties.setLayoutManager(new LinearLayoutManager(this));
        propertyList = new ArrayList<>();
        propertiesAdapter = new MyPropertiesAdapter(propertyList, this::onPropertyClick, this::onPropertyDelete);
        rvProperties.setAdapter(propertiesAdapter);
    }

    private void setupUser() {
        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = tokenManager.getUserId();
        Log.d(TAG, "Current User ID: " + currentUserId);
    }

    private void setupClickListeners() {
        fabAddProperty.setOnClickListener(v -> {
            Intent intent = new Intent(MyPropertiesActivity.this, PostRoomActivity.class);
            startActivity(intent);
        });

        ivBack.setOnClickListener(v -> finish());
    }

    private void loadProperties() {
        // FIXED: Check if currentUserId is null (not primitive comparison)
        if (currentUserId == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        tvNoProperties.setVisibility(View.GONE);
        tvErrorMessage.setVisibility(View.GONE);

        Log.d(TAG, "Loading properties for user ID: " + currentUserId);

        Call<List<Room>> call = apiInterface.getRoomsByOwner(currentUserId);
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Room> rooms = response.body();
                    propertyList.clear();

                    Log.d(TAG, "Loaded " + rooms.size() + " properties for user");

                    if (rooms.isEmpty()) {
                        tvNoProperties.setVisibility(View.VISIBLE);
                        tvErrorMessage.setText("No properties found. Try posting one!");
                        tvErrorMessage.setVisibility(View.VISIBLE);
                        propertiesAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (Room room : rooms) {
                        Log.d(TAG, "Property found - ID: " + room.getId());
                        Log.d(TAG, "  Title: " + room.getTitle());
                        Log.d(TAG, "  Price: $" + room.getPrice());
                        Log.d(TAG, "  Status: " + room.getStatus());
                        propertyList.add(room);
                    }

                    // Sort by createdAt (newest first)
                    Collections.sort(propertyList, new Comparator<Room>() {
                        @Override
                        public int compare(Room r1, Room r2) {
                            String date1 = r1.getCreatedAt();
                            String date2 = r2.getCreatedAt();

                            if (date1 == null && date2 == null) return 0;
                            if (date1 == null) return 1;
                            if (date2 == null) return -1;

                            return date2.compareTo(date1);
                        }
                    });

                    propertiesAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + propertyList.size() + " properties into adapter");

                    if (propertyList.isEmpty()) {
                        tvNoProperties.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e(TAG, "Failed to load properties. Code: " + response.code());
                    showError("Failed to load properties. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading properties: " + t.getMessage());
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void onPropertyClick(Room room) {
        Intent intent = new Intent(MyPropertiesActivity.this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        startActivity(intent);
    }

    private void onPropertyDelete(Room room) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete \"" + room.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProperty(room))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProperty(Room room) {
        if (room == null || room.getId() == 0) {
            Toast.makeText(this, "Invalid property", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        long roomId = room.getId();
        Log.d(TAG, "Attempting to delete property with ID: " + roomId);

        Call<ApiResponse<Void>> call = apiInterface.deleteRoom(roomId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                showLoading(false);

                Log.d(TAG, "Delete response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "✅ Property deleted successfully: " + roomId);
                        Toast.makeText(MyPropertiesActivity.this, "Property deleted", Toast.LENGTH_SHORT).show();
                        loadProperties(); // Refresh the list
                    } else {
                        String errorMsg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Failed to delete property";
                        Log.e(TAG, "Delete failed: " + errorMsg);
                        Toast.makeText(MyPropertiesActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "Error deleting property";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(MyPropertiesActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error deleting property: " + t.getMessage(), t);
                Toast.makeText(MyPropertiesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            tvErrorMessage.setVisibility(View.VISIBLE);
            tvErrorMessage.setText(message);
            tvNoProperties.setVisibility(View.VISIBLE);
            Toast.makeText(MyPropertiesActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            rvProperties.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProperties();
    }
}