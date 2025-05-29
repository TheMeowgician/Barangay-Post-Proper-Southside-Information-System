package com.example.barangayinformationsystem;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int NOTIFICATION_POLLING_INTERVAL = 30000; // Poll every 30 seconds (reduced from 5)
    private static final int BACKGROUND_POLLING_INTERVAL = 60000; // Poll every 60 seconds when in background
    private static final String PREF_KEY_DELETED_ANNOUNCEMENTS = "deleted_announcements";
    private static final String PREF_KEY_DELETED_DOCUMENT_REQUESTS = "deleted_document_requests";
    private static final String PREF_KEY_DELETED_DATABASE_NOTIFICATIONS = "deleted_database_notifications";
    private static final String PREF_KEY_FIRST_LOGIN_TIMESTAMP = "first_login_timestamp_";
    private static final String PREF_KEY_LAST_ANNOUNCEMENT_CHECK = "last_announcement_check";
    private static final String PREF_KEY_LAST_DOCUMENT_CHECK = "last_document_check";
    private static final String PREF_KEY_LAST_INCIDENT_CHECK = "last_incident_check";
    private static final long MIN_CACHE_DURATION = 15000; // 15 seconds minimum between checks

    private static final String PREF_KEY_SHOWN_SYSTEM_NOTIFICATION_IDS = "shown_system_notification_ids"; // Same key as in NotificationActivity

    private static final String CHANNEL_ID = "barangay_home_notification_channel"; // Different channel ID for HomeActivity
    private static final int HOME_NOTIFICATION_ID = 2; // Different notification ID for HomeActivity

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
    private Set<String> knownIncidentStatuses = new HashSet<>();
    
    // Cache variables to reduce database queries
    private long lastAnnouncementCheckTime = 0;
    private long lastDocumentCheckTime = 0;
    private long lastIncidentCheckTime = 0;
    private boolean isAppInForeground = true;
    
    // Sets to track deleted notifications to prevent counting them
    private Set<String> deletedAnnouncementIds = new HashSet<>();
    private Set<String> deletedDocumentRequestIds = new HashSet<>();
    private Set<String> deletedDatabaseNotificationIds = new HashSet<>();
    
    // First login timestamp to filter out old notifications
    private long firstLoginTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initializeComponents();
        initializeNotificationSystem();
        loadUserDetails();
        updateUserActivity();

        // Handle navigation from notifications
        if (getIntent().hasExtra("navigate_to")) {
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if ("document_status".equals(navigateTo)) {
                replaceFragment(new DocumentStatusFragment());
                // Update navigation drawer selection if needed
                navigationView.setCheckedItem(R.id.navDocumentStatus);
            } else if ("incident_status".equals(navigateTo)) {
                replaceFragment(new IncidentReportStatusFragment());
                // Update navigation drawer selection if needed
                navigationView.setCheckedItem(R.id.navIncidentReportStatus);
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
    }

    @Override
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
    }

    public void openNotificationActivity(View view) {
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
    }

    private void initializeComponents() {
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
            // Initialize first login timestamp for this user
            initializeFirstLoginTimestamp();
            
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
                
                if (currentTime - lastIncidentCheckTime >= MIN_CACHE_DURATION) {
                    checkForIncidentStatusChanges();
                    lastIncidentCheckTime = currentTime;
                }
                
                // Always check database notifications (they are the primary source)
                checkForDatabaseNotifications();
                
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
                        String announcementTitle = announcement.getAnnouncementTitle();
                        String createdAt = announcement.getCreatedAt();
                        
                        // Check if this announcement should be shown based on first login timestamp
                        if (!shouldShowNotification(createdAt)) {
                            Log.d(TAG, "HomeActivity: Skipping old announcement (before first login): " + announcementTitle);
                            continue; // Skip announcements created before first login
                        }
                        
                        // Check if this announcement was deleted by the user
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
                        // Show system notification for the first new announcement
                        if (!announcements.isEmpty()) {
                            AnnouncementResponse firstAnnouncement = announcements.get(0);
                            String announcementContentId = "announcement_" + firstAnnouncement.getId();
                            showSystemNotification("New Announcement", firstAnnouncement.getAnnouncementTitle(), announcementContentId);
                        }
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
                            String currentStatus = request.getStatus();
                            String trackedStatus = statusTracker.getTrackedStatus(request.getId());
                            
                            // If this is a new request or status has changed
                            if (trackedStatus == null || !trackedStatus.equals(currentStatus)) {
                                // Only count notifications for meaningful status changes
                                if (shouldCreateNotificationForStatus(currentStatus, trackedStatus)) {
                                    String statusKey = "doc_" + request.getId() + "_" + currentStatus;
                                    if (!knownDocumentStatuses.contains(statusKey)) {
                                        knownDocumentStatuses.add(statusKey);
                                        newStatusChangeCount++;
                                        Log.d(TAG, "Document #" + request.getId() + " status changed to: " + currentStatus);
                                    }
                                }
                                
                                // Update tracked status
                                statusTracker.updateStatus(request.getId(), currentStatus);
                            }
                        }
                        
                        if (newStatusChangeCount > 0) {
                            unreadNotificationCount += newStatusChangeCount;
                            updateNotificationCounter();
                            saveKnownNotifications();
                            statusTracker.saveTrackedStatuses(HomeActivity.this);
                            Log.d(TAG, "Found " + newStatusChangeCount + " document status changes");
                            // Show system notification for the first document status change
                            if (!requests.isEmpty()) {
                                DocumentRequest firstRequest = requests.get(0);
                                String docContentId = "doc_" + firstRequest.getId() + "_" + firstRequest.getStatus().toUpperCase();
                                showSystemNotification("Document Status Update", "Request ID #" + firstRequest.getId() + " status: " + firstRequest.getStatus(), docContentId);
                            }
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
     * Helper method to determine if we should create notification for this status change
     */
    private boolean shouldCreateNotificationForStatus(String currentStatus, String previousStatus) {
        // Don't create notifications for initial "pending" status unless there was a previous status
        if (previousStatus == null && "pending".equalsIgnoreCase(currentStatus)) {
            return false;
        }
        
        // Don't create notification if status hasn't actually changed
        if (previousStatus != null && previousStatus.equalsIgnoreCase(currentStatus)) {
            return false;
        }
        
        // Create notifications for meaningful status changes
        return "approved".equalsIgnoreCase(currentStatus) || 
               "rejected".equalsIgnoreCase(currentStatus) || 
               "cancelled".equalsIgnoreCase(currentStatus) ||
               "overdue".equalsIgnoreCase(currentStatus) ||
               // Also notify when moving from pending to other statuses
               ("pending".equalsIgnoreCase(previousStatus) && !"pending".equalsIgnoreCase(currentStatus));
    }
    
    /**
     * Check for incident status changes and update counter
     */
    private void checkForIncidentStatusChanges() {
        if (apiService == null || userId == -1) return;
        
        Call<IncidentReportListResponse> call = apiService.getUserIncidentReports(userId);
        call.enqueue(new Callback<IncidentReportListResponse>() {
            @Override
            public void onResponse(Call<IncidentReportListResponse> call, Response<IncidentReportListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<IncidentReport> reports = response.body().getReports();
                    if (reports != null) {
                        int newStatusChangeCount = 0;
                        
                        for (IncidentReport report : reports) {
                            String currentStatus = report.getStatus();
                            
                            // Only count notifications for meaningful status changes (resolved)
                            if ("resolved".equalsIgnoreCase(currentStatus)) {
                                String statusKey = "incident_" + report.getId() + "_" + currentStatus;
                                if (!knownIncidentStatuses.contains(statusKey)) {
                                    knownIncidentStatuses.add(statusKey);
                                    newStatusChangeCount++;
                                    Log.d(TAG, "Incident #" + report.getId() + " status changed to: " + currentStatus);
                                }
                            }
                        }
                        
                        if (newStatusChangeCount > 0) {
                            unreadNotificationCount += newStatusChangeCount;
                            updateNotificationCounter();
                            saveKnownNotifications();
                            Log.d(TAG, "Found " + newStatusChangeCount + " incident status changes");
                            // Show system notification for the first incident status change
                            if (!reports.isEmpty()) {
                                IncidentReport firstReport = reports.get(0);
                                String incidentContentId = "incident_" + firstReport.getId() + "_" + firstReport.getStatus().toLowerCase();
                                showSystemNotification("Incident Status Update", "Incident '" + firstReport.getTitle() + "' status: " + firstReport.getStatus(), incidentContentId);
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<IncidentReportListResponse> call, Throwable t) {
                Log.e(TAG, "Error checking incident reports: " + t.getMessage());
            }
        });
    }
    
    /**
     * Check for database notifications and update counter
     */
    private void checkForDatabaseNotifications() {
        if (apiService == null || userId == -1) return;
        
        Call<NotificationListResponse> call = apiService.getUserNotifications(userId);
        call.enqueue(new Callback<NotificationListResponse>() {
            @Override
            public void onResponse(Call<NotificationListResponse> call, Response<NotificationListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<NotificationResponse> dbNotifications = response.body().getNotifications();
                    if (dbNotifications != null) {
                        int newDbNotificationCount = 0;
                        
                        for (NotificationResponse dbNotification : dbNotifications) {
                            // Only count unread notifications
                            if (!dbNotification.isRead()) {
                                String createdAt = dbNotification.getCreatedAt();
                                
                                // Check if this notification should be shown based on first login timestamp
                                if (!shouldShowNotification(createdAt)) {
                                    Log.d(TAG, "HomeActivity: Skipping old database notification (before first login): " + dbNotification.getTitle());
                                    continue; // Skip notifications created before first login
                                }
                                
                                // Create a temporary notification item to get unique ID
                                NotificationRecyclerViewItem tempNotification = new NotificationRecyclerViewItem(
                                    dbNotification.getTitle(),
                                    dbNotification.getMessage(),
                                    R.drawable.notification_pps_logo
                                );
                                String notificationId = tempNotification.getUniqueId();
                                
                                // Check if this notification was deleted by the user
                                if (deletedDatabaseNotificationIds.contains(notificationId)) {
                                    Log.d(TAG, "Skipping deleted database notification: " + dbNotification.getTitle());
                                    continue; // Skip deleted database notifications
                                }
                                
                                String notificationKey = "db_notification_" + dbNotification.getId();
                                if (!knownIncidentStatuses.contains(notificationKey)) {
                                    knownIncidentStatuses.add(notificationKey);
                                    newDbNotificationCount++;
                                    Log.d(TAG, "Found new database notification: " + dbNotification.getTitle());
                                }
                            }
                        }
                        
                        if (newDbNotificationCount > 0) {
                            unreadNotificationCount += newDbNotificationCount;
                            updateNotificationCounter();
                            saveKnownNotifications();
                            Log.d(TAG, "Found " + newDbNotificationCount + " new database notifications");
                            // Show system notification for the first new database notification
                            if(!dbNotifications.isEmpty()){                                NotificationResponse firstDbNotif = dbNotifications.get(0);
                                String dbNotifContentId = "db_notif_" + firstDbNotif.getId();
                                showSystemNotification(firstDbNotif.getTitle(), firstDbNotif.getMessage(), dbNotifContentId);
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<NotificationListResponse> call, Throwable t) {
                Log.e(TAG, "Error checking database notifications: " + t.getMessage());
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
                // Just show a red dot - no text needed
                notificationCounterTextView.setText("");
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
        
        // Load known incident statuses
        Set<String> savedIncidentStatuses = prefs.getStringSet("known_incident_statuses", new HashSet<>());
        knownIncidentStatuses.addAll(savedIncidentStatuses);
        
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
        editor.putStringSet("known_incident_statuses", knownIncidentStatuses);
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
     * Initialize first login timestamp for this user
     */
    private void initializeFirstLoginTimestamp() {
        if (userId == -1) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String firstLoginKey = PREF_KEY_FIRST_LOGIN_TIMESTAMP + userId;
        
        // Check if this is the first time this user is logging in on this device
        firstLoginTimestamp = prefs.getLong(firstLoginKey, 0);
        
        if (firstLoginTimestamp == 0) {
            // This is the first login for this user on this device
            firstLoginTimestamp = System.currentTimeMillis();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(firstLoginKey, firstLoginTimestamp);
            editor.apply();
            
            Log.d(TAG, "HomeActivity: First login detected for user " + userId + " at timestamp: " + firstLoginTimestamp);
        } else {
            Log.d(TAG, "HomeActivity: Existing user " + userId + " first logged in at timestamp: " + firstLoginTimestamp);
        }
    }

    /**
     * Check if a notification should be shown based on its creation timestamp
     */
    private boolean shouldShowNotification(String createdAtString) {
        if (firstLoginTimestamp == 0) {
            return true; // If no first login timestamp, show all notifications
        }
        
        try {
            // Parse the created_at timestamp from the API
            java.text.SimpleDateFormat[] formats = {
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()),
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()),
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()),
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            };
            
            java.util.Date notificationDate = null;
            for (java.text.SimpleDateFormat format : formats) {
                try {
                    notificationDate = format.parse(createdAtString);
                    break;
                } catch (java.text.ParseException e) {
                    // Try next format
                }
            }
            
            if (notificationDate != null) {
                long notificationTimestamp = notificationDate.getTime();
                boolean shouldShow = notificationTimestamp >= firstLoginTimestamp;
                Log.d(TAG, "HomeActivity: Notification created at: " + createdAtString + " (" + notificationTimestamp + 
                           "), First login: " + firstLoginTimestamp + ", Should show: " + shouldShow);
                return shouldShow;
            } else {
                Log.w(TAG, "HomeActivity: Could not parse notification timestamp: " + createdAtString + ", showing by default");
                return true; // If we can't parse the timestamp, show the notification
            }
        } catch (Exception e) {
            Log.e(TAG, "HomeActivity: Error checking notification timestamp: " + e.getMessage());
            return true; // If there's an error, show the notification
        }
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

            // Load deleted database notification IDs
            String deletedDatabaseNotificationsJson = prefs.getString(PREF_KEY_DELETED_DATABASE_NOTIFICATIONS, "[]");
            JSONArray deletedDatabaseNotificationsArray = new JSONArray(deletedDatabaseNotificationsJson);
            for (int i = 0; i < deletedDatabaseNotificationsArray.length(); i++) {
                deletedDatabaseNotificationIds.add(deletedDatabaseNotificationsArray.getString(i));
            }

            Log.d(TAG, "HomeActivity: Loaded " + deletedAnnouncementIds.size() + " deleted announcement IDs and " 
                    + deletedDocumentRequestIds.size() + " deleted document request IDs and "
                    + deletedDatabaseNotificationIds.size() + " deleted database notification IDs");
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
        lastIncidentCheckTime = prefs.getLong(PREF_KEY_LAST_INCIDENT_CHECK, 0);
        Log.d(TAG, "Loaded last check times - Announcements: " + lastAnnouncementCheckTime + ", Documents: " + lastDocumentCheckTime + ", Incidents: " + lastIncidentCheckTime);
    }
    
    /**
     * Save last check times to SharedPreferences
     */
    private void saveLastCheckTimes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_KEY_LAST_ANNOUNCEMENT_CHECK, lastAnnouncementCheckTime);
        editor.putLong(PREF_KEY_LAST_DOCUMENT_CHECK, lastDocumentCheckTime);
        editor.putLong(PREF_KEY_LAST_INCIDENT_CHECK, lastIncidentCheckTime);
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

    private void showSystemNotification(String title, String message, String notificationContentId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> shownNotificationIds = prefs.getStringSet(PREF_KEY_SHOWN_SYSTEM_NOTIFICATION_IDS, new HashSet<>());

        if (shownNotificationIds.contains(notificationContentId)) {
            Log.d(TAG, "System notification for " + notificationContentId + " already shown in HomeActivity. Skipping.");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Barangay Home Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_pps_logo) // Replace with your app's notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(HOME_NOTIFICATION_ID, builder.build());

        // Add to set and save
        shownNotificationIds.add(notificationContentId);
        prefs.edit().putStringSet(PREF_KEY_SHOWN_SYSTEM_NOTIFICATION_IDS, shownNotificationIds).apply();
        Log.d(TAG, "Shown system notification for " + notificationContentId + " in HomeActivity and saved to prefs.");
    }
}
