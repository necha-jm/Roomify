package com.app.roomify.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.app.roomify.models.User;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER = "user_data";
    private static final String KEY_USER_ID = "user_id";

    private static TokenManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private Context context;

    public TokenManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }



    public static synchronized TokenManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TokenManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    public void saveToken(String token) {
        if (token != null && !token.isEmpty()) {
            Log.d(TAG, "Saving token, length: " + token.length());
            prefs.edit().putString(KEY_TOKEN, token).apply();
        }
    }

    public String getToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        Log.d(TAG, "Retrieved token: " + (token != null ? "Present, length: " + token.length() : "NULL"));
        return token;
    }

    public void saveUser(User user) {
        if (user != null) {
            String userJson = gson.toJson(user);
            prefs.edit()
                    .putString(KEY_USER, userJson)
                    .putLong(KEY_USER_ID, user.getId() != 0 ? user.getId() : -1L)
                    .apply();
            Log.d(TAG, "User saved: " + user.getEmail());
        }
    }

    public User getUser() {
        String userJson = prefs.getString(KEY_USER, null);
        User user = userJson != null ? gson.fromJson(userJson, User.class) : null;
        Log.d(TAG, "Retrieved user: " + (user != null ? user.getEmail() : "NULL"));
        return user;
    }

    public Long getUserId() {
        long userId = prefs.getLong(KEY_USER_ID, -1L);
        return userId != -1L ? userId : null;
    }

    public void clear() {
        Log.d(TAG, "Clearing all tokens and user data");
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        User user = getUser();
        boolean loggedIn = token != null && user != null && !token.isEmpty();
        Log.d(TAG, "isLoggedIn: " + loggedIn);
        return loggedIn;
    }

    public String getAuthHeader() {
        String token = getToken();
        String header = token != null ? "Bearer " + token : null;
        Log.d(TAG, "Auth header: " + (header != null ? "Present, length: " + header.length() : "NULL"));
        return header;
    }
}