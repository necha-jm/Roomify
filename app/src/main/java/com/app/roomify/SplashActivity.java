package com.app.roomify;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.app.roomify.models.AuthResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private LottieAnimationView lottieAnimationView;

    // Backend Components
    private TokenManager tokenManager;
    private APIInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        lottieAnimationView = findViewById(R.id.lottieAnimation);

        // Initialize backend components
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Wait 3 seconds for splash animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Check internet connection
            if (!isInternetAvailable()) {
                startActivity(new Intent(this, NoInternetActivity.class));
                finish();
                return;
            }

            // Check if user is logged in
            if (tokenManager.isLoggedIn()) {
                // User has token, validate it with backend
                validateTokenAndNavigate();
            } else {
                // No token, go to login
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }

        }, 3000); // 3 seconds is enough
    }

    private void validateTokenAndNavigate() {
        String token = tokenManager.getToken();
        User savedUser = tokenManager.getUser();

        if (token == null || token.isEmpty()) {
            // No valid token, go to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Validate token with backend
        Call<AuthResponse> call = apiInterface.testToken("Bearer " + token);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.isSuccess()) {
                        // Token is valid, get user info
                        User user = authResponse.getUser();
                        if (user != null) {
                            // Update stored user info
                            tokenManager.saveUser(user);

                            // Navigate based on role
                            navigateBasedOnRole(user);
                        } else if (savedUser != null) {
                            // Use saved user if response doesn't contain user
                            navigateBasedOnRole(savedUser);
                        } else {
                            // No user info, go to login
                            goToLogin();
                        }
                    } else {
                        // Token invalid or expired
                        Log.w(TAG, "Token validation failed: " + authResponse.getMessage());
                        tokenManager.clear();
                        goToLogin();
                    }
                } else {
                    // Server error, try to use saved user data
                    Log.e(TAG, "Token validation server error: " + response.code());
                    if (savedUser != null) {
                        navigateBasedOnRole(savedUser);
                    } else {
                        goToLogin();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Network error, try to use saved user data
                Log.e(TAG, "Token validation network error: " + t.getMessage());
                if (savedUser != null) {
                    navigateBasedOnRole(savedUser);
                } else {
                    goToLogin();
                }
            }
        });
    }

    private void navigateBasedOnRole(User user) {
        if (user == null) {
            goToLogin();
            return;
        }

        String role = user.getRole();
        Log.d(TAG, "User role: " + role);

        // Check if email is verified (if your backend supports this)
        // For now, we'll assume all registered users are verified
        // You can add email verification check if your backend has it

        if ("owner".equalsIgnoreCase(role)) {
            // Owner goes to OwnerDashboard
            Intent intent = new Intent(this, OwnerDashboard.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if ("tenant".equalsIgnoreCase(role)) {
            // Tenant goes to LocationMap
            Intent intent = new Intent(this, LocationMap.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if ("guest".equalsIgnoreCase(role)) {
            // Guest goes to LocationMap
            Intent intent = new Intent(this, LocationMap.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            // Unknown role, go to login
            goToLogin();
        }

        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToGuestMode() {
        // For guest users, you might want to create a guest session
        // Or just go to LocationMap without login
        Intent intent = new Intent(this, LocationMap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lottieAnimationView != null) {
            lottieAnimationView.cancelAnimation();
        }
    }



    // Internet checker
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}