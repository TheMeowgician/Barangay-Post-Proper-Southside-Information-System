package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private ProgressBar progressBar;
    private int totalTasks = 5; // Total number of initialization tasks
    private int completedTasks = 0;
    private SharedPreferences prefs;
    private boolean isUserLoggedIn = false;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Initialize SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Check if user is already logged in
        isUserLoggedIn = prefs.contains("user_id");
        if (isUserLoggedIn) {
            userId = prefs.getInt("user_id", -1);
        }
        
        initializeComponents();
        startLoading();
    }

    private void initializeComponents() {
        progressBar = findViewById(R.id.loadingProgressBar);
        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    private void startLoading() {
        new InitializationTask().execute();
    }
    
    private void updateProgress() {
        completedTasks++;
        int progress = (completedTasks * 100) / totalTasks;
        runOnUiThread(() -> progressBar.setProgress(progress));
        
        if (completedTasks >= totalTasks) {
            // Slight delay before launching the appropriate activity
            new Handler(Looper.getMainLooper()).postDelayed(this::determineNextActivity, 300);
        }
    }
    
    private void determineNextActivity() {
        if (isUserLoggedIn && userId != -1) {
            // User is logged in, check verification status
            checkUserVerificationStatus(userId);
        } else {
            // User is not logged in, go to ChooseActivity
            startChooseActivity();
        }
    }
    
    private void checkUserVerificationStatus(int userId) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<VerificationResponse> call = apiService.checkVerificationStatus(userId);

        call.enqueue(new Callback<VerificationResponse>() {
            @Override
            public void onResponse(Call<VerificationResponse> call, Response<VerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().getStatus();
                    Log.d(TAG, "User status: " + status);

                    switch(status) {
                        case "verified":
                            // User is verified, go to HomeActivity
                            startHomeActivity();
                            break;
                        case "pending":
                            // User is pending, go to PendingStatusActivity
                            startPendingStatusActivity();
                            break;
                        default:
                            // User is rejected or other status, clear preferences and go to choose screen
                            prefs.edit().clear().apply();
                            startChooseActivity();
                            break;
                    }
                } else {
                    // Error in response, go to ChooseActivity
                    startChooseActivity();
                }
            }

            @Override
            public void onFailure(Call<VerificationResponse> call, Throwable t) {
                Log.e(TAG, "Network error when checking verification status", t);
                // On network error, go to ChooseActivity
                // The user can try logging in again manually
                startChooseActivity();
            }
        });
    }

    private void startChooseActivity() {
        Intent intent = new Intent(SplashActivity.this, ChooseActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void startHomeActivity() {
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void startPendingStatusActivity() {
        Intent intent = new Intent(SplashActivity.this, PendingStatusActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class InitializationTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            // Perform real initialization tasks here
            performTask("Loading application resources", 300);
            performTask("Initializing database connection", 500);
            performTask("Checking network connectivity", 300);
            performTask("Loading user preferences", 400);
            performTask("Preparing application UI", 300);
            
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }
    }
    
    private void performTask(String taskName, int simulatedWorkTime) {
        // This would be where you'd actually perform real initialization work.
        // For now, we're simulating work with a sleep, but in a real app,
        // this would be where you'd load resources, initialize database, etc.
        try {
            Thread.sleep(simulatedWorkTime);
            updateProgress();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}