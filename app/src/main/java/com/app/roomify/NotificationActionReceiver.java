package com.app.roomify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActionReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("ACTION_ACCEPT_BOOKING".equals(action)) {
            String bookingId = intent.getStringExtra("booking_id");
            String roomId = intent.getStringExtra("room_id");
            acceptBooking(context, bookingId, roomId);
        } else if ("ACTION_REJECT_BOOKING".equals(action)) {
            String bookingId = intent.getStringExtra("booking_id");
            String roomId = intent.getStringExtra("room_id");
            rejectBooking(context, bookingId, roomId);
        }
    }

    private void acceptBooking(Context context, String bookingId, String roomId) {
        TokenManager tokenManager = TokenManager.getInstance(context);
        APIClient.init(tokenManager);
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

        Call<ApiResponse<Void>> call = apiInterface.acceptBooking(Long.parseLong(bookingId));
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(context, "Booking accepted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to accept booking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectBooking(Context context, String bookingId, String roomId) {
        TokenManager tokenManager = TokenManager.getInstance(context);
        APIClient.init(tokenManager);
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);

        Call<ApiResponse<Void>> call = apiInterface.rejectBooking(Long.parseLong(bookingId));
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(context, "Booking rejected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to reject booking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}