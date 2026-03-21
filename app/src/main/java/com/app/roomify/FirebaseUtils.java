package com.app.roomify;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtils {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Save a room to Firestore
    public static void saveRoom(Room room, OnCompleteListener<Void> listener) {
        if (room == null || room.getId() == null) return;
        db.collection("rooms")
                .document(room.getId())
                .set(room)
                .addOnCompleteListener(listener);
    }

    // Get user document by ID
    public static void getUser(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        if (userId == null) return;
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

    // New: Get room document by ID
    public static void getRoom(String roomId, OnCompleteListener<DocumentSnapshot> listener) {
        if (roomId == null) return;
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnCompleteListener(listener);
    }
}