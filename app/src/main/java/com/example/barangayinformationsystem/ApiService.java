package com.example.barangayinformationsystem;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @FormUrlEncoded
    @POST("login")
    Call<LoginResponse> loginUser(
            @Field("username") String username,
            @Field("password") String password
    );

    @GET("check_verification_status")
    Call<VerificationResponse> checkVerificationStatus(@Query("user_id") int userId);

    @GET("user/{id}")
    Call<UserDetailsResponse> getUserDetails(@Path("id") int id);

    @Multipart
    @POST("register_user.php")
    Call<RegistrationResponse> registerUser(
            @Part("firstName") RequestBody firstName,
            @Part("lastName") RequestBody lastName,
            @Part("username") RequestBody username,
            @Part("password") RequestBody password,
            @Part("age") RequestBody age,
            @Part("birthday") RequestBody birthday,
            @Part("adrHouseNo") RequestBody houseNumber,
            @Part("adrZone") RequestBody zone,
            @Part("adrStreet") RequestBody street,
            @Part("gender") RequestBody gender,
            @Part MultipartBody.Part validId,
            @Part MultipartBody.Part validIdBack
    );

    // Add this to your existing ApiService interface
    @GET("get_announcements.php")
    Call<List<AnnouncementResponse>> getAnnouncements();

    @FormUrlEncoded
    @POST("submit_incident_report.php")
    Call<IncidentReportResponse> submitIncidentReport(
            @Field("user_id") int userId,
            @Field("title") String title,
            @Field("description") String description,
            @Field("incident_picture") String encodedImage
    );

    @FormUrlEncoded
    @POST("update_user_activity.php")
    Call<ActivityResponse> updateUserActivity(
            @Field("user_id") int userId
    );

    @Multipart
    @POST("update_user_profile.php")
    Call<UpdateProfileResponse> updateUserProfile(
            @Part("user_id") RequestBody userId,
            @Part("username") RequestBody username,
            @Part("adrHouseNo") RequestBody houseNo,
            @Part("adrStreet") RequestBody street,
            @Part("adrZone") RequestBody zone,
            @Part("password") RequestBody password,
            @Part("currentPassword") RequestBody currentPassword,
            @Part MultipartBody.Part profilePicture
    );

    @FormUrlEncoded
    @POST("submit_document_request.php")
    Call<DocumentRequestResponse> submitDocumentRequest(
            @Field("userId") int userId,
            @Field("documentType") String documentType,
            @Field("name") String name,
            @Field("address") String address,
            @Field("tin") String tin,
            @Field("ctc") String ctc,
            @Field("alias") String alias,
            @Field("age") int age,
            @Field("birthday") String birthday,
            @Field("placeOfBirth") String placeOfBirth,  // New field
            @Field("occupation") String occupation,       // New field
            @Field("lengthOfStay") int lengthOfStay,
            @Field("citizenship") String citizenship,
            @Field("gender") String gender,
            @Field("civilStatus") String civilStatus,
            @Field("purpose") String purpose,
            @Field("quantity") int quantity
    );

    @Multipart
    @POST("upload_requirements.php")
    Call<UploadRequirementsResponse> uploadRequirements(
            @Part("requestId") RequestBody requestId,
            @Part("quantity") RequestBody quantity,
            @Part MultipartBody.Part frontId,
            @Part MultipartBody.Part backId
    );

    @GET("get_user_requests.php")
    Call<DocumentRequestListResponse> getUserRequests(@Query("userId") int userId);

    @POST("cancel_request.php")
    @FormUrlEncoded
    Call<DocumentRequestResponse> cancelRequest(
            @Field("requestId") int requestId,
            @Field("reason") String reason
    );

    @FormUrlEncoded
    @POST("send_message.php")
    Call<MessageResponse> sendMessage(
            @Field("message") String message,
            @Field("sender_id") int senderId,
            @Field("is_admin") int isAdmin
    );

    @FormUrlEncoded
    @POST("send_user_message.php")
    Call<MessageResponse> sendMessage(
            @Field("message") String message,
            @Field("sender_id") int senderId
    );


    @GET("get_messages.php")
    Call<List<ChatMessage>> getMessages(
            @Query("user_id") int userId
    );

    @GET("check_new_messages.php")
    Call<MessageCheckResponse> checkNewMessages(
            @Query("user_id") int userId,
            @Query("last_message_timestamp") long lastMessageTimestamp
    );

    @GET("get_user_incident_reports.php")
    Call<IncidentReportListResponse> getUserIncidentReports(@Query("userId") int userId);
}

