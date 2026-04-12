package com.app.roomify.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {

    private static Gson gson = new Gson();

    // Single converter for List<String> to String and back
    // This handles all List<String> conversions (amenities, rules, images)
    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) {
            return gson.toJson(new ArrayList<String>());
        }
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }
}