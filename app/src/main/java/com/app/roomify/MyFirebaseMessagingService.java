package com.app.roomify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "roomify_notifications";
    private static final String CHANNEL_NAME = "Roomify Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Handle data message (works when app is in background or closed)
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            // Handle notification message
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String bookingId = data.get("bookingId");
        String type = data.get("type"); // "booking_request", "booking_approved", etc.

        Log.d(TAG, "Handling data message - Title: " + title + ", Message: " + message);

        // Create notification
        showNotification(title, message, bookingId, type);
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String message = notification.getBody();

        Log.d(TAG, "Handling notification message - Title: " + title + ", Message: " + message);

        // Create notification
        showNotification(title, message, null, null);
    }

    private void showNotification(String title, String message, String bookingId, String type) {
        Intent intent;

        // Create intent based on notification type
        if (bookingId != null && !bookingId.isEmpty()) {
            // If it's a booking notification, open BookingRequestsActivity
            if ("owner".equals(type)) {
                intent = new Intent(this, BookingRequestsActivity.class);
                intent.putExtra("role", "owner");
            } else {
                intent = new Intent(this, BookingRequestsActivity.class);
                intent.putExtra("role", "tenant");
            }
            intent.putExtra("bookingId", bookingId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            // Default intent - open MainActivity
            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create notification channel for Android O and above
        createNotificationChannel();

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_apartment) // Make sure you have this icon
                .setContentTitle(title != null ? title : "Roomify")
                .setContentText(message != null ? message : "You have a new notification")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Show notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());

        Log.d(TAG, "Notification shown: " + title);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Roomify notifications for booking requests and updates");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);

        // Send token to your server or Firestore
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        String userId = FirebaseUtils.getCurrentUserId();
        if (userId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "FCM token saved to Firestore");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save FCM token", e);
                    });
        }
    }
}