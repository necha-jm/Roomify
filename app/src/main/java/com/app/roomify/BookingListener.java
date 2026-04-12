package com.app.roomify;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.app.roomify.models.ApiResponse;
import com.app.roomify.models.BookingResponse;
import com.app.roomify.network.APIClient;
import com.app.roomify.network.APIInterface;
import com.app.roomify.network.TokenManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingListener {

    private static final String TAG = "BookingListener";
    private static ScheduledExecutorService scheduler;
    private static boolean isListening = false;
    private static Set<Long> processedBookingIds = new HashSet<>();
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void listenForNewBookings(TokenManager tokenManager, long userId, String role, OnNewBookingListener listener) {
        if (isListening) {
            Log.d(TAG, "Already listening for bookings");
            return;
        }

        isListening = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();

        Log.d(TAG, "Started listening for bookings. UserId: " + userId + ", Role: " + role);

        // Poll every 3 seconds for new bookings
        scheduler.scheduleAtFixedRate(() -> {
            if (!isListening) return;

            try {
                APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);
                Call<ApiResponse<List<BookingResponse>>> call;

                // Choose endpoint based on role
                if ("owner".equals(role)) {
                    call = apiInterface.getOwnerBookings(userId);
                } else {
                    call = apiInterface.getUserBookings(userId);
                }

                call.enqueue(new Callback<ApiResponse<List<BookingResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<BookingResponse>>> call,
                                           Response<ApiResponse<List<BookingResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<BookingResponse> bookings = response.body().getData();

                            if (bookings != null && !bookings.isEmpty()) {
                                // Check for new pending bookings
                                for (BookingResponse booking : bookings) {
                                    if (!processedBookingIds.contains(booking.getId()) && "PENDING".equals(booking.getStatus())) {
                                        processedBookingIds.add(booking.getId());
                                        Log.d(TAG, "New booking found: ID=" + booking.getId());

                                        mainHandler.post(() -> {
                                            if (listener != null) {
                                                listener.onNewBooking(booking);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<BookingResponse>>> call, Throwable t) {
                        Log.e(TAG, "Failed to check bookings: " + t.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in booking listener: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    public static void stopListening() {
        isListening = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            Log.d(TAG, "Stopped listening for bookings");
        }
        processedBookingIds.clear();
    }

    public interface OnNewBookingListener {
        void onNewBooking(BookingResponse booking);
    }
}