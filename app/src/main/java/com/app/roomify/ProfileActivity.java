package com.app.roomify;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private BottomNavigationView bottom_nav;

    private LinearLayout logout;

    private TextView profileName;

    private ImageView profileImage;

    private Button request;
    private FloatingActionButton fabAddPhoto;
    private static final int PICK_IMAGE = 1;

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

        logout = findViewById(R.id.logout);


        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        bottom_nav = findViewById(R.id.bottom_nav);
        profileName = findViewById(R.id.profileName);
        profileImage = findViewById(R.id.profileImage);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        request = findViewById(R.id.request);


        if (mAuth.getCurrentUser() != null) {
            String name = mAuth.getCurrentUser().getDisplayName();
            String email = mAuth.getCurrentUser().getEmail();

            if (name != null && !name.isEmpty()) {
                profileName.setText(name); // Google users
            } else if (email != null) {
                profileName.setText(email); // Email users fallback
            } else {
                profileName.setText("Guest User");
            }
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
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

        if(logout != null){
            logout.setOnClickListener(v -> {
                AuthManager.logoutUser(this, mAuth, googleSignInClient);

            });
        }

        fabAddPhoto.setOnClickListener(v -> openGallery());
        request.setOnClickListener(v -> {
            // In your owner dashboard
            Intent userIntent = new Intent(this, BookingRequestsActivity.class);
            userIntent.putExtra("role", "tenant");
            startActivity(userIntent);
        });

    }

    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }
}