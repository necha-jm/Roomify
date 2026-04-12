package com.app.roomify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.leolin.shortcutbadger.ShortcutBadger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "roomify_notifications";
    private static final String CHANNEL_NAME = "Roomify Notifications";
    private static final String CHANNEL_DESCRIPTION = "Roomify notifications for room updates, bookings, and messages";

    private static int notificationCount = 0;
    private static Context appContext;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TokenManager tokenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        tokenManager = TokenManager.getInstance(appContext);
        loadSavedNotificationCount();
        createNotificationChannel(); // Moved to separate method
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Increment notification count for badge
        notificationCount++;
        saveNotificationCount();
        updateBadgeCount();

        // Handle data-only messages
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String body = data.get("body");
            String type = data.get("type");
            String roomId = data.get("roomId");
            String bookingId = data.get("bookingId");
            String imageUrl = data.get("imageUrl");

            Log.d(TAG, "Data message - Title: " + title + ", Type: " + type + ", BookingId: " + bookingId);

            sendNotification(title, body, type, roomId, bookingId, imageUrl);
        }

        // Handle notification messages
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            String imageUrl = remoteMessage.getNotification().getImageUrl() != null ?
                    remoteMessage.getNotification().getImageUrl().toString() : null;

            Log.d(TAG, "Notification message - Title: " + title + ", Body: " + body);

            sendNotification(title, body, null, null, null, imageUrl);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);

        // Send token to your backend server
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        if (tokenManager == null) {
            tokenManager = TokenManager.getInstance(this);
        }

        if (tokenManager.isLoggedIn()) {
            Long userId = tokenManager.getUserId();
            if (userId != null && userId != -1L) {
                Log.d(TAG, "Saving FCM token for user: " + userId);

                APIClient.init(tokenManager);
                APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

                // Get device information
                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String deviceModel = Build.MODEL;
                String osVersion = Build.VERSION.RELEASE;
                String appVersion = getAppVersion();

                Call<ApiResponse<Void>> call = apiInterface.registerFcmToken(
                        userId, token, deviceId, deviceModel, osVersion, appVersion
                );

                call.enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.d(TAG, "✅ FCM token saved to server");
                        } else {
                            Log.e(TAG, "Failed to save FCM token: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Log.e(TAG, "Error saving FCM token: " + t.getMessage());
                    }
                });
            } else {
                Log.d(TAG, "Invalid user ID, skipping token registration");
            }
        } else {
            Log.d(TAG, "User not logged in, skipping token registration");
        }
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

    private void createNotificationChannel() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.setShowBadge(true);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 100, 200});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    private void sendNotification(String title, String body, String type,
                                  String roomId, String bookingId, String imageUrl) {

        // Set default title and body if null
        if (title == null || title.isEmpty()) {
            title = "Roomify Update";
        }
        if (body == null || body.isEmpty()) {
            body = "You have a new notification";
        }

        // Create intent based on notification type
        Intent intent = createIntentForNotificationType(type, roomId, bookingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                .setColor(ContextCompat.getColor(this, R.color.primary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Add large icon if image URL is provided
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadLargeIconAsync(notificationBuilder, imageUrl);
        }

        // Add action buttons based on notification type
        addActionButtons(notificationBuilder, type, bookingId);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                // Still try to show notification, but log warning
            }
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private Intent createIntentForNotificationType(String type, String roomId, String bookingId) {
        Intent intent;

        if ("new_room".equals(type) && roomId != null) {
            intent = new Intent(this, RoomDetailsActivity.class);
            intent.putExtra("room_id", Long.parseLong(roomId));
            intent.putExtra("from_notification", true);
        } else if ("booking_request".equals(type)) {
            intent = new Intent(this, BookingRequestsActivity.class);
            intent.putExtra("role", "owner");
            intent.putExtra("from_notification", true);
            // Add booking ID if available
            if (bookingId != null) {
                intent.putExtra("booking_id", Long.parseLong(bookingId));
            }
        } else if ("booking_accepted".equals(type)) {
            intent = new Intent(this, BookingRequestsActivity.class);
            intent.putExtra("role", "tenant");
            intent.putExtra("from_notification", true);
            intent.putExtra("highlight_status", "ACCEPTED");
            if (bookingId != null) {
                intent.putExtra("booking_id", Long.parseLong(bookingId));
            }
        } else if ("booking_rejected".equals(type)) {
            intent = new Intent(this, BookingRequestsActivity.class);
            intent.putExtra("role", "tenant");
            intent.putExtra("from_notification", true);
            intent.putExtra("highlight_status", "REJECTED");
            if (bookingId != null) {
                intent.putExtra("booking_id", Long.parseLong(bookingId));
            }
        } else if ("booking_cancelled".equals(type)) {
            intent = new Intent(this, BookingRequestsActivity.class);
            intent.putExtra("role", "tenant");
            intent.putExtra("from_notification", true);
            intent.putExtra("highlight_status", "CANCELLED");
            if (bookingId != null) {
                intent.putExtra("booking_id", Long.parseLong(bookingId));
            }
        } else {
            // Default based on user role
            if (tokenManager == null) {
                tokenManager = TokenManager.getInstance(this);
            }
            if (tokenManager.isLoggedIn()) {
                User user = tokenManager.getUser();
                if (user != null && "owner".equals(user.getRole())) {
                    intent = new Intent(this, OwnerDashboard.class);
                } else {
                    intent = new Intent(this, DashboardActivity.class);
                }
            } else {
                intent = new Intent(this, DashboardActivity.class);
            }
        }

        return intent;
    }

    private void addActionButtons(NotificationCompat.Builder builder, String type, String bookingId) {
        if ("booking_request".equals(type) && bookingId != null) {
            try {
                long bookingIdLong = Long.parseLong(bookingId);

                Intent acceptIntent = new Intent(this, NotificationActionReceiver.class);
                acceptIntent.setAction("ACTION_ACCEPT_BOOKING");
                acceptIntent.putExtra("booking_id", bookingIdLong);

                PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                        this, 0, acceptIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                Intent rejectIntent = new Intent(this, NotificationActionReceiver.class);
                rejectIntent.setAction("ACTION_REJECT_BOOKING");
                rejectIntent.putExtra("booking_id", bookingIdLong);

                PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                        this, 1, rejectIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                builder.addAction(R.drawable.ic_check, "Accept", acceptPendingIntent);
                builder.addAction(R.drawable.ic_close, "Reject", rejectPendingIntent);

                Log.d(TAG, "Added action buttons for booking: " + bookingId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid booking ID format: " + bookingId);
            }
        }
    }

    private void loadLargeIconAsync(NotificationCompat.Builder builder, String imageUrl) {
        executorService.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                new Handler(Looper.getMainLooper()).post(() -> {
                    builder.setLargeIcon(bitmap);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    NotificationManagerCompat.from(MyFirebaseMessagingService.this).notify(
                            (int) System.currentTimeMillis(),
                            builder.build()
                    );
                });

                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Failed to load large icon", e);
            }
        });
    }

    private void updateBadgeCount() {
        try {
            ShortcutBadger.applyCount(getApplicationContext(), notificationCount);
            Log.d(TAG, "Badge count updated to: " + notificationCount);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update badge count: " + e.getMessage());
        }
    }

    private void saveNotificationCount() {
        getSharedPreferences("notification_prefs", MODE_PRIVATE)
                .edit()
                .putInt("notification_count", notificationCount)
                .apply();
    }

    private void loadSavedNotificationCount() {
        notificationCount = getSharedPreferences("notification_prefs", MODE_PRIVATE)
                .getInt("notification_count", 0);
    }

    public static void clearNotificationCount(Context context) {
        notificationCount = 0;
        try {
            ShortcutBadger.removeCount(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove badge count: " + e.getMessage());
        }

        if (context != null) {
            context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("notification_count", 0)
                    .apply();
        }
    }

    public static int getNotificationCount() {
        return notificationCount;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}