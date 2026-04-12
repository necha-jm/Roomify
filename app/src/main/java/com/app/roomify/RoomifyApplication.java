package com.app.roomify;


import android.app.Application;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.TokenManager;
import com.app.roomify.sync.OfflineSyncManager;

public class RoomifyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize TokenManager
        TokenManager tokenManager = TokenManager.getInstance(this);
        APIClient.init(tokenManager);

        // Start periodic sync for offline data
        OfflineSyncManager.startPeriodicSync(this);
    }
}
