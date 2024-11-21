package com.example.barangayinformationsystem;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("Android_login.php")  // This is correct
    Call<LoginResponse> loginUser(
            @Field("username") String username,
            @Field("password") String password
    );

    @FormUrlEncoded
    @GET("fetch_user_details.php")
    Call<UserDetailsResponse> getUserDetails(@Query("id") int userId);

}