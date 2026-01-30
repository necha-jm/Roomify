package com.app.roomify;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtils {

    public static void saveRoom(Room room, OnCompleteListener<Void> listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms")
                .document(room.getId())
                .set(room)
                .addOnCompleteListener(listener);
    }

    public static void getUser(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(listener);
    }

}
