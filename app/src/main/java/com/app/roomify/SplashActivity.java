package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    private LottieAnimationView lottieAnimationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Lottie view
        lottieAnimationView = findViewById(R.id.lottieAnimation);

        // Optional: Programmatically set animation (if not set in XML)
        // lottieAnimationView.setAnimation(R.raw.splash_anim);

        // Optional: Add animation listener
        lottieAnimationView.addAnimatorUpdateListener(animation -> {
            // You can track animation progress here
            float progress = animation.getAnimatedFraction();
            // Example: Fade in text based on progress
        });

        // Navigate after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainMapActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 6000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up animation to prevent memory leaks
        if (lottieAnimationView != null) {
            lottieAnimationView.cancelAnimation();
        }
    }
}
