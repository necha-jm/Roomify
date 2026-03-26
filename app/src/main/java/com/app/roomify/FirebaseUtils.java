package com.app.roomify;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtils {

    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // --- User Callbacks ---
    public interface UserCallback {
        void onCallback(String value);
    }

    // Get current user ID
    public static String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    // Get current user's name
    public static void getCurrentUserName(UserCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onCallback(null);
            return;
        }
        getUser(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String name = task.getResult().getString("name");
                callback.onCallback(name);
            } else {
                callback.onCallback(null);
            }
        });
    }

    // Get current user's phone
    public static void getCurrentUserPhone(UserCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onCallback(null);
            return;
        }
        getUser(uid, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String phone = task.getResult().getString("phone");
                callback.onCallback(phone);
            } else {
                callback.onCallback(null);
            }
        });
    }

    // --- Firestore Helpers ---
    // Get a user document by ID
    public static void getUser(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        if (userId == null) return;
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Save a room to Firestore
    public static void saveRoom(Room room, OnCompleteListener<Void> listener) {
        if (room == null || room.getId() == null) return;
        db.collection("rooms")
                .document(room.getId())
                .set(room)
                .addOnCompleteListener(listener);
    }

    // Get room document by ID
    public static void getRoom(String roomId, OnCompleteListener<DocumentSnapshot> listener) {
        if (roomId == null) return;
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnCompleteListener(listener);
    }

    // Get bookings collection reference for a room
    public static com.google.firebase.firestore.CollectionReference getRoomBookingsCollection(String roomId) {
        return db.collection("rooms").document(roomId).collection("bookings");
    }

    // Send notification to a user
    public static void sendNotificationToUser(String userId, String title, String message, String bookingId) {
        // Implementation depends on your FCM setup
        // Example: save notification document to Firestore
        if (userId == null) return;
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(new java.util.HashMap<String, Object>() {{
                    put("title", title);
                    put("message", message);
                    put("bookingId", bookingId);
                    put("timestamp", System.currentTimeMillis());
                }});
    }
}