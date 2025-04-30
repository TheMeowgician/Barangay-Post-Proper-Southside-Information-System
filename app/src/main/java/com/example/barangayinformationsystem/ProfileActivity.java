package com.example.barangayinformationsystem;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.IOUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import android.content.Intent;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
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
            profileDateOfBirthTextInputEditText, profileCurrentPasswordTextInputEditText,
            profileNewPasswordTextInputEditText, profileConfirmNewPasswordTextInputEditText;
    private ImageView profileImageView;
    private Uri selectedImageUri;
    private Button saveChangesButton;
    private ProgressBar progressBar;
    private ApiService apiService;
    private int userId;
    // Add a class-level variable to store the selected image bitmap
    private Bitmap selectedImageBitmap = null;

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
        profileHouseNoTextInputEditText = findViewById(R.id.profileHouseNoTextInputEditText);
        profileStreetTextInputEditText = findViewById(R.id.profileStreetTextInputEditText);
        profileZoneTextInputEditText = findViewById(R.id.profileZoneTextInputEditText);
        profileCurrentPasswordTextInputEditText = findViewById(R.id.profileCurrentPasswordTextInputEditText);
        profileNewPasswordTextInputEditText = findViewById(R.id.profileNewPasswordTextInputEditText);
        profileConfirmNewPasswordTextInputEditText = findViewById(R.id.profileConfirmNewPasswordTextInputEditText);
        profileImageView = findViewById(R.id.profileImageView);
        saveChangesButton = findViewById(R.id.editProfileButton);
        progressBar = findViewById(R.id.progressBar); // Make sure to add this to your layout
    }

    private void checkPermissionAndPickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and higher
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                pickImage();
            }
        } else {
            // For Android 12 and lower
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                pickImage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                // Show a more helpful message
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                                Manifest.permission.READ_MEDIA_IMAGES :
                                Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this,
                            "Storage permission is needed to select a profile picture",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Permission permanently denied
                    Toast.makeText(this,
                            "Permission denied. Please enable in Settings to change profile picture",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // Store the bitmap for later use
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    
                    // Display the selected image
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.default_profile_picture)
                            .error(R.drawable.default_profile_picture)
                            .centerCrop()
                            .into(profileImageView);
                } catch (IOException e) {
                    Log.e("ProfileActivity", "Error loading selected image", e);
                    Toast.makeText(this, "Error loading selected image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private byte[] compressImage(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveChangesButton.setEnabled(!show);
        saveChangesButton.setText(show ? "Updating..." : "Save Changes");
    }

    private void fetchUserDetails(int userId) {
        showLoading(true);
        apiService.getUserDetails(userId).enqueue(new Callback<UserDetailsResponse>() {
            @Override
            public void onResponse(Call<UserDetailsResponse> call, Response<UserDetailsResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    UserDetailsResponse userDetails = response.body();
                    if ("success".equals(userDetails.getStatus())) {
                        UserDetailsResponse.User user = userDetails.getUser();

                        runOnUiThread(() -> {
                            String fullName = user.getFirstName() + " " + user.getLastName();
                            usernameTextView.setText(fullName);
                            nameTextInputEditText.setText(fullName);

                            if (user.getUsername() != null) {
                                profileUsernameTextInputEditText.setText(user.getUsername());
                            }
                            
                            // Set address fields with correct parsing - use hints to show original values
                            String houseNo = user.getHouseNo();
                            String street = user.getStreet();  
                            String zone = user.getZone();
                            
                            if (!TextUtils.isEmpty(houseNo)) {
                                profileHouseNoTextInputEditText.setHint(houseNo);
                                profileHouseNoTextInputEditText.setText("");
                            }
                            if (!TextUtils.isEmpty(street)) {
                                profileStreetTextInputEditText.setHint(street);
                                profileStreetTextInputEditText.setText("");
                            }
                            if (!TextUtils.isEmpty(zone)) {
                                profileZoneTextInputEditText.setHint(zone);
                                profileZoneTextInputEditText.setText("");
                            }
                            
                            // Set the full address in the disabled address field
                            if (user.getAddress() != null) {
                                profileAddressTextInputEditText.setText(user.getFormattedAddress());
                            }
                            
                            // Other fields
                            if (user.getAge() > 0) {
                                profileAgeTextInputEditText.setText(String.valueOf(user.getAge()));
                            }
                            if (user.getGender() != null) {
                                profileGenderTextInputEditText.setText(user.getGender());
                            }
                            if (user.getDateOfBirth() != null) {
                                profileDateOfBirthTextInputEditText.setText(user.getDateOfBirth());
                            }

                            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                                String imageUrl = user.getProfilePicture();
                                loadProfilePicture(imageUrl);
                            } else {
                                profileImageView.setImageResource(R.drawable.default_profile_picture);
                            }

                            // Save the original address values in SharedPreferences for later use
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
                showLoading(false);
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ProfileActivity", "API call failed", t);
            }
        });
    }

    private void loadProfilePicture(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("ProfileActivity", "Loading profile picture from: " + imageUrl);
            
            // Ensure URL is complete with BASE_URL if it's a relative path
            if (!imageUrl.startsWith("http")) {
                imageUrl = RetrofitClient.BASE_URL + imageUrl;
            }
            
            // Add cache busting parameter to force fresh image loading
            String finalUrl = imageUrl + "?t=" + System.currentTimeMillis();
            Log.d("ProfileActivity", "Final image URL with cache busting: " + finalUrl);
            
            Glide.with(this)
                    .load(finalUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .skipMemoryCache(true) // Skip memory cache
                    .centerCrop()
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.default_profile_picture);
        }
    }

    private void uploadProfilePicture(Uri imageUri, final Map<String, String> updates) {
        showLoading(true);
        try {
            byte[] compressedImage = compressImage(imageUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), compressedImage);
            MultipartBody.Part profilePicturePart = MultipartBody.Part.createFormData("profile_picture", "profile.jpg", requestFile);

            updateUserProfile(updates, profilePicturePart);
        } catch (IOException e) {
            showLoading(false);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Image processing failed", e);
        }
    }

    private void updateUserProfile(Map<String, String> updates, @Nullable MultipartBody.Part profilePicturePart) {
        showLoading(true);

        // Debug logging
        Log.d("ProfileActivity", "Updates to be sent: " + updates.toString());

        // Create RequestBody objects for all fields
        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("username"));
        RequestBody houseNoBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("houseNo"));
        RequestBody streetBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("street"));
        RequestBody zoneBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("zone"));

        // Create password-related RequestBody objects if they exist
        RequestBody currentPasswordBody = null;
        RequestBody passwordBody = null;
        if (updates.containsKey("currentPassword") && updates.containsKey("password")) {
            currentPasswordBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("currentPassword"));
            passwordBody = RequestBody.create(MediaType.parse("text/plain"), updates.get("password"));
            Log.d("ProfileActivity", "Including password update in request");
        }

        apiService.updateUserProfile(
                userIdBody,
                usernameBody,
                houseNoBody,
                streetBody,
                zoneBody,
                passwordBody,
                currentPasswordBody,  // Add this parameter
                profilePicturePart
        ).enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> call, Response<UpdateProfileResponse> response) {
                showLoading(false);
                Log.d("ProfileActivity", "Response code: " + response.code());

                if (!response.isSuccessful()) {
                    try {
                        Log.e("ProfileActivity", "Error body: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (response.isSuccessful() && response.body() != null) {
                    UpdateProfileResponse updateResponse = response.body();
                    Log.d("ProfileActivity", "Update response: " + updateResponse.getStatus() + " - " + updateResponse.getMessage());

                    if ("success".equals(updateResponse.getStatus())) {
                        UpdateProfileResponse.User updatedUser = updateResponse.getUser();

                        // Show success dialog
                        SuccessDialog.showSuccess(
                                ProfileActivity.this,
                                "Your profile has been successfully saved. All changes have been applied",
                                null,
                                1500  // Show for 1.5 seconds before proceeding
                        );

                        // Schedule the activity restart after the success dialog is shown
                        new Handler().postDelayed(() -> {
                            // Restart the activity to reload everything with fresh data
                            Intent intent = getIntent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            finish();
                            startActivity(intent);
                            overridePendingTransition(0, 0); // No animation on restart
                        }, 2000); // Wait for 2 seconds to ensure the success dialog is seen
                    } else {
                        Toast.makeText(ProfileActivity.this, "Update failed: " + updateResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateProfileResponse> call, Throwable t) {
                showLoading(false);
                Log.e("ProfileActivity", "Update failed", t);
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithUserData(UpdateProfileResponse.User user) {
        if (user != null) {
            usernameTextView.setText(user.getFirstName() + " " + user.getLastName());
            profileUsernameTextInputEditText.setText(user.getUsername());
            
            // Set address fields as hints, not text
            if (user.getHouseNo() != null) {
                profileHouseNoTextInputEditText.setHint(user.getHouseNo());
                profileHouseNoTextInputEditText.setText("");
            }
            if (user.getStreet() != null) {
                profileStreetTextInputEditText.setHint(user.getStreet());
                profileStreetTextInputEditText.setText("");
            }
            if (user.getZone() != null) {
                profileZoneTextInputEditText.setHint(user.getZone());
                profileZoneTextInputEditText.setText("");
            }

            // Clear password fields after successful update
            profileNewPasswordTextInputEditText.setText("");
            profileCurrentPasswordTextInputEditText.setText("");
            profileConfirmNewPasswordTextInputEditText.setText("");

            // Update profile picture
            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                String imageUrl = RetrofitClient.BASE_URL + user.getProfilePicture();
                loadProfilePicture(imageUrl);
            }
        }
    }

    private void validateAndSaveChanges() {
        String username = profileUsernameTextInputEditText.getText().toString().trim();
        String houseNo = profileHouseNoTextInputEditText.getText().toString().trim();
        String street = profileStreetTextInputEditText.getText().toString().trim();
        String zone = profileZoneTextInputEditText.getText().toString().trim();
        String currentPassword = profileCurrentPasswordTextInputEditText.getText().toString();
        String newPassword = profileNewPasswordTextInputEditText.getText().toString();
        String confirmNewPassword = profileConfirmNewPasswordTextInputEditText.getText().toString();

        // Basic validation
        if (username.isEmpty()) {
            profileUsernameTextInputEditText.setError("Username cannot be empty");
            return;
        }

        // Password validation
        if (!newPassword.isEmpty()) {
            if (currentPassword.isEmpty()) {
                profileCurrentPasswordTextInputEditText.setError("Current password is required to change password");
                return;
            }
            if (newPassword.length() < 6) {
                profileNewPasswordTextInputEditText.setError("New password must be at least 6 characters");
                return;
            }
            if (!newPassword.equals(confirmNewPassword)) {
                profileConfirmNewPasswordTextInputEditText.setError("Passwords do not match");
                return;
            }
        }

        // Get saved address values from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Create a map of updated fields
        Map<String, String> updates = new HashMap<>();
        updates.put("username", username);
        
        // Use entered value if not empty, otherwise use the stored value from SharedPreferences
        updates.put("houseNo", !houseNo.isEmpty() ? houseNo : prefs.getString("houseNo", ""));
        updates.put("street", !street.isEmpty() ? street : prefs.getString("street", ""));
        updates.put("zone", !zone.isEmpty() ? zone : prefs.getString("zone", ""));

        // Hash passwords if being updated
        if (!newPassword.isEmpty()) {
            String hashedCurrentPassword = PasswordHasher.hashPassword(currentPassword);
            String hashedNewPassword = PasswordHasher.hashPassword(newPassword);
            if (hashedCurrentPassword != null && hashedNewPassword != null) {
                updates.put("currentPassword", hashedCurrentPassword);
                updates.put("password", hashedNewPassword);
            } else {
                Toast.makeText(this, "Error processing password", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // If image is selected, upload it first
        if (selectedImageUri != null) {
            uploadProfilePicture(selectedImageUri, updates);
        } else {
            updateUserProfile(updates, null);
        }
    }
}