package com.example.barangayinformationsystem;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Use for new laravel apis
    public static final String BASE_URL = "http://10.0.2.2:8000/api/";

    // Use for legacy apis
    //public static final String BASE_URL = "http://10.0.2.2/PostProperAdmin/";

    // If testing on a physical device, replace with your computer's local IP:
    //public static final String BASE_URL = "http://192.168.100.45/PostProperAdmin/";

    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            // Create logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .build();
        }
        return okHttpClient;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}