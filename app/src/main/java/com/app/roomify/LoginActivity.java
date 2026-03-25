package com.app.roomify;

import static android.content.ContentValues.TAG;

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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton signInButton;
    private MaterialButton guestButton, signUpButton;
    private SignInButton googleButton;
    private CardView cardView;
    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();

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
        signInButton = findViewById(R.id.button2);
        guestButton = findViewById(R.id.Register);
        signUpButton = findViewById(R.id.Signup);
        googleButton = findViewById(R.id.Google);
        cardView = findViewById(R.id.cardView);

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

        // Scale animation for logo (add this if you have an ImageView for logo)
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
        signInButton.setOnClickListener(v -> {
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
                startActivity(new Intent(LoginActivity.this, LocationMap.class));
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
                            if (user.isEmailVerified()) {
                                Toast.makeText(LoginActivity.this,
                                        "Welcome back, " + user.getEmail(),
                                        Toast.LENGTH_SHORT).show();
                                goToDashboard();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email address. Verification email sent.",
                                        Toast.LENGTH_LONG).show();
                                user.sendEmailVerification();
                            }
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
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


    public void logoutUser() {

        // Show loading (optional)
        showLoading(true);

        // 1. Firebase logout
        if (mAuth != null) {
            mAuth.signOut();
        }

        // 2. Google logout (IMPORTANT)
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {

                showLoading(false);

                Toast.makeText(LoginActivity.this,
                        "Logged out successfully",
                        Toast.LENGTH_SHORT).show();

                // 3. Go back to Login screen
                Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            showLoading(false);

            // If Google not used
            Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
                            Toast.makeText(LoginActivity.this,
                                    "Welcome, " + user.getDisplayName(),
                                    Toast.LENGTH_SHORT).show();
                            goToDashboard();
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this,
                                "Authentication Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        // Enable/disable buttons
        signInButton.setEnabled(!show);
        guestButton.setEnabled(!show);
        signUpButton.setEnabled(!show);
        googleButton.setEnabled(!show);
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, LocationMap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            goToDashboard();
        }
    }


}

