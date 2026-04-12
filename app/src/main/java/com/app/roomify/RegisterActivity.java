package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.app.roomify.models.AuthResponse;
import com.app.roomify.models.RegisterRequest;  // CORRECT import
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextView tvLogin;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword, etBusinessName, etPhone;
    private MaterialButton btnRegister;
    private MaterialCardView ownerFieldsCard;
    private Chip chipTenant, chipOwner;

    // MySQL Backend components
    private APIInterface apiInterface;
    private TokenManager tokenManager;

    private String selectedRole = "tenant";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        setupInitialization();
        setupListener();
        setupRoleSelection();
    }

    private void setupInitialization() {
        // Initialize MySQL backend
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Initialize Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etBusinessName = findViewById(R.id.etBusinessName);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);
        chipTenant = findViewById(R.id.chipTenant);
        chipOwner = findViewById(R.id.chipOwner);
        tvLogin = findViewById(R.id.tvLogin);
        ownerFieldsCard = findViewById(R.id.ownerFieldsCard);
    }

    private void setupListener() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupRoleSelection() {
        chipTenant.setChecked(true);

        chipTenant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "tenant";
                chipOwner.setChecked(false);
                ownerFieldsCard.setVisibility(View.GONE);
            }
        });

        chipOwner.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "owner";
                chipTenant.setChecked(false);
                ownerFieldsCard.setVisibility(View.VISIBLE);
            }
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String businessName = etBusinessName != null ? etBusinessName.getText().toString().trim() : "";
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter name");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter Email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (selectedRole.equals("owner")) {
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Phone number is required for owners");
                return;
            }
        }

        // Disable button and show loading
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // Register with MySQL backend
        registerWithMySQL(name, email, password, selectedRole, businessName, phone);
    }

    private void registerWithMySQL(String name, String email, String password,
                                   String role, String businessName, String phone) {

        // Create RegisterRequest object using setters (safer approach)
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        request.setBusinessName(businessName);
        request.setPhone(phone);

        Call<AuthResponse> call = apiInterface.register(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.isSuccess()) {
                        User user = authResponse.getUser();

                        if (user != null) {
                            // Save token and user data
                            if (authResponse.getToken() != null) {
                                tokenManager.saveToken(authResponse.getToken());
                            }
                            tokenManager.saveUser(user);

                            Log.d(TAG, "Registration successful for user: " + user.getEmail());

                            Toast.makeText(RegisterActivity.this,
                                    "Registration successful! Please login to continue.",
                                    Toast.LENGTH_LONG).show();

                            // Navigate to login after delay
                            new android.os.Handler().postDelayed(() -> {
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 2000);
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Registration failed: User data is null",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String message = authResponse.getMessage();
                        if (message == null) message = "Registration failed";
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                        // Handle specific error cases
                        if (message.toLowerCase().contains("email")) {
                            etEmail.setError(message);
                        } else if (message.toLowerCase().contains("password")) {
                            etPassword.setError(message);
                        }
                    }
                } else {
                    // Handle error response
                    String errorMsg = "Registration failed";
                    if (response.code() == 409) {
                        errorMsg = "Email already exists";
                    } else if (response.code() == 400) {
                        errorMsg = "Invalid input data";
                    } else if (response.code() == 500) {
                        errorMsg = "Server error. Please try again later";
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Registration error - Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");

                Log.e(TAG, "Network error during registration", t);
                Toast.makeText(RegisterActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}