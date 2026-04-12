package com.app.roomify.network;

import com.app.roomify.BookingRequest;
import com.app.roomify.NotificationRequest;
import com.app.roomify.Room;
import com.app.roomify.models.*;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface APIInterface {

    // ==================== AUTH ENDPOINTS ====================

    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("/api/auth/google")
    Call<AuthResponse> googleLogin(@Body GoogleLoginRequest request);

    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/api/auth/guest")
    Call<AuthResponse> guestLogin();

    @POST("/api/auth/logout")
    Call<AuthResponse> logout(@Header("Authorization") String token);

    @GET("/api/auth/me")
    Call<AuthResponse> getCurrentUser(@Header("Authorization") String token);

    @POST("/api/auth/forgot-password")
    Call<AuthResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @GET("/api/auth/test-token")
    Call<AuthResponse> testToken(@Header("Authorization") String authToken);


    // ==================== NOTIFICATIONS ====================

    @POST("/api/notifications/register-token")
    Call<ApiResponse<Void>> registerFcmToken(
            @Query("userId") Long userId,
            @Query("fcmToken") String fcmToken,
            @Query("deviceId") String deviceId,
            @Query("deviceModel") String deviceModel,
            @Query("osVersion") String osVersion,
            @Query("appVersion") String appVersion
    );

    @DELETE("/api/notifications/unregister-token")
    Call<ApiResponse<Void>> unregisterFcmToken(@Query("userId") Long userId);

    @POST("/api/notifications/send-room-notification")
    Call<ApiResponse<Void>> sendRoomNotification(@Query("roomId") Long roomId);

    @POST("/api/notifications/send-booking-notification")
    Call<ApiResponse<Void>> sendBookingNotification(
            @Query("ownerId") Long ownerId,
            @Query("roomId") Long roomId,
            @Query("tenantName") String tenantName
    );

    @POST("/api/notifications/send")
    Call<ApiResponse<Void>> sendNotification(@Body NotificationRequest notification);


    // ==================== ROOM ENDPOINTS ====================

    @POST("/api/rooms")
    Call<Room> createRoom(@Body Room request);

    @GET("/api/rooms")
    Call<List<Room>> getAllRooms();

    @GET("/api/rooms/{id}")
    Call<Room> getRoomById(@Path("id") long id);

    @PUT("/api/rooms/{id}")
    Call<Room> updateRoom(@Path("id") long id, @Body Room room);

    @DELETE("/api/rooms/{id}")
    Call<ApiResponse<Void>> deleteRoom(@Path("id") long id);

    @GET("/api/rooms/owner/{ownerId}")
    Call<List<Room>> getRoomsByOwner(@Path("ownerId") long ownerId);

    @GET("/api/rooms/{roomId}/bookings/count")
    Call<ApiResponse<Integer>> getBookingsCountByRoom(@Path("roomId") long roomId);


    // ==================== BOOKING ENDPOINTS (FIXED) ====================

    // GET USER BOOKINGS
    @GET("/api/bookings/user/{userId}")
    Call<ApiResponse<List<BookingResponse>>> getUserBookings(
            @Path("userId") long userId
    );

    // GET OWNER BOOKINGS
    @GET("/api/bookings/owner/{ownerId}")
    Call<ApiResponse<List<BookingResponse>>> getOwnerBookings(
            @Path("ownerId") long ownerId
    );

    // CHECK USER BOOKING
    @GET("/api/bookings/user/{userId}/room/{roomId}")
    Call<ApiResponse<List<BookingResponse>>> checkUserBooking(
            @Path("userId") long userId,
            @Path("roomId") long roomId
    );

    // CREATE BOOKING (IMPORTANT FIX)
    @POST("/api/bookings")
    Call<ApiResponse<BookingResponse>> createBooking(
            @Body BookingRequest booking
    );

    @PUT("/api/bookings/{bookingId}/accept")
    Call<ApiResponse<Void>> acceptBooking(@Path("bookingId") long bookingId);

    @PUT("/api/bookings/{bookingId}/reject")
    Call<ApiResponse<Void>> rejectBooking(@Path("bookingId") long bookingId);

    @PUT("/api/bookings/{bookingId}/cancel")
    Call<ApiResponse<Void>> cancelBooking(@Path("bookingId") long bookingId);

    @DELETE("/api/bookings/{bookingId}")
    Call<ApiResponse<Void>> deleteBooking(@Path("bookingId") long bookingId);


    // ==================== FAVORITES ====================

    @GET("/api/favorites/{userId}/{roomId}")
    Call<ApiResponse<Boolean>> isFavorite(
            @Path("userId") long userId,
            @Path("roomId") long roomId
    );

    @POST("/api/favorites/{userId}/{roomId}/toggle")
    Call<ApiResponse<Void>> toggleFavorite(
            @Path("userId") long userId,
            @Path("roomId") long roomId
    );

    @GET("/api/favorites/{userId}")
    Call<ApiResponse<List<Room>>> getUserFavorites(
            @Path("userId") long userId
    );


    // ==================== USERS ====================

    @GET("/api/users/{id}")
    Call<ApiResponse<User>> getUserById(@Path("id") long id);


    // ==================== MEDIA UPLOAD ====================

    @Multipart
    @POST("/api/rooms/{roomId}/images")
    Call<ApiResponse<List<String>>> uploadRoomImages(
            @Path("roomId") long roomId,
            @Part MultipartBody.Part images
    );

    @Multipart
    @POST("/api/rooms/{roomId}/video")
    Call<ApiResponse<String>> uploadRoomVideo(
            @Path("roomId") long roomId,
            @Part MultipartBody.Part video
    );

    @Multipart
    @POST("/api/rooms/{roomId}/contract")
    Call<ApiResponse<String>> uploadRoomContract(
            @Path("roomId") long roomId,
            @Part MultipartBody.Part contract
    );
}