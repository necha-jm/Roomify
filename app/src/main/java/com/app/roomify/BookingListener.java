package com.app.roomify;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookingListener {

    // Listen for new booking requests for owner's rooms
    public static void listenForNewBookings(String ownerId, OnNewBookingListener listener) {
        FirebaseFirestore.getInstance()
                .collection("rooms")
                .whereEqualTo("postedBy", ownerId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            String roomId = change.getDocument().getId();

                            // Listen to bookings subcollection
                            change.getDocument().getReference()
                                    .collection("bookings")
                                    .whereEqualTo("status", "pending")
                                    .addSnapshotListener((bookingSnapshots, bookingError) -> {
                                        for (DocumentChange bookingChange : bookingSnapshots.getDocumentChanges()) {
                                            if (bookingChange.getType() == DocumentChange.Type.ADDED) {
                                                BookingRequest request = bookingChange.getDocument().toObject(BookingRequest.class);
                                                listener.onNewBooking(request);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    interface OnNewBookingListener {
        void onNewBooking(BookingRequest request);
    }
}
