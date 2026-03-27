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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookingRequestsActivity extends AppCompatActivity {

    private static final String TAG = "BookingRequests";

    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoRequests;
    private TextView tvErrorMessage;

    private String currentUserId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_requests);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvNoRequests = findViewById(R.id.tvNoRequests);

        // Add error message TextView to layout if not present
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        if (tvErrorMessage == null) {
            // If no error TextView, create a reference
            tvErrorMessage = new TextView(this);
        }

        // Get user info
        currentUserId = FirebaseUtils.getCurrentUserId();
        userRole = getIntent().getStringExtra("role");

        Log.d(TAG, "=== START ===");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "Role: " + userRole);

        // Check if user is logged in
        if (currentUserId == null) {
            Log.e(TAG, "User not logged in!");
            Toast.makeText(this, "Please login to view bookings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();

        // Test Firestore connection first
        testFirestoreConnection();
    }

    private void testFirestoreConnection() {
        Log.d(TAG, "Testing Firestore connection...");

        FirebaseFirestore.getInstance()
                .collection("rooms")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Firestore connection successful!");
                        Log.d(TAG, "Found " + task.getResult().size() + " rooms");
                        loadBookingRequests();
                    } else {
                        Log.e(TAG, "❌ Firestore connection failed!", task.getException());
                        progressBar.setVisibility(View.GONE);
                        if (tvErrorMessage != null) {
                            tvErrorMessage.setVisibility(View.VISIBLE);
                            tvErrorMessage.setText("Connection error: " + task.getException().getMessage());
                        }
                        Toast.makeText(this, "Firestore connection failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("bookings")
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

                                // Ensure roomId is set (might be missing in some documents)
                                if (request.getRoomId() == null || request.getRoomId().isEmpty()) {
                                    String roomIdFromDoc = doc.getString("roomId");
                                    if (roomIdFromDoc != null) {
                                        request.setRoomId(roomIdFromDoc);
                                    }
                                }

                                // Ensure roomTitle is set
                                if (request.getRoomTitle() == null || request.getRoomTitle().isEmpty()) {
                                    String roomTitleFromDoc = doc.getString("roomTitle");
                                    if (roomTitleFromDoc != null) {
                                        request.setRoomTitle(roomTitleFromDoc);
                                    } else {
                                        request.setRoomTitle("Unknown Room");
                                    }
                                }

                                requests.add(request);
                                Log.d(TAG, "Added booking: " + request.getId() + " - Room: " + request.getRoomTitle());
                            } else {
                                Log.w(TAG, "Failed to convert document to BookingRequest: " + doc.getId());
                            }
                        }

                        adapter.setRequests(requests);

                        if (requests.isEmpty()) {
                            tvNoRequests.setVisibility(View.VISIBLE);
                            tvNoRequests.setText("No bookings found");
                        }

                    } else {
                        Log.e(TAG, "Error loading user bookings", task.getException());
                        Toast.makeText(this, "Error: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText("Error loading bookings");
                    }
                });
    }

    private void loadOwnerRequests() {
        Log.d(TAG, "Loading owner requests for: " + currentUserId);

        FirebaseFirestore.getInstance()
                .collection("rooms")
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

                        // Clear adapter before adding new requests
                        adapter.clearRequests();

                        // Track how many rooms we've processed
                        final int[] processedRooms = {0};
                        final int totalRooms = roomSnapshot.size();

                        for (DocumentSnapshot roomDoc : roomSnapshot.getDocuments()) {
                            String roomId = roomDoc.getId();
                            String roomTitle = roomDoc.getString("title");

                            if (roomTitle == null || roomTitle.isEmpty()) {
                                roomTitle = "Unknown Room";
                            }

                            Log.d(TAG, "Checking room: " + roomId + " - " + roomTitle);

                            String finalRoomTitle = roomTitle;
                            roomDoc.getReference()
                                    .collection("bookings")
                                    .get()
                                    .addOnCompleteListener(bookingTask -> {
                                        processedRooms[0]++;

                                        if (bookingTask.isSuccessful()) {
                                            QuerySnapshot bookingSnapshot = bookingTask.getResult();
                                            Log.d(TAG, "Room " + roomId + " has " + bookingSnapshot.size() + " bookings");

                                            for (DocumentSnapshot bookingDoc : bookingSnapshot.getDocuments()) {
                                                // Try to convert to BookingRequest first
                                                BookingRequest request = bookingDoc.toObject(BookingRequest.class);

                                                if (request == null) {
                                                    // If conversion fails, create manually
                                                    Log.w(TAG, "Failed to convert booking doc, creating manually: " + bookingDoc.getId());
                                                    request = new BookingRequest();

                                                    // Manually set fields from document
                                                    request.setId(bookingDoc.getId());
                                                    request.setRoomId(roomId);
                                                    request.setRoomTitle(finalRoomTitle);

                                                    // Get userId from document
                                                    String userId = bookingDoc.getString("userId");
                                                    if (userId != null) {
                                                        request.setUserId(userId);
                                                    } else {
                                                        Log.e(TAG, "Booking missing userId: " + bookingDoc.getId());
                                                    }

                                                    // Get userName from document
                                                    String userName = bookingDoc.getString("userName");
                                                    if (userName != null) {
                                                        request.setUserName(userName);
                                                    } else {
                                                        request.setUserName("Unknown User");
                                                    }

                                                    // Get userPhone from document
                                                    String userPhone = bookingDoc.getString("userPhone");
                                                    if (userPhone != null) {
                                                        request.setUserPhone(userPhone);
                                                    }

                                                    // Get status from document
                                                    String status = bookingDoc.getString("status");
                                                    if (status != null) {
                                                        request.setStatus(status);
                                                    } else {
                                                        request.setStatus("pending");
                                                    }

                                                    // Get timestamp from document
                                                    Long timestamp = bookingDoc.getLong("timestamp");
                                                    if (timestamp != null) {
                                                        request.setTimestamp(timestamp);
                                                    }

                                                    // Get bookingDate from document
                                                    String bookingDate = bookingDoc.getString("bookingDate");
                                                    if (bookingDate != null) {
                                                        request.setBookingDate(bookingDate);
                                                    }
                                                } else {
                                                    // Conversion succeeded, ensure all fields are set
                                                    request.setId(bookingDoc.getId());

                                                    // Ensure roomId is set
                                                    if (request.getRoomId() == null || request.getRoomId().isEmpty()) {
                                                        request.setRoomId(roomId);
                                                    }

                                                    // Ensure roomTitle is set
                                                    if (request.getRoomTitle() == null || request.getRoomTitle().isEmpty()) {
                                                        request.setRoomTitle(finalRoomTitle);
                                                    }

                                                    // Ensure userId exists (critical for actions)
                                                    if (request.getUserId() == null || request.getUserId().isEmpty()) {
                                                        String userId = bookingDoc.getString("userId");
                                                        if (userId != null) {
                                                            request.setUserId(userId);
                                                        } else {
                                                            Log.e(TAG, "CRITICAL: Booking missing userId: " + bookingDoc.getId());
                                                        }
                                                    }

                                                    // Ensure userName exists
                                                    if (request.getUserName() == null || request.getUserName().isEmpty()) {
                                                        String userName = bookingDoc.getString("userName");
                                                        if (userName != null) {
                                                            request.setUserName(userName);
                                                        } else {
                                                            request.setUserName("Unknown User");
                                                        }
                                                    }
                                                }

                                                // Validate required fields before adding
                                                if (request.getUserId() != null && !request.getUserId().isEmpty()) {
                                                    adapter.addRequest(request);
                                                    Log.d(TAG, "Added booking: " + request.getId() +
                                                            " - User: " + request.getUserName() +
                                                            " - Room: " + request.getRoomTitle());
                                                } else {
                                                    Log.e(TAG, "Skipping booking without userId: " + bookingDoc.getId());
                                                    Toast.makeText(this, "Warning: Some bookings missing user data", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "Error loading bookings for room: " + roomId,
                                                    bookingTask.getException());
                                        }

                                        // All rooms processed
                                        if (processedRooms[0] == totalRooms) {
                                            progressBar.setVisibility(View.GONE);
                                            if (adapter.getItemCount() == 0) {
                                                tvNoRequests.setVisibility(View.VISIBLE);
                                                tvNoRequests.setText("No booking requests for your rooms");
                                            }
                                        }
                                    });
                        }

                    } else {
                        progressBar.setVisibility(View.GONE);
                        Exception e = task.getException();
                        Log.e(TAG, "Error loading rooms", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvNoRequests.setText("Error: " + e.getMessage());
                    }
                });
    }

    private void onBookingAction(BookingRequest request, String action) {
        // Validate request has required fields before proceeding
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

        // Validate required fields
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "acceptBooking: userId is null or empty");
            Toast.makeText(this, "Error: Cannot accept booking - missing user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Accepting booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Update in ROOM
        FirebaseUtils.getRoomBookingsCollection(roomId)
                .document(bookingId)
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking status updated to approved");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update room booking", e);
                });

        // Update in USER
        FirebaseFirestore.getInstance()
                .collection("users")
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

        // Validate required fields
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "rejectBooking: userId is null or empty");
            Toast.makeText(this, "Error: Cannot reject booking - missing user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Rejecting booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Update in ROOM
        FirebaseUtils.getRoomBookingsCollection(roomId)
                .document(bookingId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking status updated to rejected");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update room booking", e);
                });

        // Update in USER
        FirebaseFirestore.getInstance()
                .collection("users")
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

        // Validate required fields
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "cancelBooking: userId is null or empty");
            Toast.makeText(this, "Error: Cannot cancel booking - missing user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Cancelling booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Update in ROOM
        FirebaseUtils.getRoomBookingsCollection(roomId)
                .document(bookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking status updated to cancelled");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update room booking", e);
                });

        // Update in USER
        FirebaseFirestore.getInstance()
                .collection("users")
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

        // Validate required fields
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "deleteBooking: userId is null or empty");
            Toast.makeText(this, "Error: Cannot delete booking - missing user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Deleting booking - Room: " + roomId + ", Booking: " + bookingId + ", User: " + userId);

        // Delete from ROOM
        FirebaseUtils.getRoomBookingsCollection(roomId)
                .document(bookingId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Room booking deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete room booking", e);
                });

        // Delete from USER
        FirebaseFirestore.getInstance()
                .collection("users")
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