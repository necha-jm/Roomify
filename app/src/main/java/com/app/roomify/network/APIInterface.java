package com.app.roomify.network;

import com.app.roomify.BookingRequest;
import com.app.roomify.NotificationRequest;
import com.app.roomify.Room;
import com.app.roomify.RoomCreateRequest;
import com.app.roomify.models.*;

import java.util.List;
import java.util.Map;

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

    // ==================== ANALYTICS ENDPOINTS ====================

    @GET("/api/analytics/monthly-trends")
    Call<Map<String, Object>> getMonthlyTrends(@Header("Authorization") String token);

    @GET("/api/analytics/top-rooms")
    Call<Map<String, Object>> getTopRevenueRooms(@Header("Authorization") String token);

    @GET("/api/analytics/price-ranges")
    Call<Map<String, Object>> getMostFrequentPriceRanges(@Header("Authorization") String token);

    @GET("/api/analytics/most-booked-areas")
    Call<Map<String, Object>> getMostBookedAreas(@Header("Authorization") String token);

    @GET("/api/analytics/exact-prices")
    Call<Map<String, Object>> getMostFrequentExactPrices(@Header("Authorization") String token);

    // ==================== ROOM ENDPOINTS ====================

    @POST("/api/rooms")
    Call<Room> createRoom(@Body RoomCreateRequest request);

    @POST("/api/rooms")
    Call<Room> createRoomLegacy(@Body Room room);




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

    @GET("/api/rooms/{roomId}/is-available")
    Call<ApiResponse<Boolean>> isRoomAvailableForBooking(@Path("roomId") long roomId);

    // ==================== DALALI (AGENT) ENDPOINTS ====================

    @GET("/api/dalali/properties")
    Call<ApiResponse<List<Room>>> getDalaliProperties(@Header("Authorization") String token);

    @GET("/api/dalali/stats")
    Call<ApiResponse<DalaliStats>> getDalaliStats(@Header("Authorization") String token);

    @GET("/api/dalali/properties/pending")
    Call<ApiResponse<List<Room>>> getPendingProperties(@Header("Authorization") String token);

    @GET("/api/dalali/properties/rented")
    Call<ApiResponse<List<Room>>> getRentedProperties(@Header("Authorization") String token);

    @GET("/api/dalali/properties/available")
    Call<ApiResponse<List<Room>>> getAvailableProperties(@Header("Authorization") String token);

    @PUT("/api/dalali/properties/{propertyId}/status")
    Call<ApiResponse<Void>> updatePropertyStatus(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId,
            @Query("status") String status
    );

    @PUT("/api/dalali/properties/{propertyId}/rented")
    Call<ApiResponse<Void>> markAsRented(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId
    );

    @PUT("/api/dalali/properties/{propertyId}/approve")
    Call<ApiResponse<Void>> approveProperty(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId
    );

    @PUT("/api/dalali/properties/{propertyId}/reject")
    Call<ApiResponse<Void>> rejectProperty(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId,
            @Query("reason") String reason
    );

    @GET("/api/dalali/earnings")
    Call<ApiResponse<EarningsResponse>> getDalaliEarnings(@Header("Authorization") String token);

    @GET("/api/dalali/earnings/monthly")
    Call<ApiResponse<Map<String, Double>>> getMonthlyEarnings(
            @Header("Authorization") String token,
            @Query("year") int year
    );

    @GET("/api/dalali/commission/total")
    Call<ApiResponse<Double>> getTotalCommission(@Header("Authorization") String token);

    @GET("/api/dalali/properties/{propertyId}/commission")
    Call<ApiResponse<Double>> getPropertyCommission(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId
    );

    @GET("/api/dalali/profile")
    Call<ApiResponse<User>> getDalaliProfile(@Header("Authorization") String token);

    @PUT("/api/dalali/profile")
    Call<ApiResponse<User>> updateDalaliProfile(
            @Header("Authorization") String token,
            @Body User user
    );

    @GET("/api/dalali/verification-status")
    Call<ApiResponse<String>> getVerificationStatus(@Header("Authorization") String token);

    @Multipart
    @POST("/api/dalali/verify")
    Call<ApiResponse<Void>> submitVerificationDocuments(
            @Header("Authorization") String token,
            @Part MultipartBody.Part licenseImage,
            @Part MultipartBody.Part idImage,
            @Query("licenseNumber") String licenseNumber
    );

    @GET("/api/dalali/properties/{propertyId}/interested-tenants")
    Call<ApiResponse<List<TenantInterest>>> getInterestedTenants(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId
    );

    @POST("/api/dalali/properties/{propertyId}/contact-tenant")
    Call<ApiResponse<Void>> contactTenant(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId,
            @Query("tenantId") long tenantId,
            @Body MessageRequest message
    );

    @GET("/api/dalali/messages")
    Call<ApiResponse<List<Conversation>>> getConversations(@Header("Authorization") String token);

    @POST("/api/dalali/properties/{propertyId}/featured")
    Call<ApiResponse<Void>> markAsFeatured(
            @Header("Authorization") String token,
            @Path("propertyId") long propertyId
    );

    @GET("/api/dalali/analytics/views")
    Call<ApiResponse<Map<String, Object>>> getPropertyViewsAnalytics(@Header("Authorization") String token);

    @GET("/api/dalali/analytics/top-properties")
    Call<ApiResponse<List<Room>>> getTopPerformingProperties(@Header("Authorization") String token);

    // ==================== BOOKING ENDPOINTS ====================

    @GET("/api/bookings/user/{userId}")
    Call<ApiResponse<List<BookingResponse>>> getUserBookings(
            @Path("userId") long userId
    );

    @GET("/api/bookings/owner/{ownerId}")
    Call<ApiResponse<List<BookingResponse>>> getOwnerBookings(
            @Path("ownerId") long ownerId
    );

    @GET("/api/bookings/user/{userId}/room/{roomId}")
    Call<ApiResponse<List<BookingResponse>>> checkUserBooking(
            @Path("userId") long userId,
            @Path("roomId") long roomId
    );

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

    @GET("/api/bookings/room/{roomId}/exists")
    Call<ApiResponse<Boolean>> isRoomBooked(@Path("roomId") long roomId);

    // ==================== FAVORITES ====================

    @GET("/api/favorites/{userId}/{roomId}")
    Call<ApiResponse<Boolean>> isFavorite(
            @Path("userId") long userId,
            @Path("roomId") long roomId
    );

    @POST("/api/favorites/{userId}/{roomId}/toggle")
    Call<ApiResponse<Boolean>> toggleFavorite(
            @Path("userId") long userId,
            @Path("roomId") long roomId
    );

    @GET("/api/favorites/{userId}")
    Call<ApiResponse<List<Room>>> getUserFavorites(
            @Path("userId") long userId
    );

    @GET("/api/rooms")
    Call<List<Room>> getAllRooms();

    // ==================== DALALI (AGENT) ENDPOINTS ====================

    @GET("/api/dalali/properties")
    Call<ApiResponse<List<Room>>> getAgentProperties(@Header("Authorization") String token);

    // ==================== USERS ====================

    @GET("/api/users/profile")
    Call<ApiResponse<User>> getUserProfile(@Header("Authorization") String token);

    @PUT("/api/users/profile")
    Call<ApiResponse<User>> updateUserProfile(
            @Header("Authorization") String token,
            @Body User user
    );

    @GET("/api/users/email/{email}")
    Call<ApiResponse<User>> getUserByEmail(@Path("email") String email);

    @GET("/api/users/{id}")
    Call<ApiResponse<User>> getUserById(@Path("id") long id);

    // ==================== MEDIA UPLOAD ====================

    @Multipart
    @POST("/api/rooms/{roomId}/images")
    Call<ApiResponse<List<String>>> uploadRoomImages(
            @Path("roomId") long roomId,
            @Part List<MultipartBody.Part> images
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