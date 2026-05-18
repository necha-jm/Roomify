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
import com.app.roomify.models.RegisterRequest;
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
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword,
            etBusinessName, etPhone, etLicenseNumber, etLocationArea;
    private MaterialButton btnRegister;
    private MaterialCardView ownerFieldsCard, dalaliFieldsCard;
    private Chip chipTenant, chipOwner, chipDalali;

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

        // Enable button by default
        btnRegister.setEnabled(true);
    }

    private void setupInitialization() {
        tokenManager = new TokenManager(this);
        APIClient.init(tokenManager);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etBusinessName = findViewById(R.id.etBusinessName);
        etPhone = findViewById(R.id.etPhone);
        etLicenseNumber = findViewById(R.id.etLicenseNumber);
        etLocationArea = findViewById(R.id.etLocationArea);
        btnRegister = findViewById(R.id.btnRegister);
        chipTenant = findViewById(R.id.chipTenant);
        chipOwner = findViewById(R.id.chipOwner);
        chipDalali = findViewById(R.id.chipDalali);
        tvLogin = findViewById(R.id.tvLogin);
        ownerFieldsCard = findViewById(R.id.ownerFieldsCard);
        dalaliFieldsCard = findViewById(R.id.dalaliFieldsCard);

        // Make sure button is clickable
        btnRegister.setClickable(true);
        btnRegister.setEnabled(true);
    }

    private void setupListener() {
        btnRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register button clicked. Selected role: " + selectedRole);
            registerUser();
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupRoleSelection() {
        chipTenant.setChecked(true);
        ownerFieldsCard.setVisibility(View.GONE);
        dalaliFieldsCard.setVisibility(View.GONE);

        chipTenant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "tenant";
                chipOwner.setChecked(false);
                chipDalali.setChecked(false);
                ownerFieldsCard.setVisibility(View.GONE);
                dalaliFieldsCard.setVisibility(View.GONE);
                Log.d(TAG, "Role changed to: TENANT");
            }
        });

        chipOwner.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "owner";
                chipTenant.setChecked(false);
                chipDalali.setChecked(false);
                ownerFieldsCard.setVisibility(View.VISIBLE);
                dalaliFieldsCard.setVisibility(View.GONE);
                Log.d(TAG, "Role changed to: OWNER");
            }
        });

        chipDalali.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "DALALI";
                chipTenant.setChecked(false);
                chipOwner.setChecked(false);
                ownerFieldsCard.setVisibility(View.GONE);
                dalaliFieldsCard.setVisibility(View.VISIBLE);
                Log.d(TAG, "Role changed to: DALALI");
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
        String licenseNumber = etLicenseNumber != null ? etLicenseNumber.getText().toString().trim() : "";
        String locationArea = etLocationArea != null ? etLocationArea.getText().toString().trim() : "";

        Log.d(TAG, "Attempting registration with role: " + selectedRole);
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Phone: " + phone);
        Log.d(TAG, "Location Area: " + locationArea);

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter name");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter Email");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Role-specific validation
        if (selectedRole.equals("owner")) {
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Phone number is required for owners");
                etPhone.requestFocus();
                return;
            }
        }

        if (selectedRole.equals("DALALI")) {
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Phone number is required for dalali");
                etPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(locationArea)) {
                etLocationArea.setError("Working location is required for dalali");
                etLocationArea.requestFocus();
                return;
            }
        }

        // Disable button during registration
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");
        btnRegister.setClickable(false);

        registerWithMySQL(name, email, password, selectedRole, businessName, phone, licenseNumber, locationArea);
    }

    private void registerWithMySQL(String name, String email, String password,
                                   String role, String businessName, String phone,
                                   String licenseNumber, String locationArea) {

        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        request.setBusinessName(businessName);
        request.setPhone(phone);

        // New fields for dalali
        if (role.equals("DALALI")) {
            request.setLicenseNumber(licenseNumber);
            request.setLocationArea(locationArea);
            request.setVerificationStatus("PENDING");
            Log.d(TAG, "Dalali registration with License: " + licenseNumber + ", Location: " + locationArea);
        }

        // Log the full request
        Log.d(TAG, "Sending registration request: " +
                "name=" + name +
                ", email=" + email +
                ", role=" + role +
                ", phone=" + phone);

        Call<AuthResponse> call = apiInterface.register(request);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                // Re-enable button
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                btnRegister.setClickable(true);

                Log.d(TAG, "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "Response success: " + authResponse.isSuccess());
                    Log.d(TAG, "Response message: " + authResponse.getMessage());

                    if (authResponse.isSuccess()) {
                        User user = authResponse.getUser();
                        if (user != null) {
                            if (authResponse.getToken() != null) {
                                tokenManager.saveToken(authResponse.getToken());
                            }
                            tokenManager.saveUser(user);

                            String roleMessage = role.equals("DALALI") ?
                                    "Registration successful! Your dalali account will be verified soon." :
                                    "Registration successful! Please login to continue.";

                            Toast.makeText(RegisterActivity.this, roleMessage, Toast.LENGTH_LONG).show();

                            new android.os.Handler().postDelayed(() -> {
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 2000);
                        } else {
                            Log.e(TAG, "User object is null in response");
                            Toast.makeText(RegisterActivity.this, "Registration failed: User data missing", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String message = authResponse.getMessage();
                        Log.e(TAG, "Registration failed: " + message);
                        Toast.makeText(RegisterActivity.this,
                                message != null ? message : "Registration failed",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = "Registration failed";
                    if (response.code() == 409) {
                        errorMsg = "Email already exists";
                    } else if (response.code() == 400) {
                        errorMsg = "Invalid input data";
                    } else if (response.code() == 500) {
                        errorMsg = "Server error. Please try again later";
                    }
                    Log.e(TAG, "HTTP Error: " + response.code() + " - " + errorMsg);

                    // Try to get error body
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Re-enable button
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                btnRegister.setClickable(true);

                Log.e(TAG, "Network error during registration", t);
                Toast.makeText(RegisterActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}