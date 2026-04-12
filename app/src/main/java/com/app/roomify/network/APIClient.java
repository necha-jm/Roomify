package com.app.roomify.network;

import android.util.Log;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class APIClient {

    private static final String BASE_URL = "https://roomify-backend-2.onrender.com/";
    private static Retrofit retrofit = null;
    private static TokenManager tokenManager;

    public static void init(TokenManager manager) {
        tokenManager = manager;
    }

    public static void resetClient() {
        retrofit = null;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {

            // Create lenient Gson
            Gson gson = Converters.registerAll(new GsonBuilder())
                    .setLenient()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    .registerTypeAdapter(com.app.roomify.models.User.class,
                            (JsonDeserializer<com.app.roomify.models.User>) (json, typeOfT, context) -> {
                                try {
                                    if (json == null || json.isJsonNull()) return null;

                                    JsonObject jsonObject = json.getAsJsonObject();
                                    com.app.roomify.models.User user = new com.app.roomify.models.User();

                                    if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) {
                                        user.setId(jsonObject.get("id").getAsLong());
                                    }
                                    if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull()) {
                                        user.setName(jsonObject.get("name").getAsString());
                                    }
                                    if (jsonObject.has("email") && !jsonObject.get("email").isJsonNull()) {
                                        user.setEmail(jsonObject.get("email").getAsString());
                                    }
                                    if (jsonObject.has("role") && !jsonObject.get("role").isJsonNull()) {
                                        user.setRole(jsonObject.get("role").getAsString());
                                    }
                                    if (jsonObject.has("emailVerified") && !jsonObject.get("emailVerified").isJsonNull()) {
                                        user.setEmailVerified(jsonObject.get("emailVerified").getAsBoolean());
                                    }
                                    return user;
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                    .create();

            // Logging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // CRITICAL: Interceptor to strip recursive JSON data
            Interceptor stripRecursionInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response response = chain.proceed(chain.request());
                    String body = response.body().string();

                    // Remove the recursive "bookings" array completely
                    // This fixes the infinite recursion issue
                    body = body.replaceAll("\"bookings\":\\[[^\\[\\]]*\\]", "\"bookings\":[]");

                    // Remove deeply nested structures
                    int maxIterations = 10;
                    for (int i = 0; i < maxIterations; i++) {
                        String newBody = body.replaceAll("\"bookings\":\\[\\{.*?\"bookings\":\\[.*?\\].*?\\}\\]", "\"bookings\":[]");
                        if (newBody.equals(body)) break;
                        body = newBody;
                    }

                    // Also remove any user objects that contain bookings recursively
                    body = body.replaceAll("\"user\":\\{[^}]*\"bookings\":\\[[^\\]]*\\][^}]*\\}", "\"user\":null");

                    // Limit body size to prevent OOM (max 2MB)
                    if (body.length() > 2000000) {
                        body = body.substring(0, 2000000) + "}";
                    }

                    Log.d("APIClient", "Response cleaned, original length: " + response.body().contentLength() + ", cleaned length: " + body.length());

                    return response.newBuilder()
                            .body(ResponseBody.create(response.body().contentType(), body))
                            .build();
                }
            };

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(stripRecursionInterceptor)
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS);

            // Add token interceptor
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();

                if (tokenManager != null) {
                    String token = tokenManager.getToken();

                    if (token != null && !token.isEmpty()) {
                        original = original.newBuilder()
                                .addHeader("Authorization", "Bearer " + token)
                                .build();
                    }
                }

                return chain.proceed(original);
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient.build())
                    .build();
        }

        return retrofit;
    }
}