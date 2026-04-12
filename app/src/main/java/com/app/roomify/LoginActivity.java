package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.app.roomify.models.AuthResponse;
import com.app.roomify.models.ForgotPasswordRequest;
import com.app.roomify.models.GoogleLoginRequest;
import com.app.roomify.models.LoginRequest;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "LoginActivity";

    // Backend components
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private GoogleSignInClient googleSignInClient;

    // UI Components
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton btnSignIn;
    private Chip chipTenantLogin, chipOwnerLogin;
    private TextView tvRoleHint;
    private MaterialButton guestButton, signUpButton;
    private SignInButton googleButton;
    private CardView cardView;
    private View loadingOverlay;

    private String selectedRole = "tenant";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize backend components
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Check if already logged in
        if (tokenManager.isLoggedIn()) {
            User user = tokenManager.getUser();
            if (user != null && user.getRole() != null) {
                navigateBasedOnRole(user.getRole());
                return;
            }
        }

        // Initialize views
        initViews();
        setupRoleSelection();
        setupAnimations();
        configureGoogleSignIn();
        setupClickListeners();
    }

    private void initViews() {
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.Email);
        passwordEditText = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.button2);
        chipTenantLogin = findViewById(R.id.chipTenantLogin);
        chipOwnerLogin = findViewById(R.id.chipOwnerLogin);
        tvRoleHint = findViewById(R.id.tvRoleHint);
        guestButton = findViewById(R.id.Register);
        signUpButton = findViewById(R.id.Signup);
        googleButton = findViewById(R.id.Google);
        cardView = findViewById(R.id.cardView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupRoleSelection() {
        chipTenantLogin.setChecked(true);
        selectedRole = "tenant";
        tvRoleHint.setText("You are logging in as a Tenant");

        chipTenantLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "tenant";
                chipOwnerLogin.setChecked(false);
                tvRoleHint.setText("You are logging in as a Tenant");
            }
        });

        chipOwnerLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "owner";
                chipTenantLogin.setChecked(false);
                tvRoleHint.setText("You are logging in as an Owner");
            }
        });
    }

    private void setupAnimations() {
        cardView.setAlpha(0f);
        cardView.setTranslationY(50f);
        cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .start();
    }

    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleButton.setSize(SignInButton.SIZE_WIDE);
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-In config error", e);
            Toast.makeText(this, "Error configuring Google Sign-In", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(this::signInWithEmailAndPassword, 200);
        });

        guestButton.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(this::signInAsGuest, 200);
        });

        signUpButton.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 200);
        });

        googleButton.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(this::signInWithGoogle, 200);
        });

        TextView forgotPassword = findViewById(R.id.forgotPassword);
        if (forgotPassword != null) {
            forgotPassword.setOnClickListener(v -> forgotPassword());
        }
    }

    private void animateButton(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void signInWithEmailAndPassword() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            emailLayout.requestFocus();
            return;
        } else {
            emailLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordLayout.requestFocus();
            return;
        } else {
            passwordLayout.setError(null);
        }

        showLoading(true);

        LoginRequest request = new LoginRequest(email, password, selectedRole);

        Call<AuthResponse> call = apiInterface.login(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.isSuccess()) {
                        User user = authResponse.getUser();

                        if (user != null) {
                            // Save JWT token and user data
                            tokenManager.saveToken(authResponse.getToken());
                            tokenManager.saveUser(user);

                            Log.d(TAG, "Login successful for user: " + user.getEmail());
                            navigateBasedOnRole(user.getRole());
                        } else {
                            Toast.makeText(LoginActivity.this, "User data is null", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String message = authResponse.getMessage();
                        if (message == null) message = "Authentication failed";

                        if (message.toLowerCase().contains("password")) {
                            passwordLayout.setError(message);
                        } else if (message.toLowerCase().contains("email")) {
                            emailLayout.setError(message);
                        } else {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    String errorMsg = "Authentication failed";
                    if (response.code() == 401) {
                        errorMsg = "Invalid email or password";
                    } else if (response.code() == 403) {
                        errorMsg = "Account locked or disabled";
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error", t);
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInAsGuest() {
        showLoading(true);

        Call<AuthResponse> call = apiInterface.guestLogin();
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.isSuccess()) {
                        User user = authResponse.getUser();

                        if (user != null) {
                            tokenManager.saveToken(authResponse.getToken());
                            tokenManager.saveUser(user);

                            Toast.makeText(LoginActivity.this, "Continuing as Guest", Toast.LENGTH_SHORT).show();
                            goToDashboard();
                        } else {
                            Toast.makeText(LoginActivity.this, "Guest sign-in failed", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, authResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Guest sign-in failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Guest sign-in failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        try {
            showLoading(true);
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "Google Sign-In error", e);
            Toast.makeText(this, "Error starting Google Sign-In", Toast.LENGTH_SHORT).show();
        }
    }

    private void forgotPassword() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            emailLayout.requestFocus();
            return;
        }

        showLoading(true);

        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        Call<AuthResponse> call = apiInterface.forgotPassword(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.isSuccess()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                authResponse.getMessage() != null ? authResponse.getMessage() : "Failed to send reset email",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send reset email", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    showLoading(false);
                    Toast.makeText(this, "Google Sign-In failed: No account", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                showLoading(false);
                Log.e(TAG, "Google Sign-In failed", e);
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        GoogleLoginRequest request = new GoogleLoginRequest(idToken, selectedRole);

        Call<AuthResponse> call = apiInterface.googleLogin(request);
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                showLoading(false);

                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response is successful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "Auth Response success: " + authResponse.isSuccess());
                    Log.d(TAG, "Auth Response message: " + authResponse.getMessage());

                    if (authResponse.isSuccess()) {
                        User user = authResponse.getUser();

                        if (user != null) {
                            Log.d(TAG, "User - ID: " + user.getId());
                            Log.d(TAG, "User - Email: " + user.getEmail());
                            Log.d(TAG, "User - Name: " + user.getName());
                            Log.d(TAG, "User - Role: " + user.getRole());

                            // Save token and user
                            tokenManager.saveToken(authResponse.getToken());
                            tokenManager.saveUser(user);

                            // Navigate based on role
                            String role = user.getRole();
                            Log.d(TAG, "Navigating with role: " + role);
                            navigateBasedOnRole(role);
                        } else {
                            Log.e(TAG, "User object is null!");
                            Toast.makeText(LoginActivity.this, "User data is null", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Auth failed: " + authResponse.getMessage());
                        Toast.makeText(LoginActivity.this, authResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "Response error - Code: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e(TAG, "Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(LoginActivity.this, "Google Sign-In failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network failure", t);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateBasedOnRole(String role) {
        Log.d(TAG, "navigateBasedOnRole called with role: " + role);

        Intent intent;

        if ("owner".equalsIgnoreCase(role)) {
            Log.d(TAG, "Navigating to OwnerDashboard");
            intent = new Intent(LoginActivity.this, OwnerDashboard.class);
        } else if ("tenant".equalsIgnoreCase(role)) {
            Log.d(TAG, "Navigating to DashboardActivity");
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
        } else {
            Log.d(TAG, "Unknown role, navigating to default Dashboard");
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
        }

        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, LocationMap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        btnSignIn.setEnabled(!show);
        guestButton.setEnabled(!show);
        signUpButton.setEnabled(!show);
        googleButton.setEnabled(!show);
        chipTenantLogin.setEnabled(!show);
        chipOwnerLogin.setEnabled(!show);
    }
}