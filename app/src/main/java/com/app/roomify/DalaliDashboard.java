package com.app.roomify;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.DalaliStats;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DalaliDashboard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "DalaliDashboard";

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvAgentName, tvAgentEmail, tvVerificationStatus, tvAgentLocation;
    private TextView tvTotalListings, tvActiveListings, tvTotalCommission, tvPendingRequests;
    private CardView cardAddListing, cardManageListings, cardMessages, cardEarnings;
    private RecyclerView recyclerViewRecentListings;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LottieAnimationView animationView;
    private MaterialButton btnAddNewListing;
    private TabLayout tabLayout;
    private ImageView ivMenuIcon;

    // Backend components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private User currentUser;
    private PropertyAdapter propertyAdapter;
    private List<Room> recentProperties = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dalali_dashboard);

        initializeComponents();
        setupUI();
        setupNavigationDrawer();
        loadAgentData();
        loadDashboardStats();
        loadRecentListings();
        setupListeners();
    }

    private void initializeComponents() {
        // Initialize backend
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Get current user
        currentUser = tokenManager.getUser();

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        tvAgentName = findViewById(R.id.tvAgentName);
        tvAgentEmail = findViewById(R.id.tvAgentEmail);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvAgentLocation = findViewById(R.id.tvAgentLocation);
        tvTotalListings = findViewById(R.id.tvTotalListings);
        tvActiveListings = findViewById(R.id.tvActiveListings);
        tvTotalCommission = findViewById(R.id.tvTotalCommission);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        cardAddListing = findViewById(R.id.cardAddListing);
        cardManageListings = findViewById(R.id.cardManageListings);
        cardMessages = findViewById(R.id.cardMessages);
        cardEarnings = findViewById(R.id.cardEarnings);
        recyclerViewRecentListings = findViewById(R.id.recyclerViewRecentListings);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        animationView = findViewById(R.id.animationView);
        btnAddNewListing = findViewById(R.id.btnAddNewListing);
        tabLayout = findViewById(R.id.tabLayout);
        ivMenuIcon = findViewById(R.id.ivMenuIcon);

        // Setup recycler view - FIXED: Added both click methods
        propertyAdapter = new PropertyAdapter(recentProperties,
                new PropertyAdapter.OnPropertyClickListener() {
                    @Override
                    public void onPropertyClick(Room room) {
                        // Handle property click
                        Intent intent = new Intent(DalaliDashboard.this, RoomDetailsActivity.class);
                        intent.putExtra("property_id", room.getId());
                        intent.putExtra("property_title", room.getTitle());
                        startActivity(intent);
                    }

                    @Override
                    public void onMenuClick(Room room, View view) {
                        // Handle menu click (three dots)
                        showPropertyOptionsMenu(room, view);
                    }
                },
                "dalali"
        );
        recyclerViewRecentListings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecentListings.setAdapter(propertyAdapter);
    }

    // Add this method to handle menu clicks
    private void showPropertyOptionsMenu(Room room, View view) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.property_menu, popupMenu.getMenu());

        // Customize menu based on property status
        if (room.isStatusAvailable()) {
            popupMenu.getMenu().findItem(R.id.action_mark_rented).setVisible(true);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                editProperty(room);
                return true;
            } else if (id == R.id.action_mark_rented) {
                markAsRented(room);
                return true;
            } else if (id == R.id.action_delete) {
                deleteProperty(room);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void editProperty(Room room) {
        Intent intent = new Intent(this, PostRoomActivity.class);
        intent.putExtra("property_id", room.getId());
        intent.putExtra("mode", "edit");
        startActivity(intent);
    }

    private void markAsRented(Room room) {
        String authHeader = tokenManager.getAuthHeader();
        if (authHeader == null) return;

        Call<ApiResponse<Void>> call = apiInterface.markAsRented(authHeader, room.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(DalaliDashboard.this, "Property marked as rented", Toast.LENGTH_SHORT).show();
                    loadRecentListings(); // Refresh list
                    loadDashboardStats(); // Refresh stats
                } else {
                    Toast.makeText(DalaliDashboard.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DalaliDashboard.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteProperty(Room room) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete " + room.getTitle() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String authHeader = tokenManager.getAuthHeader();
                    if (authHeader == null) return;

                    Call<ApiResponse<Void>> call = apiInterface.deleteRoom(room.getId());
                    call.enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Toast.makeText(DalaliDashboard.this, "Property deleted", Toast.LENGTH_SHORT).show();
                                loadRecentListings();
                                loadDashboardStats();
                            } else {
                                Toast.makeText(DalaliDashboard.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(DalaliDashboard.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupUI() {
        if (currentUser != null) {
            tvAgentName.setText(currentUser.getName() != null ? currentUser.getName() : "Dalali Agent");
            tvAgentEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "agent@roomify.com");

            String location = currentUser.getLocationArea();
            if (location != null && !location.isEmpty()) {
                tvAgentLocation.setText(location);
            } else {
                tvAgentLocation.setText("Working area not set");
            }
        }
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        TextView navAgentName = headerView.findViewById(R.id.navAgentName);
        TextView navAgentEmail = headerView.findViewById(R.id.navAgentEmail);

        if (currentUser != null) {
            if (navAgentName != null) navAgentName.setText(currentUser.getName());
            if (navAgentEmail != null) navAgentEmail.setText(currentUser.getEmail());
        }

        ivMenuIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void loadAgentData() {
        String authHeader = tokenManager.getAuthHeader();
        if (authHeader == null) {
            if (currentUser != null) {
                updateAgentUI(currentUser);
            }
            return;
        }

        Call<ApiResponse<User>> call = apiInterface.getUserProfile(authHeader);
        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        User user = apiResponse.getData();
                        updateAgentUI(user);
                        tokenManager.saveUser(user);
                    }
                } else {
                    if (currentUser != null) {
                        updateAgentUI(currentUser);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                if (currentUser != null) {
                    updateAgentUI(currentUser);
                }
            }
        });
    }

    private void updateAgentUI(User user) {
        tvAgentName.setText(user.getName());
        tvAgentEmail.setText(user.getEmail());

        if (user.getLocationArea() != null && !user.getLocationArea().isEmpty()) {
            tvAgentLocation.setText(user.getLocationArea());
        }

        String verificationStatus = user.getVerificationStatus();
        if ("VERIFIED".equalsIgnoreCase(verificationStatus)) {
            tvVerificationStatus.setText("✓ Verified Agent");
            tvVerificationStatus.setBackgroundResource(R.drawable.bg_verified);
            tvVerificationStatus.setTextColor(getColor(R.color.success_green));
        } else if ("PENDING".equalsIgnoreCase(verificationStatus)) {
            tvVerificationStatus.setText("⏳ Pending Verification");
            tvVerificationStatus.setBackgroundResource(R.drawable.bg_pending);
            tvVerificationStatus.setTextColor(getColor(R.color.warning_orange));
        } else {
            tvVerificationStatus.setText("⚠️ Not Verified");
            tvVerificationStatus.setBackgroundResource(R.drawable.bg_unverified);
            tvVerificationStatus.setTextColor(getColor(R.color.error_red));
        }
    }

    private void loadDashboardStats() {
        String authHeader = tokenManager.getAuthHeader();
        if (authHeader == null) {
            setDefaultStats();
            return;
        }

        if (animationView != null) {
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();
        }

        Call<ApiResponse<DalaliStats>> call = apiInterface.getDalaliStats(authHeader);
        call.enqueue(new Callback<ApiResponse<DalaliStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<DalaliStats>> call, Response<ApiResponse<DalaliStats>> response) {
                if (animationView != null) {
                    animationView.setVisibility(View.GONE);
                    animationView.cancelAnimation();
                }

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<DalaliStats> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        DalaliStats stats = apiResponse.getData();
                        updateStatsUI(stats);
                    } else {
                        setDefaultStats();
                    }
                } else {
                    setDefaultStats();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<DalaliStats>> call, Throwable t) {
                if (animationView != null) {
                    animationView.setVisibility(View.GONE);
                }
                setDefaultStats();
            }
        });
    }

    private void updateStatsUI(DalaliStats stats) {
        tvTotalListings.setText(String.valueOf(stats.getTotalListings()));
        tvActiveListings.setText(String.valueOf(stats.getActiveListings()));
        tvTotalCommission.setText("TZS " + String.format("%,.0f", stats.getTotalCommission()));
        tvPendingRequests.setText(String.valueOf(stats.getPendingListings()));
    }

    private void setDefaultStats() {
        tvTotalListings.setText("0");
        tvActiveListings.setText("0");
        tvTotalCommission.setText("TZS 0");
        tvPendingRequests.setText("0");
    }

    private void loadRecentListings() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        String authHeader = tokenManager.getAuthHeader();
        if (authHeader == null) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        Call<ApiResponse<List<Room>>> call = apiInterface.getAgentProperties(authHeader);
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Room>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        recentProperties.clear();
                        recentProperties.addAll(apiResponse.getData());
                        propertyAdapter.updateProperties(recentProperties);
                    }
                } else {
                    Toast.makeText(DalaliDashboard.this, "Failed to load listings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Toast.makeText(DalaliDashboard.this, "Failed to load listings: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDashboardStats();
            loadRecentListings();
        });

        btnAddNewListing.setOnClickListener(v -> {
            Intent intent = new Intent(DalaliDashboard.this, PostRoomActivity.class);
            intent.putExtra("role", "dalali");
            startActivity(intent);
        });

        cardAddListing.setOnClickListener(v -> {
            Intent intent = new Intent(DalaliDashboard.this, PostRoomActivity.class);
            intent.putExtra("role", "dalali");
            startActivity(intent);
        });

        cardManageListings.setOnClickListener(v -> {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        });

        cardMessages.setOnClickListener(v -> {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        });

        cardEarnings.setOnClickListener(v -> {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        loadRecentListings();
                        break;
                    case 1:
                        loadPendingListings();
                        break;
                    case 2:
                        loadRentedListings();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadPendingListings() {
        String authHeader = tokenManager.getAuthHeader();
        if (authHeader == null) return;

        Call<ApiResponse<List<Room>>> call = apiInterface.getPendingProperties(authHeader);
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Room>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        recentProperties.clear();
                        recentProperties.addAll(apiResponse.getData());
                        propertyAdapter.updateProperties(recentProperties);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                Toast.makeText(DalaliDashboard.this, "Failed to load pending listings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRentedListings() {
        String authHeader = tokenManager.getAuthHeader();
        if (authHeader == null) return;

        Call<ApiResponse<List<Room>>> call = apiInterface.getRentedProperties(authHeader);
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Room>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        recentProperties.clear();
                        recentProperties.addAll(apiResponse.getData());
                        propertyAdapter.updateProperties(recentProperties);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                Toast.makeText(DalaliDashboard.this, "Failed to load rented listings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_add_listing) {
            startActivity(new Intent(this, PostRoomActivity.class));
        } else if (id == R.id.nav_my_listings) {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_messages) {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_earnings) {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        tokenManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}