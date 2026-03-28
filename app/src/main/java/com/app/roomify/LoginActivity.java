package com.app.roomify;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    // Views
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton btnSignIn;
    private Chip chipTenantLogin, chipOwnerLogin;
    private TextView tvRoleHint;
    private MaterialButton guestButton, signUpButton;
    private SignInButton googleButton;
    private CardView cardView;
    private View loadingOverlay;

    private String selectedRole = "tenant"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            // Reload user to get latest verification status
            currentUser.reload().addOnCompleteListener(task -> {

                if (currentUser.isEmailVerified() || currentUser.isAnonymous()) {

                    // User already logged in → skip login screen
                    goToDashboard();

                } else {
                    // User exists but not verified
                    Toast.makeText(this,
                            "Please verify your email first",
                            Toast.LENGTH_SHORT).show();

                    mAuth.signOut(); // force login again
                }
            });
        }

        // Initialize views
        initViews();

        // Set up role selection
        setupRoleSelection();

        // Setup animations
        setupAnimations();

        // Configure Google Sign-In
        configureGoogleSignIn();

        // Setup click listeners
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
        loadingOverlay = findViewById(R.id.loadingOverlay); // Make sure you have this in your layout
    }

    private void setupRoleSelection() {
        // Set tenant as default checked
        chipTenantLogin.setChecked(true);
        selectedRole = "tenant";
        tvRoleHint.setText("You are logging in as a Tenant");

        // Tenant selected
        chipTenantLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "tenant";
                chipOwnerLogin.setChecked(false);
                tvRoleHint.setText("You are logging in as a Tenant");
                tvRoleHint.setTextColor(getColor(R.color.primary_green));
            }
        });

        // Owner selected
        chipOwnerLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRole = "owner";
                chipTenantLogin.setChecked(false);
                tvRoleHint.setText("You are logging in as an Owner");
                tvRoleHint.setTextColor(getColor(R.color.primary_green));
            }
        });
    }

    private void setupAnimations() {
        // Fade in animation for card view
        cardView.setAlpha(0f);
        cardView.setTranslationY(50f);
        cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .start();

        // Scale animation for logo
        View logo = findViewById(R.id.logo);
        if (logo != null) {
            Animation scaleAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            logo.startAnimation(scaleAnimation);
        }
    }

    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            googleSignInClient = GoogleSignIn.getClient(this, gso);

            // Customize Google Sign-In button
            googleButton.setSize(SignInButton.SIZE_WIDE);
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-In config error", e);
            Toast.makeText(this, "Error configuring Google Sign-In", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        // Sign In button
        btnSignIn.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(this::signInWithEmailAndPassword, 200);
        });

        // Guest button
        guestButton.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(this::signInAsGuest, 200);
        });

        // Sign Up button
        signUpButton.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 200);
        });

        // Google Sign-In button
        googleButton.setOnClickListener(v -> {
            animateButton(v);
            new Handler().postDelayed(this::signInWithGoogle, 200);
        });

        // Forgot password
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
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        button.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    }
                })
                .start();
    }

    private void signInWithEmailAndPassword() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
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

        // Show loading
        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Check if email is verified
                            if (user.isEmailVerified()) {
                                // Update emailVerified status in Firestore
                                updateEmailVerifiedStatus(user.getUid());
                                // Check user role before navigating
                                checkUserRoleAndNavigate(user.getUid());
                            } else {
                                // Email not verified - show message and sign out
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email address. A verification email has been sent to " + email,
                                        Toast.LENGTH_LONG).show();

                                // Resend verification email
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(LoginActivity.this,
                                                        "Verification email resent. Please check your inbox.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                mAuth.signOut();
                            }
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());

                        // Handle specific error cases
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage != null) {
                            if (errorMessage.contains("password")) {
                                passwordLayout.setError("Incorrect password");
                            } else if (errorMessage.contains("email")) {
                                emailLayout.setError("Email not found");
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Authentication failed: " + errorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void updateEmailVerifiedStatus(String userId) {
        db.collection("users").document(userId)
                .update("emailVerified", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Email verified status updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update email verified status", e);
                });
    }
    private void signInAsGuest() {
        showLoading(true);

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInAnonymously:success");
                        Toast.makeText(LoginActivity.this,
                                "Continuing as Guest",
                                Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Guest sign-in failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
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
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Failed to send reset email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            showLoading(false);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google Sign-In failed: No account", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign-In failed", e);
                Toast.makeText(this,
                        "Google Sign-In failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user exists in Firestore
                            checkUserExistsInFirestore(user);
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this,
                                "Authentication Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserExistsInFirestore(FirebaseUser user) {
        String userId = user.getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists, check role and navigate
                        String userRole = documentSnapshot.getString("role");
                        if (userRole != null) {
                            navigateBasedOnRole(userRole);
                        } else {
                            // No role assigned, go to registration to select role
                            goToRegistrationWithEmail(user.getEmail(), user.getDisplayName());
                        }
                    } else {
                        // New user, go to registration
                        goToRegistrationWithEmail(user.getEmail(), user.getDisplayName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user existence", e);
                    Toast.makeText(this, "Error checking user data", Toast.LENGTH_SHORT).show();
                    goToDashboard(); // Fallback to dashboard
                });
    }

    private void goToRegistrationWithEmail(String email, String name) {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        if (email != null) intent.putExtra("email", email);
        if (name != null) intent.putExtra("name", name);
        startActivity(intent);
        finish();
    }

    private void checkUserRoleAndNavigate(String userId) {
        showLoading(true);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        String userRole = documentSnapshot.getString("role");

                        // Validate selected role matches actual user role
                        if (userRole != null && userRole.equals(selectedRole)) {
                            // Role matches, navigate to appropriate activity
                            navigateBasedOnRole(userRole);
                        } else {
                            // Role mismatch
                            Toast.makeText(LoginActivity.this,
                                    "You selected " + selectedRole + " but your account is registered as " + userRole,
                                    Toast.LENGTH_LONG).show();

                            // Auto-correct the role selection
                            if ("owner".equals(userRole)) {
                                chipOwnerLogin.setChecked(true);
                            } else if ("tenant".equals(userRole)) {
                                chipTenantLogin.setChecked(true);
                            }
                        }
                    } else {
                        // User document not found
                        Toast.makeText(LoginActivity.this,
                                "User data not found. Please contact support.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error checking user role", e);
                    Toast.makeText(LoginActivity.this,
                            "Error checking user role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;

        if ("owner".equals(role)) {
            // Owner dashboard
            intent = new Intent(LoginActivity.this, ProfileActivity.class);
        } else {
            // Tenant/User dashboard
            intent = new Intent(LoginActivity.this, MainActivity.class);
        }

        intent.putExtra("role", role);
        intent.putExtra("userId", mAuth.getCurrentUser().getUid());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, LocationMap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Enable/disable buttons
        btnSignIn.setEnabled(!show);
        guestButton.setEnabled(!show);
        signUpButton.setEnabled(!show);
        googleButton.setEnabled(!show);
        chipTenantLogin.setEnabled(!show);
        chipOwnerLogin.setEnabled(!show);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in (anonymous users)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isAnonymous()) {
            goToDashboard();
        }
    }
}