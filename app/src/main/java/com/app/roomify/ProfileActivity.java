package com.app.roomify;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final String TAG = "ProfileActivity";

    // Views
    private BottomNavigationView bottom_nav;
    private LinearLayout logout;
    private TextView profileName, profileEmail, profileRole, profileMemberSince;
    private TextView tvBookingsCount, tvPropertiesCount, tvReviewsCount;
    private ImageView profileImage;
    private Button request;
    private FloatingActionButton fabAddPhoto;

    // Backend Components
    private TokenManager tokenManager;
    private APIInterface apiInterface;
    private User currentUser;
    private long currentUserId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        initialization();
        loadUserProfile();
        listener();
        loadUserStats();
    }

    private void initialization() {
        // Initialize backend components
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Get current user
        currentUser = tokenManager.getUser();
        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        // Initialize views
        logout = findViewById(R.id.logout);
        bottom_nav = findViewById(R.id.bottom_nav);
        profileName = findViewById(R.id.profileName);
        profileImage = findViewById(R.id.profileImage);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        request = findViewById(R.id.request);

        // Stats views
        tvBookingsCount = findViewById(R.id.tvBookingsCount);


        // Set default bottom navigation selection
        if (bottom_nav != null) {
            bottom_nav.setSelectedItemId(R.id.tab_profile);
        }
    }

    private void loadUserProfile() {
        if (currentUser != null) {
            displayUserInfo(currentUser);
        } else if (tokenManager.isLoggedIn()) {
            fetchUserFromServer();
        } else {
            showDefaultUserInfo();
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserFromServer() {
        String token = tokenManager.getToken();
        if (token == null) {
            showDefaultUserInfo();
            return;
        }

        Call<com.app.roomify.models.AuthResponse> call = apiInterface.getCurrentUser("Bearer " + token);
        call.enqueue(new Callback<com.app.roomify.models.AuthResponse>() {
            @Override
            public void onResponse(Call<com.app.roomify.models.AuthResponse> call,
                                   Response<com.app.roomify.models.AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.app.roomify.models.AuthResponse authResponse = response.body();
                    if (authResponse.isSuccess() && authResponse.getUser() != null) {
                        currentUser = authResponse.getUser();
                        currentUserId = currentUser.getId();
                        tokenManager.saveUser(currentUser);
                        displayUserInfo(currentUser);
                    } else {
                        showDefaultUserInfo();
                    }
                } else {
                    showDefaultUserInfo();
                }
            }

            @Override
            public void onFailure(Call<com.app.roomify.models.AuthResponse> call, Throwable t) {
                showDefaultUserInfo();
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserInfo(User user) {
        if (user == null) {
            showDefaultUserInfo();
            return;
        }

        // Display name
        String name = user.getName();
        if (name != null && !name.isEmpty()) {
            profileName.setText(name);
        } else {
            String email = user.getEmail();
            profileName.setText(email != null ? email.split("@")[0] : "User");
        }

        // Display email
        if (profileEmail != null) {
            String email = user.getEmail();
            profileEmail.setText(email != null && !email.isEmpty() ? email : "No email provided");
            profileEmail.setVisibility(View.VISIBLE);
        }

        // Display role
        if (profileRole != null) {
            String role = user.getRole();
            if (role != null && !role.isEmpty()) {
                String displayRole = role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
                profileRole.setText(displayRole);
                profileRole.setVisibility(View.VISIBLE);

                if ("owner".equalsIgnoreCase(role)) {
                    profileRole.setBackgroundResource(R.drawable.role_badge_owner);
                } else {
                    profileRole.setBackgroundResource(R.drawable.role_badge_tenant);
                }
            } else {
                profileRole.setVisibility(View.GONE);
            }
        }

        // Member since
        if (profileMemberSince != null) {
            profileMemberSince.setVisibility(View.GONE);
        }

        // FIXED: Load saved image instead of always default
        if (profileImage != null) {
            String savedImage = getSharedPreferences("profile", MODE_PRIVATE)
                    .getString("image_uri", null);

            if (savedImage != null) {
                Glide.with(this)
                        .load(Uri.parse(savedImage))
                        .placeholder(R.drawable.ic_profile)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void showDefaultUserInfo() {
        profileName.setText("User");
        if (profileEmail != null) {
            profileEmail.setText("Not logged in");
            profileEmail.setVisibility(View.VISIBLE);
        }
        if (profileRole != null) profileRole.setVisibility(View.GONE);
        if (profileMemberSince != null) profileMemberSince.setVisibility(View.GONE);
        if (profileImage != null) profileImage.setImageResource(R.drawable.ic_profile);
    }

    private void loadUserStats() {
        if (currentUserId == -1) {
            setDefaultStats();
            return;
        }

        // Load bookings count
        Call<com.app.roomify.models.ApiResponse<java.util.List<com.app.roomify.models.BookingResponse>>> bookingsCall =
                apiInterface.getUserBookings(currentUserId);
        bookingsCall.enqueue(new Callback<com.app.roomify.models.ApiResponse<java.util.List<com.app.roomify.models.BookingResponse>>>() {
            @Override
            public void onResponse(Call<com.app.roomify.models.ApiResponse<java.util.List<com.app.roomify.models.BookingResponse>>> call,
                                   Response<com.app.roomify.models.ApiResponse<java.util.List<com.app.roomify.models.BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    java.util.List<com.app.roomify.models.BookingResponse> bookings = response.body().getData();
                    int count = bookings != null ? bookings.size() : 0;
                    if (tvBookingsCount != null) {
                        tvBookingsCount.setText(String.valueOf(count));
                    }
                } else {
                    setDefaultStats();
                }
            }

            @Override
            public void onFailure(Call<com.app.roomify.models.ApiResponse<java.util.List<com.app.roomify.models.BookingResponse>>> call, Throwable t) {
                setDefaultStats();
            }
        });

        // Load properties count (only for owners)
        if (currentUser != null && "owner".equals(currentUser.getRole())) {
            Call<java.util.List<Room>> roomsCall = apiInterface.getRoomsByOwner(currentUserId);
            roomsCall.enqueue(new Callback<java.util.List<Room>>() {
                @Override
                public void onResponse(Call<java.util.List<Room>> call, Response<java.util.List<Room>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        int count = response.body().size();
                        if (tvPropertiesCount != null) {
                            tvPropertiesCount.setText(String.valueOf(count));
                        }
                    }
                }

                @Override
                public void onFailure(Call<java.util.List<Room>> call, Throwable t) {
                    if (tvPropertiesCount != null) tvPropertiesCount.setText("0");
                }
            });
        } else {
            if (tvPropertiesCount != null) tvPropertiesCount.setText("0");
        }
    }

    private void setDefaultStats() {
        if (tvBookingsCount != null) tvBookingsCount.setText("0");
        if (tvPropertiesCount != null) tvPropertiesCount.setText("0");
        if (tvReviewsCount != null) tvReviewsCount.setText("0");
    }

    private void listener() {
        // Bottom navigation
        if (bottom_nav != null) {
            bottom_nav.setOnItemSelectedListener(menuItem -> {
                int itemId = menuItem.getItemId();

                if (itemId == R.id.tab_menu) {
                    startActivity(new Intent(this, LocationMap.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_explore) {
                    // Navigate to dashboard based on role
                    if (currentUser != null && "owner".equals(currentUser.getRole())) {
                        startActivity(new Intent(this, OwnerDashboard.class));
                    } else {
                        startActivity(new Intent(this, DashboardActivity.class));
                    }
                    return true;
                } else if (itemId == R.id.tab_profile) {
                    // Already on profile
                    return true;
                }
                return false;
            });
        }

        // Logout button
        if (logout != null) {
            logout.setOnClickListener(v -> {
                // Clear session
                tokenManager.clear();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Add photo button
        if (fabAddPhoto != null) {
            fabAddPhoto.setOnClickListener(v -> openGallery());
        }

        // Request button - navigate to booking requests
        if (request != null) {
            request.setOnClickListener(v -> {
                Intent intent = new Intent(this, BookingRequestsActivity.class);
                if (currentUser != null && "owner".equals(currentUser.getRole())) {
                    intent.putExtra("role", "owner");
                } else {
                    intent.putExtra("role", "tenant");
                }
                startActivity(intent);
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // SAVE the image URI
            getSharedPreferences("profile", MODE_PRIVATE)
                    .edit()
                    .putString("image_uri", imageUri.toString())
                    .apply();

            // Use Glide (better than setImageURI)
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_profile)
                    .into(profileImage);

            Toast.makeText(this, "Profile image updated locally", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data
        loadUserProfile();
        loadUserStats();
        if (bottom_nav != null) {
            bottom_nav.setSelectedItemId(R.id.tab_profile);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}