package com.example.barangayinformationsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Intent;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText nameTextInputEditText, profileUsernameTextInputEditText, profileAddressTextInputEditText,
            profileAgeTextInputEditText, profileGenderTextInputEditText, profileDateOfBirthTextInputEditText,
            profilePasswordTextInputEditText;
    private TextView usernameTextView;
    private ApiService apiService;
    private int userId; // This should come from the login session or intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        usernameTextView = findViewById(R.id.usernameTextView);
        nameTextInputEditText = findViewById(R.id.nameTextInputEditText);
        profileUsernameTextInputEditText = findViewById(R.id.profileUsernameTextInputEditText);
        profileAddressTextInputEditText = findViewById(R.id.profileAddressTextInputEditText);
        profileAgeTextInputEditText = findViewById(R.id.profileAgeTextInputEditText);
        profileGenderTextInputEditText = findViewById(R.id.profileGenderTextInputEditText);
        profileDateOfBirthTextInputEditText = findViewById(R.id.profileDateOfBirthTextInputEditText);
        profilePasswordTextInputEditText = findViewById(R.id.profilePasswordTextInputEditText);

        // Initialize API service
        apiService = RetrofitClient.getApiService();


        // Get user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("user_id", -1);
        if (userId != -1) {
            fetchUserDetails(userId); // Fetch details if userId is valid
        } else {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
        }

    }

    private void fetchUserDetails(int userId) {
        // Make API call to get user details using Retrofit
        apiService.getUserDetails(userId).enqueue(new Callback<UserDetailsResponse>() {
            @Override
            public void onResponse(Call<UserDetailsResponse> call, Response<UserDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDetailsResponse userDetails = response.body();
                    if ("success".equals(userDetails.getStatus())) {
                        UserDetailsResponse.User user = userDetails.getUser();

                        runOnUiThread(() -> {
                            // Set the user details to the TextInputEditTexts
                            usernameTextView.setText(user.getFirstName() + " " + user.getLastName());
                            nameTextInputEditText.setText(user.getFirstName() + " " + user.getLastName());
                            profileUsernameTextInputEditText.setText(user.getUsername());
                            profileAddressTextInputEditText.setText(user.getAddress());
                            profileAgeTextInputEditText.setText(String.valueOf(user.getAge()));
                            profileGenderTextInputEditText.setText(user.getGender());
                            profileDateOfBirthTextInputEditText.setText(user.getDateOfBirth());
                            profilePasswordTextInputEditText.setText(user.getPassword());
                        });

                    } else {
                        Toast.makeText(ProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserDetailsResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
