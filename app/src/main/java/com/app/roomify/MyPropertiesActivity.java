package com.app.roomify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Collections;
import java.util.List;

public class MyPropertiesActivity extends AppCompatActivity {

    private static final String TAG = "MyProperties";

    private RecyclerView rvProperties;
    private ProgressBar progressBar;
    private TextView tvNoProperties, tvErrorMessage;
    private FloatingActionButton fabAddProperty;
    private ImageView ivBack;
    private TextView tvTitle;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
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

        rvProperties.setLayoutManager(new LinearLayoutManager(this));
        propertyList = new ArrayList<>();
        propertiesAdapter = new MyPropertiesAdapter(propertyList, this::onPropertyClick, this::onPropertyDelete);
        rvProperties.setAdapter(propertiesAdapter);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        Log.d(TAG, "Current User UID: " + currentUserId);

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

        Log.d(TAG, "Loading properties for user: " + currentUserId);

        // FIRST: Check ALL rooms to see what exists
        db.collection("rooms")
                .get()
                .addOnCompleteListener(allRoomsTask -> {
                    if (allRoomsTask.isSuccessful()) {
                        Log.d(TAG, "=== TOTAL ROOMS IN DATABASE: " + allRoomsTask.getResult().size());
                        for (QueryDocumentSnapshot doc : allRoomsTask.getResult()) {
                            String postedBy = doc.getString("postedBy");
                            Log.d(TAG, "Room ID: " + doc.getId() +
                                    ", Title: " + doc.getString("title") +
                                    ", postedBy: " + postedBy +
                                    ", Current User: " + currentUserId +
                                    ", Match: " + (currentUserId.equals(postedBy)));
                        }
                    }
                });

        // Now load only current user's properties
        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    propertyList.clear();

                    Log.d(TAG, "Query returned " + queryDocumentSnapshots.size() + " properties for user");

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoProperties.setVisibility(View.VISIBLE);
                        tvErrorMessage.setText("No properties found. Try posting one!");
                        tvErrorMessage.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No properties found for user: " + currentUserId);
                        propertiesAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Room room = document.toObject(Room.class);
                            room.setId(document.getId());

                            // Debug: Print all fields
                            Log.d(TAG, "Property found - ID: " + room.getId());
                            Log.d(TAG, "  Title: " + room.getTitle());
                            Log.d(TAG, "  postedBy field: " + document.getString("postedBy"));
                            Log.d(TAG, "  All data: " + document.getData());

                            propertyList.add(room);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document: " + e.getMessage());
                        }
                    }

                    // Sort by createdAt (newest first)
                    Collections.sort(propertyList, (r1, r2) ->
                            Long.compare(r2.getCreatedAt(), r1.getCreatedAt())
                    );

                    propertiesAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + propertyList.size() + " properties into adapter");

                    if (propertyList.isEmpty()) {
                        tvNoProperties.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading properties: ", e);
                    tvErrorMessage.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading bookings: ", e));
    }

    private void onPropertyClick(Room room) {
        Intent intent = new Intent(MyPropertiesActivity.this, RoomDetailsActivity.class);
        intent.putExtra("room_id", room.getId());
        startActivity(intent);
    }

    private void onPropertyDelete(Room room) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete \"" + room.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProperty(room))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProperty(Room room) {
        showLoading(true);

        db.collection("rooms")
                .document(room.getId())
                .collection("bookings")
                .get()
                .addOnSuccessListener(bookings -> {
                    for (QueryDocumentSnapshot booking : bookings) {
                        booking.getReference().delete();
                    }

                    db.collection("rooms")
                            .document(room.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(this, "Property deleted", Toast.LENGTH_SHORT).show();
                                loadProperties();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvProperties.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}