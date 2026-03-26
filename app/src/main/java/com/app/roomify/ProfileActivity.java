package com.app.roomify;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private BottomNavigationView bottom_nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        initialization();

        //listner action
        listerner();

    }

    private void initialization(){
//firebase
        mAuth = FirebaseAuth.getInstance();


        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        bottom_nav = findViewById(R.id.bottom_nav);


    }

    private void listerner(){
        //item

        bottom_nav.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.tab_menu) {
                Intent i = new Intent(this, LocationMap.class);
                startActivity(i);
                return true;
            }
            else if (itemId == R.id.nav_explore) {
                // You were opening ProfileActivity again - fix this
                Intent intent = new Intent(this, RoomDetailsActivity.class); // Change to your Explore activity
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.tab_profile) {
                // Already on ProfileActivity, don't restart it
                // Just return true to indicate it's handled
                return true;
            }

            return false;
        });

    }
}