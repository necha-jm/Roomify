package com.app.roomify.sync;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.app.roomify.Room;
import com.app.roomify.RoomCreateRequest;
import com.app.roomify.database.RoomEntity;
import com.app.roomify.database.RoomifyDatabase;
import com.app.roomify.models.ApiResponse;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

public class OfflineSyncWorker extends Worker {

    private static final String TAG = "OfflineSyncWorker";

    public OfflineSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            syncOfflineData();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed: " + e.getMessage());
            return Result.retry();
        }
    }

    private void syncOfflineData() {
        TokenManager tokenManager = new TokenManager(getApplicationContext());

        if (!tokenManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping sync");
            return;
        }

        RoomifyDatabase db = RoomifyDatabase.getInstance(getApplicationContext());
        List<RoomEntity> unsyncedRooms = db.roomDao().getUnsyncedRooms();

        if (unsyncedRooms.isEmpty()) {
            Log.d(TAG, "No unsynced data found");
            fetchLatestFromServer(db, tokenManager);
            return;
        }

        Log.d(TAG, "Found " + unsyncedRooms.size() + " unsynced rooms");

        APIClient.init(tokenManager);
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

        for (RoomEntity entity : unsyncedRooms) {
            try {
                Room room = OfflineSyncManager.convertToRoom(entity);

                // ============ FIX: Use RoomCreateRequest instead of Room ============
                // Create the request object with only necessary fields
                RoomCreateRequest request = new RoomCreateRequest(room);

                // Log for debugging
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String json = gson.toJson(request);
                Log.d(TAG, "Syncing room with JSON: " + json);

                // Use the new request object
                Call<Room> call = apiInterface.createRoom(request);
                Response<Room> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    Room syncedRoom = response.body();
                    // Update the entity with the server-generated ID
                    entity.setServerId(syncedRoom.getId());
                    entity.setSynced(true);
                    db.roomDao().updateRoom(entity);
                    Log.d(TAG, "Synced room: " + entity.getId() + " -> Server ID: " + syncedRoom.getId());
                } else {
                    String errorMsg = "Failed to sync room: " + entity.getId();
                    if (response.errorBody() != null) {
                        errorMsg = response.errorBody().string();
                        Log.e(TAG, "Error body: " + errorMsg);
                    }
                    Log.e(TAG, errorMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing room: " + entity.getId(), e);
            }
        }

        fetchLatestFromServer(db, tokenManager);
    }

    // ONLY THIS METHOD IS MODIFIED - fetchLatestFromServer
    private void fetchLatestFromServer(RoomifyDatabase db, TokenManager tokenManager) {
        try {
            APIClient.init(tokenManager);
            APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

            // Use unwrapped response
            Call<List<Room>> call = apiInterface.getAllRooms();
            Response<List<Room>> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                List<Room> serverRooms = response.body();

                if (serverRooms != null && !serverRooms.isEmpty()) {
                    db.roomDao().deleteAllRooms();
                    for (Room room : serverRooms) {
                        RoomEntity entity = OfflineSyncManager.convertToEntity(room);
                        entity.setSynced(true);
                        entity.setServerId(room.getId());
                        db.roomDao().insertRoom(entity);
                    }
                    Log.d(TAG, "Fetched " + serverRooms.size() + " rooms from server");
                }
            } else {
                Log.e(TAG, "Failed to fetch rooms from server. Response code: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch latest data from server", e);
        }
    }
}