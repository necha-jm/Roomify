package com.app.roomify.database;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface RoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoom(RoomEntity room);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllRooms(List<RoomEntity> rooms);

    @Update
    void updateRoom(RoomEntity room);

    @Query("SELECT * FROM rooms WHERE isAvailable = 1 ORDER BY createdAt DESC")
    List<RoomEntity> getAllAvailableRooms();

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    RoomEntity getRoomById(long roomId);

    @Query("SELECT * FROM rooms WHERE isSynced = 0")
    List<RoomEntity> getUnsyncedRooms();

    @Query("DELETE FROM rooms")
    void deleteAllRooms();

    @Query("DELETE FROM rooms WHERE id = :roomId")
    void deleteRoomById(long roomId);

    @Query("UPDATE rooms SET isSynced = 1 WHERE id = :roomId")
    void markAsSynced(long roomId);

    @Query("SELECT COUNT(*) FROM rooms")
    int getRoomCount();

    @Query("SELECT * FROM rooms WHERE postedBy = :ownerId")
    List<RoomEntity> getRoomsByOwner(long ownerId);
}
