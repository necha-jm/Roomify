package com.app.roomify;


import android.content.Context;
import android.util.Log;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.User;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import java.util.List;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private static DatabaseHelper instance;
    private APIInterface apiInterface;
    private TokenManager tokenManager;
    private Context context;

    private DatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
        this.tokenManager = new TokenManager(context);
        APIClient.init(tokenManager);
        this.apiInterface = APIClient.getClient().create(APIInterface.class);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    // ==================== USER METHODS ====================

    public interface UserCallback {
        void onCallback(User user);
    }

    public interface StringCallback {
        void onCallback(String value);
    }

    public String getCurrentUserId() {
        long userId = tokenManager.getUserId();
        return userId != -1 ? String.valueOf(userId) : null;
    }

    public User getCurrentUser() {
        return tokenManager.getUser();
    }

    public void getCurrentUserName(StringCallback callback) {
        User user = getCurrentUser();
        if (callback != null) {
            callback.onCallback(user != null ? user.getName() : null);
        }
    }


    public void getUser(String userId, UserCallback callback) {
        try {
            long id = Long.parseLong(userId);
            Call<ApiResponse<User>> call = apiInterface.getUserById(id);
            call.enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        if (callback != null) callback.onCallback(response.body().getData());
                    } else if (callback != null) callback.onCallback(null);
                }
                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    Log.e(TAG, "Error getting user", t);
                    if (callback != null) callback.onCallback(null);
                }
            });
        } catch (NumberFormatException e) {
            if (callback != null) callback.onCallback(null);
        }
    }

    // ==================== ROOM METHODS ====================

    public interface RoomCallback {
        void onCallback(Room room);
    }

    public interface RoomsCallback {
        void onCallback(List<Room> rooms);
    }

    public interface VoidCallback {
        void onComplete(boolean success, String message);
    }

    public void saveRoom(Room room, VoidCallback callback) {
        if (room == null) {
            if (callback != null) callback.onComplete(false, "Room is null");
            return;
        }

        Call<Room> call = apiInterface.createRoom(room);
        call.enqueue(new Callback<Room>() {
            @Override
            public void onResponse(Call<Room> call, Response<Room> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Room createdRoom = response.body();
                    if (callback != null) callback.onComplete(true, "Room saved");
                } else {
                    if (callback != null) callback.onComplete(false, "Failed to save room");
                }
            }
            @Override
            public void onFailure(Call<Room> call, Throwable t) {
                if (callback != null) callback.onComplete(false, t.getMessage());
            }
        });
    }

    public void getRoom(String roomId, RoomCallback callback) {
        try {
            long id = Long.parseLong(roomId);
            Call<Room> call = apiInterface.getRoomById(id);
            call.enqueue(new Callback<Room>() {
                @Override
                public void onResponse(Call<Room> call, Response<Room> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (callback != null) callback.onCallback(response.body());
                    } else if (callback != null) callback.onCallback(null);
                }
                @Override
                public void onFailure(Call<Room> call, Throwable t) {
                    Log.e(TAG, "Error getting room", t);
                    if (callback != null) callback.onCallback(null);
                }
            });
        } catch (NumberFormatException e) {
            if (callback != null) callback.onCallback(null);
        }
    }

    public void getAllRooms(RoomsCallback callback) {
        Call<List<Room>> call = apiInterface.getAllRooms();
        call.enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onCallback(response.body());
                } else if (callback != null) callback.onCallback(null);
            }
            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                Log.e(TAG, "Error getting rooms", t);
                if (callback != null) callback.onCallback(null);
            }
        });
    }

    public void getRoomsByOwner(String ownerId, RoomsCallback callback) {
        try {
            long id = Long.parseLong(ownerId);
            Call<List<Room>> call = apiInterface.getRoomsByOwner(id);
            call.enqueue(new Callback<List<Room>>() {
                @Override
                public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (callback != null) callback.onCallback(response.body());
                    } else if (callback != null) callback.onCallback(null);
                }
                @Override
                public void onFailure(Call<List<Room>> call, Throwable t) {
                    Log.e(TAG, "Error getting rooms by owner", t);
                    if (callback != null) callback.onCallback(null);
                }
            });
        } catch (NumberFormatException e) {
            if (callback != null) callback.onCallback(null);
        }
    }

    public void deleteRoom(String roomId, VoidCallback callback) {
        try {
            long id = Long.parseLong(roomId);
            Call<ApiResponse<Void>> call = apiInterface.deleteRoom(id);
            call.enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        if (callback != null) callback.onComplete(true, "Room deleted");
                    } else {
                        if (callback != null) callback.onComplete(false, "Failed to delete");
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    if (callback != null) callback.onComplete(false, t.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            if (callback != null) callback.onComplete(false, "Invalid room ID");
        }
    }

    public void sendNotificationToUser(String userId, String title, String message, String bookingId) {
        // Implement if needed
        Log.d(TAG, "Notification: " + title + " - " + message);
    }
}
