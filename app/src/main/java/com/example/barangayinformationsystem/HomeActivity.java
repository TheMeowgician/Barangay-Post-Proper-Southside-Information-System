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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {    private static final String TAG = "HomeActivity";
    private static final int NOTIFICATION_POLLING_INTERVAL = 30000; // Poll every 30 seconds (reduced from 5)
    private static final int BACKGROUND_POLLING_INTERVAL = 60000; // Poll every 60 seconds when in background
    private static final String PREF_KEY_DELETED_ANNOUNCEMENTS = "deleted_announcements";
    private static final String PREF_KEY_DELETED_DOCUMENT_REQUESTS = "deleted_document_requests";
    private static final String PREF_KEY_LAST_ANNOUNCEMENT_CHECK = "last_announcement_check";
    private static final String PREF_KEY_LAST_DOCUMENT_CHECK = "last_document_check";
    private static final long MIN_CACHE_DURATION = 15000; // 15 seconds minimum between checks

    ImageButton menuImageButton, closeMenuImageButton;
    DrawerLayout homeDrawerLayout;
    NavigationView navigationView;
    ShapeableImageView profileMiniIconCircleImageView;
    MaterialTextView navHeaderFullNameTextView;
    TextView notificationCounterTextView;    // Notification tracking variables
    private final Handler notificationHandler = new Handler();
    private ApiService apiService;
    private DocumentStatusTracker statusTracker;
    private int userId;
    private int unreadNotificationCount = 0;
    private Set<String> knownAnnouncements = new HashSet<>();
    private Set<String> knownDocumentStatuses = new HashSet<>();
    
    // Cache variables to reduce database queries
    private long lastAnnouncementCheckTime = 0;
    private long lastDocumentCheckTime = 0;
    private boolean isAppInForeground = true;
    
    // Sets to track deleted notifications to prevent counting them
    private Set<String> deletedAnnouncementIds = new HashSet<>();
    private Set<String> deletedDocumentRequestIds = new HashSet<>();@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initializeComponents();
        initializeNotificationSystem();
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
            String imageUrl = user.getProfilePicture();
            
            // Ensure URL is complete with BASE_URL if it's a relative path
            if (!imageUrl.startsWith("http")) {
                imageUrl = RetrofitClient.BASE_URL + imageUrl;
            }
            
            // Add cache busting parameter to force fresh image loading
            String finalUrl = imageUrl + "?t=" + System.currentTimeMillis();

            // Load image for navigation header
            Glide.with(this)
                    .load(finalUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .skipMemoryCache(true) // Skip memory cache
                    .centerCrop()
                    .into(navHeaderImageView);

            // Load image for top bar mini icon
            Glide.with(this)
                    .load(finalUrl)
                    .placeholder(R.drawable.default_profile_picture)
                    .error(R.drawable.default_profile_picture)
                    .skipMemoryCache(true) // Skip memory cache
                    .centerCrop()
                    .into(profileMiniIconCircleImageView);
        } else {
            navHeaderImageView.setImageResource(R.drawable.default_profile_picture);
            profileMiniIconCircleImageView.setImageResource(R.drawable.default_profile_picture);
        }
    }    @Override
    protected void onResume() {
        super.onResume();
        isAppInForeground = true;
        
        // Update user activity status
        updateUserActivity();
        // Reload user details to get the latest profile picture
        loadUserDetails();
        
        // Reload deleted notification IDs in case they were updated in NotificationActivity
        loadDeletedNotificationIds();
        
        // Load last check times to respect cache
        loadLastCheckTimes();
        
        // Restart notification polling if user is logged in
        if (userId != -1) {
            startNotificationPolling();
        }
    }public void openNotificationActivity(View view) {
        // Reset notification counter when user opens notifications
        resetNotificationCounter();
        
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
    }    private void initializeComponents() {
        menuImageButton = findViewById(R.id.menuImageButton);
        homeDrawerLayout = findViewById(R.id.homeDrawerLayout);
        navigationView = findViewById(R.id.navigationView);
        closeMenuImageButton = findViewById(R.id.closeMenuImageButton);
        profileMiniIconCircleImageView = findViewById(R.id.profileMiniIconCircleImageView);
        notificationCounterTextView = findViewById(R.id.notificationCounterTextView);

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
                }else if(id == R.id.navVerifyDocument) {
                    Intent verifyIntent = new Intent(HomeActivity.this, DocumentVerificationActivity.class);
                    startActivity(verifyIntent);
                } else if(id == R.id.navLogOut) {
                    showLogoutConfirmationDialog();
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
            // Check which fragment is currently active
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            
            if (currentFragment instanceof HomeFragment) {
                // If HomeFragment is active, show exit confirmation dialog
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
            } else {
                // If any other fragment is active, navigate back to HomeFragment
                replaceFragment(new HomeFragment());
                // Update navigation drawer selection to highlight Home
                navigationView.setCheckedItem(R.id.navHome);
            }
        }
    }
      /**
     * Initialize the notification tracking system
     */
    private void initializeNotificationSystem() {
        // Initialize API service and status tracker
        apiService = RetrofitClient.getApiService();
        statusTracker = DocumentStatusTracker.getInstance();
        statusTracker.loadTrackedStatuses(this);
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("user_id", -1);
        
        if (userId != -1) {
            // Load deleted notification IDs
            loadDeletedNotificationIds();
            
            // Load known notifications from preferences
            loadKnownNotifications();
            
            // Start polling for new notifications
            startNotificationPolling();
        }
    }
      /**
     * Start periodic notification checking with intelligent caching
     */
    private void startNotificationPolling() {
        notificationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Use different intervals based on app state
                int interval = isAppInForeground ? NOTIFICATION_POLLING_INTERVAL : BACKGROUND_POLLING_INTERVAL;
                
                // Only check if enough time has passed since last check
                long currentTime = System.currentTimeMillis();
                
                if (currentTime - lastAnnouncementCheckTime >= MIN_CACHE_DURATION) {
                    checkForNewAnnouncements();
                    lastAnnouncementCheckTime = currentTime;
                }
                
                if (currentTime - lastDocumentCheckTime >= MIN_CACHE_DURATION) {
                    checkForDocumentStatusChanges();
                    lastDocumentCheckTime = currentTime;
                }
                
                notificationHandler.postDelayed(this, interval);
            }
        }, NOTIFICATION_POLLING_INTERVAL);
    }
    
    /**
     * Stop notification polling
     */
    private void stopNotificationPolling() {
        notificationHandler.removeCallbacksAndMessages(null);
    }
      /**
     * Check for new announcements and update counter
     */
    private void checkForNewAnnouncements() {
        if (apiService == null) return;
        
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();
        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AnnouncementResponse> announcements = response.body();
                    int newAnnouncementCount = 0;
                    
                    for (AnnouncementResponse announcement : announcements) {
                        // Check if this announcement was deleted by the user
                        String announcementTitle = announcement.getAnnouncementTitle();
                        if (deletedAnnouncementIds.contains(announcementTitle)) {
                            Log.d(TAG, "HomeActivity: Skipping deleted announcement: " + announcementTitle);
                            continue; // Skip deleted announcements
                        }
                        
                        String announcementKey = "announcement_" + announcement.getId();
                        if (!knownAnnouncements.contains(announcementKey)) {
                            knownAnnouncements.add(announcementKey);
                            newAnnouncementCount++;
                            Log.d(TAG, "HomeActivity: Found new announcement: " + announcementTitle);
                        }
                    }
                    
                    if (newAnnouncementCount > 0) {
                        unreadNotificationCount += newAnnouncementCount;
                        updateNotificationCounter();
                        saveKnownNotifications();
                        Log.d(TAG, "Found " + newAnnouncementCount + " new announcements");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e(TAG, "Error checking announcements: " + t.getMessage());
            }
        });
    }
    
    /**
     * Check for document status changes and update counter
     */
    private void checkForDocumentStatusChanges() {
        if (apiService == null || userId == -1) return;
        
        Call<DocumentRequestListResponse> call = apiService.getUserRequests(userId);
        call.enqueue(new Callback<DocumentRequestListResponse>() {
            @Override
            public void onResponse(Call<DocumentRequestListResponse> call, Response<DocumentRequestListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DocumentRequest> requests = response.body().getRequests();
                    if (requests != null) {
                        int newStatusChangeCount = 0;
                        
                        for (DocumentRequest request : requests) {
                            String statusKey = "doc_" + request.getId() + "_" + request.getStatus();
                            boolean statusChanged = statusTracker.hasStatusChanged(request.getId(), request.getStatus());
                            
                            if (statusChanged && !knownDocumentStatuses.contains(statusKey)) {
                                knownDocumentStatuses.add(statusKey);
                                newStatusChangeCount++;
                                Log.d(TAG, "Document #" + request.getId() + " status changed to: " + request.getStatus());
                            }
                        }
                        
                        if (newStatusChangeCount > 0) {
                            unreadNotificationCount += newStatusChangeCount;
                            updateNotificationCounter();
                            saveKnownNotifications();
                            Log.d(TAG, "Found " + newStatusChangeCount + " document status changes");
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<DocumentRequestListResponse> call, Throwable t) {
                Log.e(TAG, "Error checking document requests: " + t.getMessage());
            }
        });
    }
    
    /**
     * Update the notification counter badge
     */
    private void updateNotificationCounter() {
        runOnUiThread(() -> {
            if (unreadNotificationCount > 0) {
                notificationCounterTextView.setVisibility(View.VISIBLE);
                String countText = unreadNotificationCount > 99 ? "99+" : String.valueOf(unreadNotificationCount);
                notificationCounterTextView.setText(countText);
            } else {
                notificationCounterTextView.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Reset notification counter when user views notifications
     */
    public void resetNotificationCounter() {
        unreadNotificationCount = 0;
        updateNotificationCounter();
        saveNotificationCount();
    }
    
    /**
     * Load known notifications from SharedPreferences
     */
    private void loadKnownNotifications() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Load notification count
        unreadNotificationCount = prefs.getInt("unread_notification_count", 0);
        
        // Load known announcements
        Set<String> savedAnnouncements = prefs.getStringSet("known_announcements", new HashSet<>());
        knownAnnouncements.addAll(savedAnnouncements);
        
        // Load known document statuses
        Set<String> savedDocStatuses = prefs.getStringSet("known_document_statuses", new HashSet<>());
        knownDocumentStatuses.addAll(savedDocStatuses);
        
        // Update counter display
        updateNotificationCounter();
    }
    
    /**
     * Save known notifications to SharedPreferences
     */
    private void saveKnownNotifications() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putStringSet("known_announcements", knownAnnouncements);
        editor.putStringSet("known_document_statuses", knownDocumentStatuses);
        editor.putInt("unread_notification_count", unreadNotificationCount);
        editor.apply();
    }
      /**
     * Save notification count only
     */
    private void saveNotificationCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("unread_notification_count", unreadNotificationCount);
        editor.apply();
    }
      /**
     * Load deleted notification IDs from SharedPreferences
     */
    private void loadDeletedNotificationIds() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            // Load deleted announcement IDs
            String deletedAnnouncementsJson = prefs.getString(PREF_KEY_DELETED_ANNOUNCEMENTS, "[]");
            JSONArray deletedAnnouncementsArray = new JSONArray(deletedAnnouncementsJson);
            for (int i = 0; i < deletedAnnouncementsArray.length(); i++) {
                String deletedId = deletedAnnouncementsArray.getString(i);
                deletedAnnouncementIds.add(deletedId);
                Log.d(TAG, "HomeActivity: Loaded deleted announcement ID: " + deletedId);
            }

            // Load deleted document request IDs
            String deletedDocRequestsJson = prefs.getString(PREF_KEY_DELETED_DOCUMENT_REQUESTS, "[]");
            JSONArray deletedDocRequestsArray = new JSONArray(deletedDocRequestsJson);
            for (int i = 0; i < deletedDocRequestsArray.length(); i++) {
                deletedDocumentRequestIds.add(deletedDocRequestsArray.getString(i));
            }

            Log.d(TAG, "HomeActivity: Loaded " + deletedAnnouncementIds.size() + " deleted announcement IDs and " 
                    + deletedDocumentRequestIds.size() + " deleted document request IDs");
        } catch (Exception e) {
            Log.e(TAG, "HomeActivity: Error loading deleted notification IDs", e);
        }
    }
    
    /**
     * Load last check times from SharedPreferences
     */
    private void loadLastCheckTimes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastAnnouncementCheckTime = prefs.getLong(PREF_KEY_LAST_ANNOUNCEMENT_CHECK, 0);
        lastDocumentCheckTime = prefs.getLong(PREF_KEY_LAST_DOCUMENT_CHECK, 0);
        Log.d(TAG, "Loaded last check times - Announcements: " + lastAnnouncementCheckTime + ", Documents: " + lastDocumentCheckTime);
    }
    
    /**
     * Save last check times to SharedPreferences
     */
    private void saveLastCheckTimes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_KEY_LAST_ANNOUNCEMENT_CHECK, lastAnnouncementCheckTime);
        editor.putLong(PREF_KEY_LAST_DOCUMENT_CHECK, lastDocumentCheckTime);
        editor.apply();
        Log.d(TAG, "Saved last check times");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNotificationPolling();
        if (statusTracker != null) {
            statusTracker.saveTrackedStatuses(this);
        }
        saveKnownNotifications();
    }
      @Override
    protected void onPause() {
        super.onPause();
        isAppInForeground = false;
        
        if (statusTracker != null) {
            statusTracker.saveTrackedStatuses(this);
        }
        saveKnownNotifications();
        saveLastCheckTimes();
    }
}
