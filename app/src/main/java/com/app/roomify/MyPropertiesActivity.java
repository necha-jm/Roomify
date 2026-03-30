package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyPropertiesActivity extends AppCompatActivity {

    // Views
    private RecyclerView rvProperties;
    private ProgressBar progressBar;
    private TextView tvNoProperties, tvErrorMessage;
    private FloatingActionButton fabAddProperty;
    private ImageView ivBack;
    private TextView tvTitle;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Adapter
    private MyPropertiesAdapter propertiesAdapter;
    private List<Room> propertyList;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_properties);

        initViews();
        setupFirebase();
        setupClickListeners();
        loadProperties();
    }

    private void initViews() {
        rvProperties = findViewById(R.id.rvProperties);
        progressBar = findViewById(R.id.progressBar);
        tvNoProperties = findViewById(R.id.tvNoProperties);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        fabAddProperty = findViewById(R.id.fabAddProperty);
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);

        // Setup RecyclerView
        rvProperties.setLayoutManager(new LinearLayoutManager(this));
        propertyList = new ArrayList<>();
        propertiesAdapter = new MyPropertiesAdapter(propertyList, this::onPropertyClick, this::onPropertyDelete);
        rvProperties.setAdapter(propertiesAdapter);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        fabAddProperty.setOnClickListener(v -> {
            Intent intent = new Intent(MyPropertiesActivity.this, PostRoomActivity.class);
            startActivity(intent);
        });

        ivBack.setOnClickListener(v -> finish());
    }

    private void loadProperties() {
        if (currentUserId == null) return;

        showLoading(true);
        tvNoProperties.setVisibility(View.GONE);
        tvErrorMessage.setVisibility(View.GONE);

        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    propertyList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Room room = document.toObject(Room.class);
                        room.setId(document.getId());

                        // Load bookings count for this room
                        loadBookingsCountForRoom(room);
                        propertyList.add(room);
                    }

                    propertiesAdapter.notifyDataSetChanged();

                    if (propertyList.isEmpty()) {
                        tvNoProperties.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    tvErrorMessage.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText("Error loading properties: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBookingsCountForRoom(Room room) {
        if (room.getId() == null) return;

        db.collection("rooms")
                .document(room.getId())
                .collection("bookings")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(bookings -> {
                    room.setBookingsCount(bookings.size());
                    propertiesAdapter.notifyDataSetChanged();
                });
    }

    private void onPropertyClick(Room room) {
        Intent intent = new Intent(MyPropertiesActivity.this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        startActivity(intent);
    }

    private void onPropertyDelete(Room room) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete \"" + room.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProperty(room))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProperty(Room room) {
        showLoading(true);

        // First, delete all bookings for this room
        db.collection("rooms")
                .document(room.getId())
                .collection("bookings")
                .get()
                .addOnSuccessListener(bookings -> {
                    // Delete each booking
                    for (QueryDocumentSnapshot booking : bookings) {
                        booking.getReference().delete();
                    }

                    // Then delete the room document
                    db.collection("rooms")
                            .document(room.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(this, "Property deleted successfully", Toast.LENGTH_SHORT).show();
                                loadProperties(); // Refresh list
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error deleting property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error deleting bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvProperties.setVisibility(show ? View.GONE : View.VISIBLE);

        if (!show && propertyList.isEmpty()) {
            tvNoProperties.setVisibility(View.VISIBLE);
        }
    }
}