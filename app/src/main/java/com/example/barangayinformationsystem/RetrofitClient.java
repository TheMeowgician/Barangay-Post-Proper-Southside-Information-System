package com.example.barangayinformationsystem;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Use the appropriate base URL
    private static final String BASE_URL = "http://10.0.2.2/PostProperAdmin/"; // For Android Emulator
    // If testing on a physical device, replace with your computer's local IP:
    //private static final String BASE_URL = "http://192.168.100.45/PostProperAdmin/";

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // Ensure this URL is correct
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}

