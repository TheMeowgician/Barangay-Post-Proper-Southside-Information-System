package com.example.barangayinformationsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // For Laravel API (development)
    //public static final String BASE_URL = "http://10.0.2.2:8000/api/";

    // For Connecting to Heroku
    public static final String BASE_URL = "https://postproperadminlaravel-a3c73529c6b6.herokuapp.com/api/";

    // Use for legacy apis
    //public static final String BASE_URL = "http://10.0.2.2/PostProperAdmin/";

    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;

    static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            // Create logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .build();
        }
        return okHttpClient;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}