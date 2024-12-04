package com.example.barangayinformationsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingStatusActivity extends AppCompatActivity {
    private TextView statusMessageTextView;
    private Button logoutButton;
    private Handler verificationCheckHandler = new Handler();
    private static final long CHECK_INTERVAL = 30000; // 30 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_status);

        statusMessageTextView = findViewById(R.id.statusMessageTextView);
        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            // Clear shared preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().clear().apply();

            // Return to login activity
            Intent intent = new Intent(PendingStatusActivity.this, LogInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Start checking for verification
        startVerificationCheck();
    }

    private void startVerificationCheck() {
        Runnable verificationRunnable = new Runnable() {
            @Override
            public void run() {
                checkVerificationStatus();
                verificationCheckHandler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        verificationCheckHandler.post(verificationRunnable);
    }

    private void checkVerificationStatus() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("user_id", -1);

        if (userId != -1) {
            ApiService apiService = RetrofitClient.getApiService();
            Call<VerificationResponse> call = apiService.checkVerificationStatus(userId);
            call.enqueue(new Callback<VerificationResponse>() {
                @Override
                public void onResponse(Call<VerificationResponse> call, Response<VerificationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if ("verified".equals(response.body().getStatus())) {
                            showVerificationDialog();
                        }
                    }
                }

                @Override
                public void onFailure(Call<VerificationResponse> call, Throwable t) {
                    // Handle error silently
                }
            });
        }
    }

    private void showVerificationDialog() {
        verificationCheckHandler.removeCallbacksAndMessages(null);

        new AlertDialog.Builder(this)
                .setTitle("Account Verified!")
                .setMessage("Your account has been verified. You can now log in to access the app.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    // Clear shared preferences
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().clear().apply();

                    // Return to login activity
                    Intent intent = new Intent(PendingStatusActivity.this, LogInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        verificationCheckHandler.removeCallbacksAndMessages(null);
    }
}