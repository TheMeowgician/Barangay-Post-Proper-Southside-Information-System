package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChooseActivity extends AppCompatActivity {

    Button logInButton;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        initializeComponents();

        // Check if user is already logged in
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains("user_id")) {
            // Get user ID and check verification status
            int userId = prefs.getInt("user_id", -1);
            if (userId != -1) {
                showLoadingState(); // You might want to add a loading indicator
                checkUserStatus(userId);
            }
        }
    }

    private void showLoadingState() {
        // Add loading indicator if needed
        logInButton.setEnabled(false);
        registerButton.setEnabled(false);
    }

    private void hideLoadingState() {
        logInButton.setEnabled(true);
        registerButton.setEnabled(true);
    }

    private void checkUserStatus(int userId) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<VerificationResponse> call = apiService.checkVerificationStatus(userId);

        call.enqueue(new Callback<VerificationResponse>() {
            @Override
            public void onResponse(Call<VerificationResponse> call, Response<VerificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().getStatus();
                    Log.d("ChooseActivity", "User status: " + status);

                    switch(status) {
                        case "verified":
                            // User is verified, go to HomeActivity
                            Intent homeIntent = new Intent(ChooseActivity.this, HomeActivity.class);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(homeIntent);
                            finish();
                            break;

                        case "pending":
                            // User is pending, go to PendingStatusActivity
                            Intent pendingIntent = new Intent(ChooseActivity.this, PendingStatusActivity.class);
                            pendingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(pendingIntent);
                            finish();
                            break;

                        default:
                            // User is rejected or other status, clear preferences
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ChooseActivity.this);
                            prefs.edit().clear().apply();
                            hideLoadingState();
                            break;
                    }
                } else {
                    hideLoadingState();
                }
            }

            @Override
            public void onFailure(Call<VerificationResponse> call, Throwable t) {
                Log.e("ChooseActivity", "Network error", t);
                hideLoadingState();
            }
        });
    }

    public void moveToLogInActivity(View view) {
        Intent intent = new Intent(ChooseActivity.this, LogInActivity.class);
        startActivity(intent);
    }

    public void moveToRegistrationActivity(View view) {
        Intent intent = new Intent(ChooseActivity.this, TermsAndConditionsActivity.class);
        startActivity(intent);
    }

    private void initializeComponents() {
        logInButton = findViewById(R.id.logInButton);
        registerButton = findViewById(R.id.registerButton);
    }
    @Override
    public void onBackPressed() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_exit_confirmation);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button noButton = dialog.findViewById(R.id.noButton);
        Button yesButton = dialog.findViewById(R.id.yesButton);

        noButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }
}