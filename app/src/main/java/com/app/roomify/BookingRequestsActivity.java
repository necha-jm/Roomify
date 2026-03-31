package com.app.roomify;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookingRequestsActivity extends AppCompatActivity {

    private static final String TAG = "BookingRequests";

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoRequests;
    private TextView tvErrorMessage;

    private String currentUserId;
    private String userRole;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_requests);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvNoRequests = findViewById(R.id.tvNoRequests);

        // Add error message TextView to layout if not present
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        if (tvErrorMessage == null) {
            tvErrorMessage = new TextView(this);
        }

        // Get user info
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        userRole = getIntent().getStringExtra("role");

        Log.d(TAG, "=== START ===");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "Role: " + userRole);

        // Check if user is logged in
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "User not logged in!");
            Toast.makeText(this, "Please login to view bookings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();

        // Load booking requests
        loadBookingRequests();
    }

    private void setupRecyclerView() {
        adapter = new BookingAdapter(new ArrayList<>(), this::onBookingAction);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadBookingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoRequests.setVisibility(View.GONE);

        if (tvErrorMessage != null) {
            tvErrorMessage.setVisibility(View.GONE);
        }

        if ("owner".equals(userRole)) {
            loadOwnerRequests();
        } else {
            loadUserRequests();
        }
    }

    private void loadUserRequests() {
        Log.d(TAG, "Loading user requests for: " + currentUserId);

        // Query user's bookings from users/{userId}/bookings
        db.collection("users")
                .document(currentUserId)
                .collection("bookings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        List<BookingRequest> requests = new ArrayList<>();
                        QuerySnapshot snapshot = task.getResult();

                        Log.d(TAG, "Found " + snapshot.size() + " bookings for user");

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            BookingRequest request = doc.toObject(BookingRequest.class);
                            if (request != null) {
                                request.setId(doc.getId());

                                // Ensure all required fields are set
                                if (request.getRoomId() == null || request.getRoomId().isEmpty()) {
                                    String roomIdFromDoc = doc.getString("roomId");
                                    if (roomIdFromDoc != null) {
                                        request.setRoomId(roomIdFromDoc);
                                    } else {
                                        Log.e(TAG, "Booking missing roomId: " + doc.getId());
                                        continue; // Skip this booking
                                    }
                                }

                                if (request.getRoomTitle() == null || request.getRoomTitle().isEmpty()) {
                                    request.setRoomTitle("Loading...");
                                    // Try to fetch room title
                                    fetchRoomTitle(request);
                                }

                                if (request.getStatus() == null || request.getStatus().isEmpty()) {
                                    request.setStatus("pending");
                                }

                                if (request.getTimestamp() == 0) {
                                    Long timestamp = doc.getLong("timestamp");
                                    if (timestamp != null) {
                                        request.setTimestamp(timestamp);
                                    } else {
                                        request.setTimestamp(System.currentTimeMillis());
                                    }
                                }

                                requests.add(request);
                                Log.d(TAG, "Added booking: " + request.getId() +
                                        " - Room: " + request.getRoomTitle() +
                                        " - Status: " + request.getStatus());
                            } else {
                                Log.w(TAG, "Failed to convert document to BookingRequest: " + doc.getId());
                            }
                        }

                        adapter.setRequests(requests);

                        if (requests.isEmpty()) {
                            tvNoRequests.setVisibility(View.VISIBLE);
                            tvNoRequests.setText("No booking requests found");
                        }

                    } else {
                        Log.e(TAG, "Error loading user bookings", task.getException());
                        Toast.makeText(this, "Error loading bookings: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText("Error loading bookings");
                    }
                });
    }

    private void fetchRoomTitle(BookingRequest request) {
        if (request.getRoomId() == null || request.getRoomId().isEmpty()) {
            return;
        }

        db.collection("rooms")
                .document(request.getRoomId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String title = doc.getString("title");
                        if (title != null && !title.isEmpty()) {
                            request.setRoomTitle(title);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch room title", e);
                });
    }

    private void loadOwnerRequests() {
        Log.d(TAG, "Loading owner requests for: " + currentUserId);

        // First, get all rooms owned by this user
        db.collection("rooms")
                .whereEqualTo("postedBy", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot roomSnapshot = task.getResult();
                        Log.d(TAG, "Found " + roomSnapshot.size() + " rooms owned");

                        if (roomSnapshot.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            tvNoRequests.setVisibility(View.VISIBLE);
                            tvNoRequests.setText("You don't own any rooms yet");
                            return;
                        }

                        adapter.clearRequests();

                        // Process each room's bookings
                        for (DocumentSnapshot roomDoc : roomSnapshot.getDocuments()) {
                            String roomId = roomDoc.getId();
                            String roomTitle = roomDoc.getString("title");

                            if (roomTitle == null || roomTitle.isEmpty()) {
                                roomTitle = "Unknown Room";
                            }

                            Log.d(TAG, "Checking bookings for room: " + roomId + " - " + roomTitle);

                            // Query bookings for this room
                            String finalRoomTitle = roomTitle;
                            db.collection("rooms")
                                    .document(roomId)
                                    .collection("bookings")
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .get()
                                    .addOnCompleteListener(bookingTask -> {
                                        if (bookingTask.isSuccessful()) {
                                            QuerySnapshot bookingSnapshot = bookingTask.getResult();
                                            Log.d(TAG, "Room " + roomId + " has " + bookingSnapshot.size() + " bookings");

                                            for (DocumentSnapshot bookingDoc : bookingSnapshot.getDocuments()) {
                                                // Try to get booking data
                                                Map<String, Object> bookingData = bookingDoc.getData();
                                                if (bookingData == null) {
                                                    Log.w(TAG, "Booking data is null for: " + bookingDoc.getId());
                                                    continue;
                                                }

                                                BookingRequest request = new BookingRequest();
                                                request.setId(bookingDoc.getId());
                                                request.setRoomId(roomId);
                                                request.setRoomTitle(finalRoomTitle);

                                                // Get user ID from booking
                                                String userId = (String) bookingData.get("userId");
                                                if (userId == null || userId.isEmpty()) {
                                                    Log.e(TAG, "Booking missing userId: " + bookingDoc.getId());
                                                    continue; // Skip this booking
                                                }
                                                request.setUserId(userId);

                                                // Get status
                                                String status = (String) bookingData.get("status");
                                                request.setStatus(status != null ? status : "pending");

                                                // Get timestamp
                                                Long timestamp = (Long) bookingData.get("timestamp");
                                                request.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());

                                                // Get booking date
                                                String bookingDate = (String) bookingData.get("bookingDate");
                                                request.setBookingDate(bookingDate != null ? bookingDate : "");

                                                // Now fetch user details
                                                fetchUserDetails(request, userId);
                                            }
                                        } else {
                                            Log.e(TAG, "Error loading bookings for room: " + roomId,
                                                    bookingTask.getException());
                                        }

                                        // Check if all rooms have been processed
                                        adapter.notifyDataSetChanged();
                                    });
                        }

                        // Hide progress bar after a delay
                        new android.os.Handler().postDelayed(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (adapter.getItemCount() == 0) {
                                tvNoRequests.setVisibility(View.VISIBLE);
                                tvNoRequests.setText("No booking requests for your rooms");
                            }
                        }, 2000);

                    } else {
                        progressBar.setVisibility(View.GONE);
                        Exception e = task.getException();
                        Log.e(TAG, "Error loading rooms", e);
                        Toast.makeText(this, "Error loading rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText("Error loading rooms");
                    }
                });
    }

    private void fetchUserDetails(BookingRequest request, String userId) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        // Get user name
                        String userName = userDoc.getString("name");
                        if (userName == null || userName.isEmpty()) {
                            userName = userDoc.getString("fullName");
                        }
                        if (userName == null || userName.isEmpty()) {
                            userName = "User";
                        }
                        request.setUserName(userName);

                        // Get user phone
                        String userPhone = userDoc.getString("phone");
                        if (userPhone == null || userPhone.isEmpty()) {
                            userPhone = userDoc.getString("contactPhone");
                        }
                        request.setUserPhone(userPhone != null ? userPhone : "");

                        // Add to adapter
                        adapter.addRequest(request);
                        Log.d(TAG, "Added booking with user details: " + userName + " - " + request.getRoomTitle());
                    } else {
                        Log.w(TAG, "User document not found for ID: " + userId);
                        request.setUserName("Unknown User");
                        adapter.addRequest(request);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user details", e);
                    request.setUserName("Unknown User");
                    adapter.addRequest(request);
                });
    }

    private void onBookingAction(BookingRequest request, String action) {
        // Validate request has required fields
        if (request == null) {
            Log.e(TAG, "onBookingAction: request is null");
            Toast.makeText(this, "Error: Invalid booking request", Toast.LENGTH_SHORT).show();
            return;
        }

        if (request.getId() == null || request.getId().isEmpty()) {
            Log.e(TAG, "onBookingAction: booking ID is null or empty");
            Toast.makeText(this, "Error: Invalid booking ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (request.getRoomId() == null || request.getRoomId().isEmpty()) {
            Log.e(TAG, "onBookingAction: room ID is null or empty");
            Toast.makeText(this, "Error: Invalid room ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            Log.e(TAG, "onBookingAction: user ID is null or empty");
            Toast.makeText(this, "Error: Cannot process booking - missing user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "onBookingAction: " + action + " - Booking ID: " + request.getId() +
                ", Room ID: " + request.getRoomId() +
                ", User ID: " + request.getUserId());

        switch (action) {
            case "accept":
                acceptBooking(request);
                break;
            case "reject":
                rejectBooking(request);
                break;
            case "cancel":
                cancelBooking(request);
                break;
            case "delete":
                deleteBooking(request);
                break;
        }
    }

    private void acceptBooking(BookingRequest request) {
        String roomId = request.getRoomId();
        String bookingId = request.getId();
        String userId = request.getUserId();

        Log.d(TAG, "Accepting booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Update in ROOM bookings collection
        db.collection("rooms")
                .document(roomId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking status updated to approved");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update room booking", e);
                });

        // Update in USER bookings collection
        db.collection("users")
                .document(userId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User booking status updated to approved");
                    Toast.makeText(this, "Booking accepted!", Toast.LENGTH_SHORT).show();
                    request.setStatus("approved");
                    adapter.notifyDataSetChanged();

                    // Send notification to user
                    sendNotificationToUser(userId, "Booking Approved",
                            "Your booking for " + request.getRoomTitle() + " has been approved!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update user booking", e);
                    Toast.makeText(this, "Error updating user booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectBooking(BookingRequest request) {
        String roomId = request.getRoomId();
        String bookingId = request.getId();
        String userId = request.getUserId();

        Log.d(TAG, "Rejecting booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Update in ROOM bookings collection
        db.collection("rooms")
                .document(roomId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking status updated to rejected");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update room booking", e);
                });

        // Update in USER bookings collection
        db.collection("users")
                .document(userId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User booking status updated to rejected");
                    Toast.makeText(this, "Booking rejected", Toast.LENGTH_SHORT).show();
                    request.setStatus("rejected");
                    adapter.notifyDataSetChanged();

                    // Send notification to user
                    sendNotificationToUser(userId, "Booking Rejected",
                            "Your booking for " + request.getRoomTitle() + " has been rejected.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update user booking", e);
                    Toast.makeText(this, "Error updating user booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelBooking(BookingRequest request) {
        String roomId = request.getRoomId();
        String bookingId = request.getId();
        String userId = request.getUserId();

        Log.d(TAG, "Cancelling booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Update in ROOM bookings collection
        db.collection("rooms")
                .document(roomId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking status updated to cancelled");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update room booking", e);
                });

        // Update in USER bookings collection
        db.collection("users")
                .document(userId)
                .collection("bookings")
                .document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User booking status updated to cancelled");
                    Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    adapter.removeRequest(request);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update user booking", e);
                    Toast.makeText(this, "Error updating user booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteBooking(BookingRequest request) {
        String roomId = request.getRoomId();
        String bookingId = request.getId();
        String userId = request.getUserId();

        Log.d(TAG, "Deleting booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Delete from ROOM bookings collection
        db.collection("rooms")
                .document(roomId)
                .collection("bookings")
                .document(bookingId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete room booking", e);
                });

        // Delete from USER bookings collection
        db.collection("users")
                .document(userId)
                .collection("bookings")
                .document(bookingId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User booking deleted");
                    Toast.makeText(this, "Booking deleted", Toast.LENGTH_SHORT).show();
                    adapter.removeRequest(request);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete user booking", e);
                    Toast.makeText(this, "Error deleting user booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToUser(String userId, String title, String message) {
        FirebaseUtils.sendNotificationToUser(userId, title, message, null);
    }
}