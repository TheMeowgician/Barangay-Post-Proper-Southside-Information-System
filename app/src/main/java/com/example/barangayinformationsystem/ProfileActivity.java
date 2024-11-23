package com.example.barangayinformationsystem;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.IOUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private TextView usernameTextView;
    private TextInputEditText nameTextInputEditText, profileUsernameTextInputEditText,
            profileHouseNoTextInputEditText, profileStreetTextInputEditText, profileZoneTextInputEditText,
            profileAddressTextInputEditText, profileAgeTextInputEditText, profileGenderTextInputEditText,
            profileDateOfBirthTextInputEditText, profilePasswordTextInputEditText,
            profileConfirmPasswordTextInputEditText;
    private ImageView profileImageView;
    private Uri selectedImageUri;
    private Button saveChangesButton;
    private ApiService apiService;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        initializeViews();

        // Initialize API service
        apiService = RetrofitClient.getApiService();

        // Setup click listeners
        findViewById(R.id.editProfilePictureButton).setOnClickListener(v -> checkPermissionAndPickImage());
        saveChangesButton.setOnClickListener(v -> validateAndSaveChanges());

        // Get user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("user_id", -1);
        if (userId != -1) {
            fetchUserDetails(userId);
        } else {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        usernameTextView = findViewById(R.id.usernameTextView);
        nameTextInputEditText = findViewById(R.id.nameTextInputEditText);
        profileUsernameTextInputEditText = findViewById(R.id.profileUsernameTextInputEditText);
        profileAddressTextInputEditText = findViewById(R.id.profileAddressTextInputEditText);
        profileAgeTextInputEditText = findViewById(R.id.profileAgeTextInputEditText);
        profileGenderTextInputEditText = findViewById(R.id.profileGenderTextInputEditText);
        profileDateOfBirthTextInputEditText = findViewById(R.id.profileDateOfBirthTextInputEditText);
        profilePasswordTextInputEditText = findViewById(R.id.profilePasswordTextInputEditText);
        profileHouseNoTextInputEditText = findViewById(R.id.profileHouseNoTextInputEditText);
        profileStreetTextInputEditText = findViewById(R.id.profileStreetTextInputEditText);
        profileZoneTextInputEditText = findViewById(R.id.profileZoneTextInputEditText);
        profileConfirmPasswordTextInputEditText = findViewById(R.id.profileConfirmPasswordTextInputEditText);
        profileImageView = findViewById(R.id.profileImageView);
        saveChangesButton = findViewById(R.id.editProfileButton);
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            pickImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri);
        }
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void fetchUserDetails(int userId) {
        apiService.getUserDetails(userId).enqueue(new Callback<UserDetailsResponse>() {
            @Override
            public void onResponse(Call<UserDetailsResponse> call, Response<UserDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDetailsResponse userDetails = response.body();
                    if ("success".equals(userDetails.getStatus())) {
                        UserDetailsResponse.User user = userDetails.getUser();

                        runOnUiThread(() -> {
                            // Set the user details to the TextInputEditTexts
                            String fullName = user.getFirstName() + " " + user.getLastName();
                            usernameTextView.setText(fullName);
                            nameTextInputEditText.setText(fullName);

                            // Username
                            if (user.getUsername() != null) {
                                profileUsernameTextInputEditText.setText(user.getUsername());
                            }

                            // Address fields
                            if (user.getHouseNo() != null) {
                                profileHouseNoTextInputEditText.setText(user.getHouseNo());
                            }
                            if (user.getStreet() != null) {
                                profileStreetTextInputEditText.setText(user.getStreet());
                            }
                            if (user.getZone() != null) {
                                profileZoneTextInputEditText.setText(user.getZone());
                            }
                            if (user.getAddress() != null) {
                                profileAddressTextInputEditText.setText(user.getAddress());
                            }

                            // Other details
                            if (user.getAge() > 0) {
                                profileAgeTextInputEditText.setText(String.valueOf(user.getAge()));
                            }
                            if (user.getGender() != null) {
                                profileGenderTextInputEditText.setText(user.getGender());
                            }
                            if (user.getDateOfBirth() != null) {
                                profileDateOfBirthTextInputEditText.setText(user.getDateOfBirth());
                            }

                            // Profile Picture
                            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                                String imageUrl = RetrofitClient.BASE_URL + user.getProfilePicture();
                                Log.d("ProfileActivity", "Loading profile picture from: " + imageUrl);

                                Glide.with(ProfileActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_profile_picture)
                                        .error(R.drawable.default_profile_picture)
                                        .centerCrop()
                                        .into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.default_profile_picture);
                            }

                            // Save to SharedPreferences for persistence
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("houseNo", user.getHouseNo());
                            editor.putString("street", user.getStreet());
                            editor.putString("zone", user.getZone());
                            editor.apply();
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
                Log.e("ProfileActivity", "API call failed", t);
            }
        });
    }

    private void loadProfilePicture(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .centerCrop()
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.default_profile_picture);
        }
    }

    private void uploadProfilePicture(Uri imageUri, final Map<String, String> updates) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = IOUtils.toByteArray(inputStream);
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            updates.put("profile_picture", base64Image);
            updateUserProfile(updates);

        } catch (IOException e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfile(Map<String, String> updates) {
        // Create individual RequestBody objects for each field
        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("username"));
        RequestBody houseNoBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("houseNo"));
        RequestBody streetBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("street"));
        RequestBody zoneBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("zone"));
        RequestBody passwordBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("password"));

        // Handle profile picture
        MultipartBody.Part profilePicturePart = null;
        if (selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                byte[] imageBytes = IOUtils.toByteArray(inputStream);
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
                profilePicturePart = MultipartBody.Part.createFormData("profile_picture", "profile.jpg", requestFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Make the API call
        apiService.updateUserProfile(
                userIdBody,
                usernameBody,
                houseNoBody,
                streetBody,
                zoneBody,
                passwordBody,
                profilePicturePart
        ).enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> call, Response<UpdateProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UpdateProfileResponse updateResponse = response.body();
                    if ("success".equals(updateResponse.getStatus())) {
                        // Update SharedPreferences with new values
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("houseNo", updates.get("houseNo"));
                        editor.putString("street", updates.get("street"));
                        editor.putString("zone", updates.get("zone"));
                        editor.apply();

                        // Update UI
                        UpdateProfileResponse.User updatedUser = updateResponse.getUser();
                        updateUIWithUserData(updatedUser);

                        // Show success dialog
                        SuccessDialog.showSuccess(
                                ProfileActivity.this,
                                "Your profile has been successfully saved. All changes have been applied",
                                null,
                                2000
                        );
                    } else {
                        Toast.makeText(ProfileActivity.this, "Update failed: " + updateResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateProfileResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithUserData(UpdateProfileResponse.User user) {
        usernameTextView.setText(user.getFirstName() + " " + user.getLastName());
        profileUsernameTextInputEditText.setText(user.getUsername());
        profileHouseNoTextInputEditText.setText(user.getHouseNo());
        profileStreetTextInputEditText.setText(user.getStreet());
        profileZoneTextInputEditText.setText(user.getZone());

        // Update profile picture
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            String baseUrl = RetrofitClient.BASE_URL;
            String imageUrl = baseUrl + user.getProfilePicture();
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .centerCrop()
                    .into(profileImageView);
        }
    }


    private void validateAndSaveChanges() {
        String password = profilePasswordTextInputEditText.getText().toString();
        String confirmPassword = profileConfirmPasswordTextInputEditText.getText().toString();

        if (!password.equals(confirmPassword)) {
            profileConfirmPasswordTextInputEditText.setError("Passwords do not match");
            return;
        }

        // Create a map of updated fields
        Map<String, String> updates = new HashMap<>();
        updates.put("username", profileUsernameTextInputEditText.getText().toString());
        updates.put("houseNo", profileHouseNoTextInputEditText.getText().toString());
        updates.put("street", profileStreetTextInputEditText.getText().toString());
        updates.put("zone", profileZoneTextInputEditText.getText().toString());
        updates.put("password", password);

        // If image is selected, upload it first
        if (selectedImageUri != null) {
            uploadProfilePicture(selectedImageUri, updates);
        } else {
            updateUserProfile(updates);
        }
    }
    }

