package com.app.roomify.sync;

import android.content.Context;
import android.util.Log;
import androidx.work.*;
import com.app.roomify.Room;
import com.app.roomify.database.RoomEntity;
import com.app.roomify.database.RoomifyDatabase;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;
import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OfflineSyncManager {

    private static final String TAG = "OfflineSync";
    private static final String SYNC_WORK_NAME = "roomify_offline_sync";
    private static Gson gson = new Gson();

    public static void startPeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                OfflineSyncWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
        );
    }

    public static void triggerImmediateSync(Context context) {
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(OfflineSyncWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(context).enqueue(syncWorkRequest);
    }

    public static void saveRoomOffline(Context context, Room room) {
        RoomifyDatabase db = RoomifyDatabase.getInstance(context);
        RoomEntity entity = convertToEntity(room);
        entity.setSynced(false);
        entity.setLastUpdated(System.currentTimeMillis());

        new Thread(() -> {
            db.roomDao().insertRoom(entity);
            Log.d(TAG, "Room saved offline: " + room.getId());
        }).start();
    }

    public static RoomEntity convertToEntity(Room room) {
        RoomEntity entity = new RoomEntity();
        entity.setId(room.getId());
        entity.setTitle(room.getTitle());
        entity.setDescription(room.getDescription());
        entity.setPropertyType(room.getPropertyType());
        entity.setPrice(room.getPrice());
        entity.setLatitude(room.getLatitude());
        entity.setLongitude(room.getLongitude());
        entity.setAddress(room.getAddress());
        entity.setPostedBy(room.getPostedBy());
        entity.setOwnerName(room.getOwnerName());
        entity.setContactPhone(room.getContactPhone());
        entity.setContactEmail(room.getContactEmail());
        entity.setRoomsCount(room.getRoomsCount());
        entity.setBathroomsCount(room.getBathroomsCount());
        entity.setArea(room.getArea());

        // Convert lists to JSON strings using Gson
        entity.setAmenities(room.getAmenities());
        entity.setRules(room.getRules());
        entity.setImages(room.getImages());

        entity.setImageCount(room.getImageCount());
        entity.setHasVideo(room.isHasVideo());
        entity.setHasContract(room.isHasContract());
        entity.setVideoUrl(room.getVideoUrl());
        entity.setContractUrl(room.getContractUrl());
        entity.setAvailable(room.isAvailable());
        entity.setStatus(room.getStatus());
        entity.setBookingsCount(room.getBookingsCount());
        entity.setCreatedAt(room.getCreatedAt());
        entity.setSynced(false);
        entity.setLastUpdated(System.currentTimeMillis());
        return entity;
    }

    public static Room convertToRoom(RoomEntity entity) {
        Room room = new Room();
        room.setId(entity.getId());
        room.setTitle(entity.getTitle());
        room.setDescription(entity.getDescription());
        room.setPropertyType(entity.getPropertyType());
        room.setPrice(entity.getPrice());
        room.setLatitude(entity.getLatitude());
        room.setLongitude(entity.getLongitude());
        room.setAddress(entity.getAddress());
        room.setPostedBy(entity.getPostedBy());
        room.setOwnerName(entity.getOwnerName());
        room.setContactPhone(entity.getContactPhone());
        room.setContactEmail(entity.getContactEmail());
        room.setRoomsCount(entity.getRoomsCount());
        room.setBathroomsCount(entity.getBathroomsCount());
        room.setArea(entity.getArea());

        // Lists are already stored as JSON strings in RoomEntity
        // The Converters class handles the conversion automatically
        room.setAmenities(entity.getAmenities());
        room.setRules(entity.getRules());
        room.setImages(entity.getImages());

        room.setImageCount(entity.getImageCount());
        room.setHasVideo(entity.isHasVideo());
        room.setHasContract(entity.isHasContract());
        room.setVideoUrl(entity.getVideoUrl());
        room.setContractUrl(entity.getContractUrl());
        room.setAvailable(entity.isAvailable());
        room.setStatus(entity.getStatus());
        room.setBookingsCount(entity.getBookingsCount());
        room.setCreatedAt(entity.getCreatedAt());
        return room;
    }
}