package com.app.roomify;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.app.roomify.network.APIClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.app.roomify.network.TokenManager;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

@GlideModule
public class RoomifyGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        TokenManager tokenManager = TokenManager.getInstance(context);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String url = original.url().toString();
                    
                    // Add authentication header for backend requests
                    if (url.startsWith(APIClient.BASE_URL)) {
                        String token = tokenManager.getToken();
                        if (token != null && !token.isEmpty()) {
                            Request authenticated = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(authenticated);
                        }
                    }
                    return chain.proceed(original);
                })
                .build();

        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
