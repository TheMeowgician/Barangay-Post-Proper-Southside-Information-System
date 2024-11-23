package com.example.barangayinformationsystem;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @FormUrlEncoded
    @POST("Android_login.php")  // This is correct
    Call<LoginResponse> loginUser(
            @Field("username") String username,
            @Field("password") String password
    );

    @GET("fetch_user_details.php")
    Call<UserDetailsResponse> getUserDetails(@Query("id") int userId);

    @FormUrlEncoded
    @POST("register_user.php")
    Call<RegistrationResponse> registerUser(
            @Field("firstName") String firstName,
            @Field("lastName") String lastName,
            @Field("username") String username,
            @Field("password") String password,
            @Field("age") int age,
            @Field("birthday") String birthday,
            @Field("adrHouseNo") String houseNumber,
            @Field("adrZone") String zone,
            @Field("adrStreet") String street
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

    @FormUrlEncoded
    @POST("submit_document_request.php")
    Call<DocumentRequestResponse> submitDocumentRequest(
            @Field("documentType") String documentType,
            @Field("name") String name,
            @Field("address") String address,
            @Field("tin") String tin,
            @Field("ctc") String ctc,
            @Field("alias") String alias,
            @Field("age") int age,
            @Field("birthday") String birthday,
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
            @Part MultipartBody.Part validId
    );
}

