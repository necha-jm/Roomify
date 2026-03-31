package com.app.roomify;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        lottieAnimationView = findViewById(R.id.lottieAnimation);

        // Wait 6 seconds for splash animation
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (!isInternetAvailable()) {
                startActivity(new Intent(this, NoInternetActivity.class));
                finish();
                return;
            }

            if (user != null) {

                user.reload().addOnCompleteListener(task -> {

                    if (user.isAnonymous()) {
                        // Guest → go to map
                        startActivity(new Intent(this, LocationMap.class));
                        finish();
                        return;
                    }

                    if (!user.isEmailVerified()) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    // NOW CHECK ROLE FROM FIRESTORE
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {

                                if (documentSnapshot.exists()) {

                                    String role = documentSnapshot.getString("role");

                                    if ("owner".equals(role)) {
                                        startActivity(new Intent(this, OwnerDashboard.class));
                                    } else {
                                        startActivity(new Intent(this, LocationMap.class));
                                    }

                                } else {
                                    // No user data → force login
                                    FirebaseAuth.getInstance().signOut();
                                    startActivity(new Intent(this, LoginActivity.class));
                                }

                                finish();
                            });

                });

            } else {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }

        }, 3000);
       // 3 seconds is enough
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

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        NetworkCapabilities nc =
                cm.getNetworkCapabilities(cm.getActiveNetwork());

        return nc != null &&
                nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}