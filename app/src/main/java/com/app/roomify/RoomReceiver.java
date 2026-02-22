package com.app.roomify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RoomReceiver extends BroadcastReceiver {

    public static final String ACTION_NEW_ROOM =
            "com.app.roomify.NEW_ROOM_ADDED";

    private static final String TAG = "RoomBroadcastReceiver";

    public interface RoomUpdateListener {
        void onRoomAdded();
    }

    private static RoomUpdateListener listener;

    // Method to attach listener from Activity
    public static void setRoomUpdateListener(RoomUpdateListener l) {
        listener = l;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null &&
                ACTION_NEW_ROOM.equals(intent.getAction())) {

            Log.d(TAG, "Broadcast received: New room added");

            if (listener != null) {
                listener.onRoomAdded();
            }
        }
    }

}