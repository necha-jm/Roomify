package com.app.roomify;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class AuthManager {

    // Static method so you can call it from any Activity
    public static void logoutUser(Activity activity, FirebaseAuth mAuth, GoogleSignInClient googleSignInClient) {

        // Firebase sign out
        if (mAuth != null) mAuth.signOut();

        // Google sign out
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
                Toast.makeText(activity, "Logged out successfully", Toast.LENGTH_SHORT).show();
                redirectToLogin(activity);
            });
        } else {
            redirectToLogin(activity);
        }
    }

    private static void redirectToLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}