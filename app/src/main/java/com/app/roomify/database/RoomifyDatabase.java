package com.app.roomify.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {RoomEntity.class}, version = 1, exportSchema = false)
public abstract class RoomifyDatabase extends RoomDatabase {

    private static RoomifyDatabase instance;

    public abstract RoomDao roomDao();

    public static synchronized RoomifyDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            RoomifyDatabase.class, "roomify_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}