package com.app.roomify;

import android.content.Context;
import android.util.Log;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";
    private static Context appContext;

    // Initialize with application context
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // --- User Callbacks ---
    public interface UserCallback {
        void onCallback(String value);
    }

    public interface UserObjectCallback {
        void onCallback(User user);
    }

    public interface RoomCallback {
        void onCallback(Room room);
    }

    public interface RoomsCallback {
        void onCallback(List<Room> rooms);
    }

    public interface SuccessCallback {
        void onSuccess(boolean success, String message);
    }

    public interface VoidCallback {
        void onComplete(boolean success, String message);
    }

    // Get current user ID from TokenManager
    public static String getCurrentUserId() {
        if (appContext == null) {
            Log.e(TAG, "FirebaseUtils not initialized. Call init() first.");
            return null;
        }
        TokenManager tokenManager = new TokenManager(appContext);
        long userId = tokenManager.getUserId();
        return userId != -1 ? String.valueOf(userId) : null;
    }

    // Get current user object
    public static User getCurrentUser() {
        if (appContext == null) {
            Log.e(TAG, "FirebaseUtils not initialized. Call init() first.");
            return null;
        }
        TokenManager tokenManager = new TokenManager(appContext);
        return tokenManager.getUser();
    }

    // Get current user's name
    public static void getCurrentUserName(UserCallback callback) {
        User user = getCurrentUser();
        if (user != null && callback != null) {
            callback.onCallback(user.getName());
        } else if (callback != null) {
            callback.onCallback(null);
        }
    }

    // Get current user's phone (returns empty string for now, can be extended)
    public static void getCurrentUserPhone(UserCallback callback) {
        User user = getCurrentUser();
        if (callback != null) {
            // Phone might not be in User model, return empty for now
            callback.onCallback(user != null ? user.getEmail() : null);
        }
    }

    // Get user by ID from backend
    public static void getUser(String userId, UserObjectCallback callback) {
        if (userId == null || appContext == null) {
            if (callback != null) callback.onCallback(null);
            return;
        }

        try {
            long userIdLong = Long.parseLong(userId);
            APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

            Call<ApiResponse<User>> call = apiInterface.getUserById(userIdLong);
            call.enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        if (callback != null) callback.onCallback(response.body().getData());
                    } else {
                        if (callback != null) callback.onCallback(null);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    Log.e(TAG, "Error getting user: " + t.getMessage());
                    if (callback != null) callback.onCallback(null);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid user ID format: " + userId);
            if (callback != null) callback.onCallback(null);
        }
    }

    // Save a room to backend
    public static void saveRoom(Room room, SuccessCallback callback) {
        if (room == null || appContext == null) {
            if (callback != null) callback.onSuccess(false, "Room is null");
            return;
        }

        // Create the request object with only necessary fields
        RoomCreateRequest request = new RoomCreateRequest(room);

        // Log the request for debugging
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(request);
            Log.d(TAG, "Saving room with JSON: " + json);
        } catch (Exception e) {
            Log.e(TAG, "Error logging JSON", e);
        }

        TokenManager tokenManager = new TokenManager(appContext);
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

        // Use the new request object instead of Room directly
        Call<Room> call = apiInterface.createRoom(request);
        call.enqueue(new Callback<Room>() {
            @Override
            public void onResponse(Call<Room> call, Response<Room> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Room createdRoom = response.body();
                    Log.d(TAG, "Room saved successfully with ID: " + createdRoom.getId());
                    if (callback != null) callback.onSuccess(true, "Room saved successfully");
                } else {
                    String errorMsg = "Failed to save room";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    if (callback != null) callback.onSuccess(false, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Room> call, Throwable t) {
                Log.e(TAG, "Error saving room: " + t.getMessage());
                if (callback != null) callback.onSuccess(false, t.getMessage());
            }
        });
    }

    // Get room by ID from backend
    public static void getRoom(String roomId, RoomCallback callback) {
        if (roomId == null || appContext == null) {
            if (callback != null) callback.onCallback(null);
            return;
        }

        try {
            long roomIdLong = Long.parseLong(roomId);
            APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

            Call<Room> call = apiInterface.getRoomById(roomIdLong);
            call.enqueue(new Callback<Room>() {
                @Override
                public void onResponse(Call<Room> call, Response<Room> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (callback != null) callback.onCallback(response.body());
                    } else {
                        if (callback != null) callback.onCallback(null);
                    }
                }

                @Override
                public void onFailure(Call<Room> call, Throwable t) {
                    Log.e(TAG, "Error getting room: " + t.getMessage());
                    if (callback != null) callback.onCallback(null);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid room ID format: " + roomId);
            if (callback != null) callback.onCallback(null);
        }
    }

    // ==================== ONLY THIS METHOD IS MODIFIED ====================
    // Get all rooms from backend
    public static void getAllRooms(RoomsCallback callback) {
        if (appContext == null) {
            if (callback != null) callback.onCallback(null);
            return;
        }

        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

        // Use unwrapped response
        Call<List<Room>> call = apiInterface.getAllRooms();
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onCallback(response.body());
                } else {
                    if (callback != null) callback.onCallback(null);
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                Log.e(TAG, "Error getting rooms: " + t.getMessage());
                if (callback != null) callback.onCallback(null);
            }
        });
    }
    // ==================== END OF MODIFIED METHOD ====================

    // Get rooms by owner ID
    public static void getRoomsByOwner(String ownerId, RoomsCallback callback) {
        if (ownerId == null || appContext == null) {
            if (callback != null) callback.onCallback(null);
            return;
        }

        try {
            long ownerIdLong = Long.parseLong(ownerId);
            APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

            Call<List<Room>> call = apiInterface.getRoomsByOwner(ownerIdLong);
            call.enqueue(new Callback<List<Room>>() {
                @Override
                public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (callback != null) callback.onCallback(response.body());
                    } else {
                        if (callback != null) callback.onCallback(null);
                    }
                }

                @Override
                public void onFailure(Call<List<Room>> call, Throwable t) {
                    Log.e(TAG, "Error getting rooms by owner: " + t.getMessage());
                    if (callback != null) callback.onCallback(null);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid owner ID format: " + ownerId);
            if (callback != null) callback.onCallback(null);
        }
    }

    // Update room availability
    public static void updateRoomAvailability(String roomId, boolean isAvailable, VoidCallback callback) {
        if (roomId == null || appContext == null) {
            if (callback != null) callback.onComplete(false, "Room ID is null");
            return;
        }

        try {
            long roomIdLong = Long.parseLong(roomId);
            APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

            // First get the room
            Call<Room> getCall = apiInterface.getRoomById(roomIdLong);
            getCall.enqueue(new Callback<Room>() {
                @Override
                public void onResponse(Call<Room> call, Response<Room> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Room room = response.body();
                        room.setAvailable(isAvailable);

                        // Then update it
                        Call<Room> updateCall = apiInterface.updateRoom(roomIdLong, room);
                        updateCall.enqueue(new Callback<Room>() {
                            @Override
                            public void onResponse(Call<Room> call, Response<Room> response) {
                                if (response.isSuccessful()) {
                                    if (callback != null) callback.onComplete(true, "Room availability updated");
                                } else {
                                    if (callback != null) callback.onComplete(false, "Failed to update room");
                                }
                            }

                            @Override
                            public void onFailure(Call<Room> call, Throwable t) {
                                if (callback != null) callback.onComplete(false, t.getMessage());
                            }
                        });
                    } else {
                        if (callback != null) callback.onComplete(false, "Room not found");
                    }
                }

                @Override
                public void onFailure(Call<Room> call, Throwable t) {
                    if (callback != null) callback.onComplete(false, t.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid room ID format: " + roomId);
            if (callback != null) callback.onComplete(false, "Invalid room ID");
        }
    }

    // Send notification to a user (using backend API)
    public static void sendNotificationToUser(String userId, String title, String message, String bookingId) {
        if (userId == null || appContext == null) return;

        try {
            TokenManager tokenManager = new TokenManager(appContext);
            APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

            NotificationRequest notification = new NotificationRequest();
            notification.setUserId(Long.parseLong(userId));
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setBookingId(bookingId);

            Call<ApiResponse<Void>> call = apiInterface.sendNotification(notification);
            call.enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification sent successfully");
                    } else {
                        Log.e(TAG, "Failed to send notification");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Log.e(TAG, "Error sending notification: " + t.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid user ID format: " + userId);
        }
    }
}