package com.example.barangayinformationsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    ImageButton menuImageButton, closeMenuImageButton;
    DrawerLayout homeDrawerLayout;
    NavigationView navigationView;
    ShapeableImageView profileMiniIconCircleImageView;
    MaterialTextView navHeaderFullNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initializeComponents();
        loadUserDetails();
        updateUserActivity();

        // Handle navigation from document request
        if (getIntent().hasExtra("navigate_to")) {
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if ("document_status".equals(navigateTo)) {
                replaceFragment(new DocumentStatusFragment());
                // Update navigation drawer selection if needed
                navigationView.setCheckedItem(R.id.navDocumentStatus);
            } else {
                // Default to home fragment
                replaceFragment(new HomeFragment());
            }
        } else {
            // Default to home fragment
            replaceFragment(new HomeFragment());
        }
    }

    private void updateUserActivity() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("user_id", -1);

        if (userId != -1) {
            ApiService apiService = RetrofitClient.getApiService();
            Call<ActivityResponse> call = apiService.updateUserActivity(userId);

            call.enqueue(new Callback<ActivityResponse>() {
                @Override
                public void onResponse(Call<ActivityResponse> call, Response<ActivityResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Activity updated successfully
                    }
                }

                @Override
                public void onFailure(Call<ActivityResponse> call, Throwable t) {
                    // Silent failure - we don't want to bother the user with activity tracking errors
                }
            });
        }
    }

    private void loadUserDetails() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("user_id", -1);

        if (userId != -1) {
            ApiService apiService = RetrofitClient.getApiService();
            Call<UserDetailsResponse> call = apiService.getUserDetails(userId);

            call.enqueue(new Callback<UserDetailsResponse>() {
                @Override
                public void onResponse(Call<UserDetailsResponse> call, Response<UserDetailsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UserDetailsResponse userDetailsResponse = response.body();
                        if ("success".equals(userDetailsResponse.getStatus()) && userDetailsResponse.getUser() != null) {
                            updateNavigationHeader(userDetailsResponse.getUser());
                            updateUserActivity();
                        } else {
                            Toast.makeText(HomeActivity.this, "Failed to load user details", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<UserDetailsResponse> call, Throwable t) {
                    Toast.makeText(HomeActivity.this, "Network error: Failed to load user details", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateNavigationHeader(UserDetailsResponse.User user) {
        // Update Navigation Header
        View headerView = navigationView.getHeaderView(0);
        navHeaderFullNameTextView = headerView.findViewById(R.id.navHeaderFullNameTextView);
        ShapeableImageView navHeaderImageView = headerView.findViewById(R.id.navheaderMiniIconCircleImageView);

        String fullName = user.getFirstName() + " " + user.getLastName();
        navHeaderFullNameTextView.setText(fullName);

        // Update both profile pictures (nav header and top bar)
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            String imageUrl = RetrofitClient.BASE_URL + user.getProfilePicture();

            // Load image for navigation header
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .centerCrop()
                    .into(navHeaderImageView);

            // Load image for top bar mini icon
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .centerCrop()
                    .into(profileMiniIconCircleImageView);
        } else {
            navHeaderImageView.setImageResource(R.drawable.default_profile_picture);
            profileMiniIconCircleImageView.setImageResource(R.drawable.default_profile_picture);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserActivity();
    }

    public void openNotificationActivity(View view) {
        Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
        startActivity(intent);
    }

    public void closeNavigationDrawer(View view) {
        homeDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public void openNavigationDrawer(View View) {
        homeDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void openProfileActivity(View view) {
        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void showLogoutConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_logout_confirmation);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button noButton = dialog.findViewById(R.id.noButton);
        Button yesButton = dialog.findViewById(R.id.yesButton);

        noButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        dialog.show();
    }

    private void performLogout() {
        updateUserActivity();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.apply();

        Intent loginIntent = new Intent(HomeActivity.this, LogInActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        SuccessDialog.showSuccess(HomeActivity.this, "You have successfully logged out", loginIntent);
    }

    private void initializeComponents() {
        menuImageButton = findViewById(R.id.menuImageButton);
        homeDrawerLayout = findViewById(R.id.homeDrawerLayout);
        navigationView = findViewById(R.id.navigationView);
        closeMenuImageButton = findViewById(R.id.closeMenuImageButton);
        profileMiniIconCircleImageView = findViewById(R.id.profileMiniIconCircleImageView);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @org.jetbrains.annotations.NotNull MenuItem item) {
                int id = item.getItemId();
                item.setChecked(true);
                homeDrawerLayout.closeDrawer(GravityCompat.START);

                updateUserActivity();

                if(id == R.id.navHome) {
                    replaceFragment(new HomeFragment());
                } //else if(id == R.id.navUpdates) {
                //replaceFragment(new NewsFragment());
                //}
                else if(id == R.id.navDocumentRequest) {
                    replaceFragment(new DocumentRequestFragment());
                } else if(id == R.id.navDocumentStatus) {
                    replaceFragment(new DocumentStatusFragment());
                } else if(id == R.id.navDeskChat) {
                    replaceFragment(new DeskChatFragment());
                } else if(id == R.id.navHotlineNumbers) {
                    replaceFragment(new EmergencyHotlineFragment());
                } else if(id == R.id.navAboutBarangay) {
                    replaceFragment(new AboutBarangayFragment());
                } else if(id == R.id.navBarangayLeaders) {
                    replaceFragment(new BarangayLeadersFragment());
                } else if(id == R.id.navIncidentReport) {
                    replaceFragment(new IncidentReportFragment());
                }else if(id == R.id.navIncidentStatus) {
                    replaceFragment(new IncidentReportStatusFragment());
                } else if(id == R.id.navLogOut) {
                    showLogoutConfirmationDialog();  // Show confirmation instead of direct logout
                }
                return true;
            }
        });
    }

    public void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (homeDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            homeDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
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
}
