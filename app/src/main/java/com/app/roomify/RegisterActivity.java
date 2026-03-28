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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextView tvLogin;

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword, etBusinessName, etPhone;
    private MaterialButton btnRegister;

    private MaterialCardView ownerFieldsCard;

    private Chip chipTenant, chipOwner;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedRole = "tenant"; // Default role

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
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
        // Register button
        btnRegister.setOnClickListener(v -> registerUser());

        // Go to login
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupRoleSelection() {
        // Set tenant as default checked
        chipTenant.setChecked(true);

        // Tenant selected
        chipTenant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "tenant";
                chipOwner.setChecked(false);
                ownerFieldsCard.setVisibility(View.GONE);
            }
        });

        // Owner selected
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

        // Validate owner-specific fields
        if (selectedRole.equals("owner")) {
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Phone number is required for owners");
                return;
            }
        }

        // Disable button and show loading
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // Create user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            Log.d("Register", "User created successfully: " + user.getUid());

                            // Update display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d("Register", "User profile updated");
                                        }
                                    });

                            // Send email verification - DON'T sign out yet
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Log.d("Register", "Verification email sent successfully to: " + email);

                                            // Save user data to Firestore
                                            saveUserToFirestore(user.getUid(), name, email, selectedRole, businessName, phone);
                                        } else {
                                            // Failed to send verification email
                                            Log.e("Register", "Failed to send verification email", verifyTask.getException());
                                            btnRegister.setEnabled(true);
                                            btnRegister.setText("Register");
                                            Toast.makeText(RegisterActivity.this,
                                                    "Failed to send verification email: " + verifyTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();

                                            // Delete the created user since verification failed
                                            user.delete().addOnCompleteListener(deleteTask -> {
                                                if (deleteTask.isSuccessful()) {
                                                    Log.d("Register", "User deleted due to verification failure");
                                                }
                                            });
                                        }
                                    });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                        Log.e("Register", "Registration failed", task.getException());
                        Toast.makeText(RegisterActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String role,
                                     String businessName, String phone) {
        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("emailVerified", false);

        // Add role-specific fields
        if (role.equals("owner")) {
            userData.put("businessName", businessName != null ? businessName : "");
            userData.put("phone", phone != null ? phone : "");
        } else {
            userData.put("phone", phone != null ? phone : "");
        }

        // Save to Firestore
        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Register", "User data saved to Firestore");

                    // Show success message
                    Toast.makeText(RegisterActivity.this,
                            "Registration successful! Please check your email to verify your account.\n\nIf you don't see the email, check your spam folder.",
                            Toast.LENGTH_LONG).show();

                    // DON'T sign out yet - let the user see the message
                    // Navigate to Login after a delay
                    new android.os.Handler().postDelayed(() -> {
                        // Sign out only before navigating
                        mAuth.signOut();

                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, 3000); // 3 seconds delay
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    Log.e("Register", "Failed to save to Firestore", e);
                    Toast.makeText(RegisterActivity.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // If Firestore save fails, delete the Firebase Auth user
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.delete();
                    }
                });
    }
}