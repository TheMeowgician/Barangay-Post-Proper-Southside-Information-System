package com.example.barangayinformationsystem;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "NotificationActivity";
    private static final String PREF_KEY_SAVED_NOTIFICATIONS = "saved_notifications";
    private static final String PREF_KEY_DELETED_ANNOUNCEMENTS = "deleted_announcements";
    private static final String PREF_KEY_DELETED_DOCUMENT_REQUESTS = "deleted_document_requests";
    private static final String PREF_KEY_DELETED_INCIDENT_REPORTS = "deleted_incident_reports";
    private static final String PREF_KEY_DELETED_DATABASE_NOTIFICATIONS = "deleted_database_notifications";
    private static final String PREF_KEY_FIRST_LOGIN_TIMESTAMP = "first_login_timestamp_";
    private static final int MAX_NOTIFICATIONS = 50; // Maximum notifications to keep
    private static final int POLLING_INTERVAL = 20000; // Reduced from 5000 to 20000 (20 seconds)
    private static final int BACKGROUND_POLLING_INTERVAL = 60000; // Poll every 60 seconds when in background
    private static final long MIN_CACHE_DURATION = 15000; // Minimum 15 seconds between API calls
    
    // Cache-related constants for SharedPreferences
    private static final String PREF_KEY_LAST_ANNOUNCEMENT_CHECK = "last_announcement_check_time";
    private static final String PREF_KEY_LAST_DOCUMENT_CHECK = "last_document_check_time";
    private static final String PREF_KEY_LAST_INCIDENT_CHECK = "last_incident_check_time";

    private static final String CHANNEL_ID = "barangay_notification_channel";
    private static final int NOTIFICATION_ID = 1;

    private static final String PREF_KEY_SHOWN_SYSTEM_NOTIFICATION_IDS = "shown_system_notification_ids";

    private RecyclerView notificationRecyclerView;
    private MaterialTextView notification_activity_recent_textview;
    private AppCompatImageButton notification_activity_header_back_button;
    private NotificationAdapter notificationAdapter;
    private List<NotificationRecyclerViewItem> notificationItems;
    private ApiService apiService;
    private DocumentStatusTracker statusTracker;
    private IncidentStatusTracker incidentStatusTracker;
    private int userId;

    // Sets to track deleted notifications to prevent re-adding them
    private Set<String> deletedAnnouncementIds;
    private Set<String> deletedDocumentRequestIds;
    private Set<String> deletedIncidentReportIds;
    private Set<String> deletedDatabaseNotificationIds;
    private final Handler handler = new Handler();
    
    // Caching variables to reduce API calls
    private long lastAnnouncementCheckTime = 0;
    private long lastDocumentCheckTime = 0;
    private long lastIncidentCheckTime = 0;
    private boolean isAppInForeground = true;
    
    // First login timestamp to filter out old notifications
    private long firstLoginTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        initializeViews();
        setupRecyclerView();

        // Initialize deleted notification tracking
        deletedAnnouncementIds = new HashSet<>();
        deletedDocumentRequestIds = new HashSet<>();
        deletedIncidentReportIds = new HashSet<>();
        deletedDatabaseNotificationIds = new HashSet<>();
        loadDeletedNotificationIds();

        // Initialize the API service
        apiService = RetrofitClient.getApiService();

        // Initialize status trackers
        statusTracker = DocumentStatusTracker.getInstance();
        statusTracker.loadTrackedStatuses(this);
        
        incidentStatusTracker = IncidentStatusTracker.getInstance();
        incidentStatusTracker.loadTrackedStatuses(this);
        // Get user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("user_id", -1);
        
        // Initialize first login timestamp for this user
        initializeFirstLoginTimestamp();
        
        // Load cache timestamps
        loadLastCheckTimes();

        if (userId != -1) {
            // Load saved notifications first
            loadSavedNotifications();

            // Fetch database notifications first (these are the real notifications)
            fetchDatabaseNotifications();

            // Then fetch announcements
            fetchAnnouncements();

            // Check document requests with notification enabled
            fetchDocumentRequests(true);

            // Check incident reports with notification enabled
            fetchIncidentReports(true);

            // Start polling for updates
            startPolling();
        } else {
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        notification_activity_recent_textview = findViewById(R.id.notification_activity_recent_textview);
        notification_activity_header_back_button = findViewById(R.id.notification_activity_header_back_button);
        notificationRecyclerView = findViewById(R.id.notification_activity_recycler_view);
    }

    private void setupRecyclerView() {
        // Initialize the list and adapter
        notificationItems = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationItems);

        // Set up the interaction listener
        notificationAdapter.setOnNotificationInteractionListener(new NotificationAdapter.OnNotificationInteractionListener() {
            @Override
            public void onNotificationClick(NotificationRecyclerViewItem item, int position) {
                handleNotificationClick(item, position);
            }

            @Override
            public void onNotificationDelete(NotificationRecyclerViewItem item, int position) {
                handleNotificationDelete(item, position);
            }
        });

        // Setup LinearLayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        notificationRecyclerView.setLayoutManager(layoutManager);
        notificationRecyclerView.setAdapter(notificationAdapter);

        // Remove any default item decorations to eliminate gaps
        while (notificationRecyclerView.getItemDecorationCount() > 0) {
            notificationRecyclerView.removeItemDecorationAt(0);
        }

        // Disable nested scrolling for better performance
        notificationRecyclerView.setNestedScrollingEnabled(false);

        // Remove any padding from RecyclerView
        notificationRecyclerView.setPadding(0, 0, 0, 0);
        notificationRecyclerView.setClipToPadding(false);
    }

    public void goBack(View view) {
        finish();
    }

    private void handleNotificationClick(NotificationRecyclerViewItem item, int position) {
        // Handle navigation based on notification type
        String title = item.getNameOfUser();
        String message = item.getCaption();
        
        try {
            if (message.toLowerCase().contains("announcement") || message.toLowerCase().contains("posted")) {
                // Navigate back to home to show announcements
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if (message.toLowerCase().contains("request") || 
                      message.toLowerCase().contains("approved") || 
                      message.toLowerCase().contains("denied") ||
                      message.toLowerCase().contains("pending") ||
                      message.toLowerCase().contains("cancelled")) {
                // Navigate to document status fragment
                Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra("navigate_to", "document_status");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if (message.toLowerCase().contains("incident") || 
                      message.toLowerCase().contains("resolved")) {
                // Navigate to incident report status fragment
                Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra("navigate_to", "incident_status");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                // Default behavior - show a toast with the notification content
                Toast.makeText(this, "Notification: " + message, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling notification click", e);
            Toast.makeText(this, "Error opening notification", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleNotificationDelete(NotificationRecyclerViewItem item, int position) {
        try {
            // Track the deleted notification to prevent re-adding
            // Use nameOfUser to properly identify notification source instead of message content
            String nameOfUser = item.getNameOfUser();
            String message = item.getCaption();
            
            if (nameOfUser.equals("Post Proper Southside")) {
                // This is an announcement notification
                String announcementId = extractAnnouncementId(message);
                if (announcementId != null) {
                    deletedAnnouncementIds.add(announcementId);
                    Log.d(TAG, "Added announcement to deleted list: " + announcementId);
                    Log.d(TAG, "Total deleted announcements: " + deletedAnnouncementIds.size());
                }
            } else if (nameOfUser.equals("Document Request Update")) {
                // This is an app-generated document request notification
                String requestId = extractDocumentRequestId(message);
                if (requestId != null) {
                    deletedDocumentRequestIds.add(requestId);
                    Log.d(TAG, "Added document request to deleted list: " + requestId);
                }
            } else if (nameOfUser.equals("Incident Report Update")) {
                // This is an app-generated incident report notification
                String incidentId = extractIncidentReportId(message);
                if (incidentId != null) {
                    deletedIncidentReportIds.add(incidentId);
                    Log.d(TAG, "Added incident report to deleted list: " + incidentId);
                }
            } else {
                // Handle database notifications (anything else) - create a unique identifier
                String databaseNotificationId = item.getUniqueId();
                deletedDatabaseNotificationIds.add(databaseNotificationId);
                Log.d(TAG, "Added database notification to deleted list: " + databaseNotificationId);
                Log.d(TAG, "Database notification nameOfUser: " + nameOfUser + ", message: " + message);
            }

            // Remove the notification from the adapter
            notificationAdapter.removeNotification(position);
            
            // Update the notification counter in HomeActivity
            updateNotificationCounter();
            
            // Save the updated notifications list and deleted IDs
            saveNotifications();
            saveDeletedNotificationIds();
            
            Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Notification deleted at position: " + position);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting notification", e);
            Toast.makeText(this, "Error deleting notification", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNotificationCounter() {
        // Update the notification counter to reflect current number of notifications
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("notification_count", notificationItems.size());
        editor.apply();
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                
                // Check if enough time has passed since last announcement check
                if (currentTime - lastAnnouncementCheckTime >= MIN_CACHE_DURATION) {
                    fetchNewAnnouncements();
                    lastAnnouncementCheckTime = currentTime;
                    Log.d(TAG, "Fetched announcements due to cache expiry");
                } else {
                    Log.d(TAG, "Skipped announcement fetch due to cache (time remaining: " + 
                          (MIN_CACHE_DURATION - (currentTime - lastAnnouncementCheckTime)) + "ms)");
                }
                
                // Check if enough time has passed since last document check
                if (currentTime - lastDocumentCheckTime >= MIN_CACHE_DURATION) {
                    fetchDocumentRequests(true);
                    lastDocumentCheckTime = currentTime;
                    Log.d(TAG, "Fetched document requests due to cache expiry");
                } else {
                    Log.d(TAG, "Skipped document fetch due to cache (time remaining: " + 
                          (MIN_CACHE_DURATION - (currentTime - lastDocumentCheckTime)) + "ms)");
                }
                
                // Check if enough time has passed since last incident check
                if (currentTime - lastIncidentCheckTime >= MIN_CACHE_DURATION) {
                    fetchIncidentReports(true);
                    lastIncidentCheckTime = currentTime;
                    Log.d(TAG, "Fetched incident reports due to cache expiry");
                } else {
                    Log.d(TAG, "Skipped incident fetch due to cache (time remaining: " + 
                          (MIN_CACHE_DURATION - (currentTime - lastIncidentCheckTime)) + "ms)");
                }
                
                // Always fetch database notifications (they are the primary source)
                fetchDatabaseNotifications();
                
                // Use different polling intervals based on app state
                int pollingInterval = isAppInForeground ? POLLING_INTERVAL : BACKGROUND_POLLING_INTERVAL;
                handler.postDelayed(this, pollingInterval);
                
                Log.d(TAG, "Next poll in " + pollingInterval + "ms (foreground: " + isAppInForeground + ")");
            }
        }, POLLING_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Stop all scheduled tasks
        // Save tracked statuses when activity is destroyed
        statusTracker.saveTrackedStatuses(this);
        incidentStatusTracker.saveTrackedStatuses(this);
        // Save notifications and deleted IDs
        saveNotifications();
        saveDeletedNotificationIds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppInForeground = true;
        // Load cache times when resuming
        loadLastCheckTimes();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isAppInForeground = false;
        // Save tracked statuses when activity is paused
        statusTracker.saveTrackedStatuses(this);
        incidentStatusTracker.saveTrackedStatuses(this);
        // Save notifications and deleted IDs
        saveNotifications();
        saveDeletedNotificationIds();
        // Save cache times
        saveLastCheckTimes();
    }

    /**
     * Save current notifications to SharedPreferences
     */
    private void saveNotifications() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();

            JSONArray jsonArray = new JSONArray();

            // Only save the most recent MAX_NOTIFICATIONS notifications
            int count = Math.min(notificationItems.size(), MAX_NOTIFICATIONS);
            for (int i = 0; i < count; i++) {
                NotificationRecyclerViewItem item = notificationItems.get(i);
                jsonArray.put(item.toJson().toString());
            }

            editor.putString(PREF_KEY_SAVED_NOTIFICATIONS, jsonArray.toString());
            editor.apply();

            Log.d(TAG, "Saved " + count + " notifications");
        } catch (Exception e) {
            Log.e(TAG, "Error saving notifications: " + e.getMessage());
        }
    }

    /**
     * Load saved notifications from SharedPreferences
     */
    private void loadSavedNotifications() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String savedNotificationsJson = prefs.getString(PREF_KEY_SAVED_NOTIFICATIONS, "");

            if (savedNotificationsJson.isEmpty()) {
                Log.d(TAG, "No saved notifications found");
                return;
            }

            JSONArray jsonArray = new JSONArray(savedNotificationsJson);

            for (int i = 0; i < jsonArray.length(); i++) {
                String notificationJson = jsonArray.getString(i);
                NotificationRecyclerViewItem item = NotificationRecyclerViewItem.fromJson(notificationJson);

                if (item != null && !notificationItems.contains(item)) {
                    notificationItems.add(item);
                }
            }

            notificationAdapter.notifyDataSetChanged();
            Log.d(TAG, "Loaded " + notificationItems.size() + " saved notifications");
        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications: " + e.getMessage());
        }
    }

    private void fetchNewAnnouncements() {
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();
        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AnnouncementResponse> announcements = response.body();
                    for (AnnouncementResponse announcement : announcements) {
                        String announcementTitle = announcement.getAnnouncementTitle();
                        String createdAt = announcement.getCreatedAt();
                        
                        // Check if this announcement should be shown based on first login timestamp
                        if (!shouldShowNotification(createdAt)) {
                            Log.d(TAG, "Skipping old announcement (before first login): " + announcementTitle);
                            continue; // Skip announcements created before first login
                        }
                        
                        // Check if this announcement was deleted by the user
                        // Use the announcement title as the identifier to match deletion tracking
                        Log.d(TAG, "Checking announcement: " + announcementTitle);
                        Log.d(TAG, "Deleted announcements set: " + deletedAnnouncementIds.toString());
                        
                        if (deletedAnnouncementIds.contains(announcementTitle)) {
                            Log.d(TAG, "Skipping deleted announcement: " + announcementTitle);
                            continue; // Skip deleted announcements
                        }

                        String updatedTitle = "Post Proper Southside posted a new announcement.\n" + announcementTitle;

                        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        );

                        // Avoid duplicates by checking both the list and content
                        boolean isDuplicate = false;
                        for (NotificationRecyclerViewItem existingItem : notificationItems) {
                            if (existingItem.getCaption().equals(updatedTitle)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (!isDuplicate) {
                            notificationItems.add(0, newNotification); // Add new to top
                            notificationAdapter.notifyItemInserted(0); // Notify adapter
                            saveNotifications(); // Save after adding
                            String notificationContentId = "announcement_" + announcement.getId();
                            
                            Intent intent = new Intent(NotificationActivity.this, NotificationActivity.class); // Always go to NotificationActivity
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationActivity.this, notificationContentId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                            
                            showSystemNotification(newNotification.getNameOfUser(), newNotification.getCaption(), notificationContentId, pendingIntent);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching announcements: " + t.getMessage());
            }
        });
    }

    private void fetchAnnouncements() {
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();
        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AnnouncementResponse> announcements = response.body();
                    for (AnnouncementResponse announcement : announcements) {
                        String announcementTitle = announcement.getAnnouncementTitle();
                        String createdAt = announcement.getCreatedAt();
                        
                        // Check if this announcement should be shown based on first login timestamp
                        if (!shouldShowNotification(createdAt)) {
                            Log.d(TAG, "Skipping old announcement (before first login): " + announcementTitle);
                            continue; // Skip announcements created before first login
                        }
                        
                        // Check if this announcement was deleted by the user
                        Log.d(TAG, "Initial fetch - checking announcement: " + announcementTitle);
                        if (deletedAnnouncementIds.contains(announcementTitle)) {
                            Log.d(TAG, "Initial fetch - skipping deleted announcement: " + announcementTitle);
                            continue; // Skip deleted announcements
                        }

                        // Prepend the text to the announcement title
                        String updatedTitle = "Post Proper Southside posted a new announcement.\n" + announcementTitle;

                        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        );

                        // Check for duplicates by content, not just object equality
                        boolean isDuplicate = false;
                        for (NotificationRecyclerViewItem existingItem : notificationItems) {
                            if (existingItem.getCaption().equals(updatedTitle)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        // Only add if not already in the list
                        if (!isDuplicate) {
                            notificationItems.add(newNotification);
                            String notificationContentId = "announcement_" + announcement.getId();

                            Intent intent = new Intent(NotificationActivity.this, NotificationActivity.class); // Always go to NotificationActivity
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationActivity.this, notificationContentId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                            showSystemNotification(newNotification.getNameOfUser(), newNotification.getCaption(), notificationContentId, pendingIntent);
                        }
                    }
                    notificationAdapter.notifyDataSetChanged();
                    saveNotifications(); // Save after adding
                } else {
                    Toast.makeText(NotificationActivity.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
                Toast.makeText(NotificationActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Improved method to fetch document requests and check for status changes
    private void fetchDocumentRequests(final boolean notifyChanges) {
        if (userId == -1) return;

        Call<DocumentRequestListResponse> call = apiService.getUserRequests(userId);
        call.enqueue(new Callback<DocumentRequestListResponse>() {
            @Override
            public void onResponse(Call<DocumentRequestListResponse> call, Response<DocumentRequestListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DocumentRequest> requests = response.body().getRequests();
                    if (requests != null) {
                        Log.d(TAG, "Fetched " + requests.size() + " document requests");

                        // Filter out notifications for deleted requests
                        filterDeletedDocumentRequestNotifications(requests);

                        // Rest of your existing code for checking status changes
                        if (!requests.isEmpty()) {
                            for (DocumentRequest request : requests) {
                                Log.d(TAG, "Document #" + request.getId() + " status: " + request.getStatus());

                                // If this is the first view or if we want to notify about changes
                                if (notifyChanges) {
                                    boolean statusChanged = statusTracker.hasStatusChanged(request.getId(), request.getStatus());
                                    Log.d(TAG, "Status changed for doc #" + request.getId() + ": " + statusChanged);

                                    if (statusChanged) {
                                        addDocumentStatusNotification(request);
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No document requests found");
                        // If there are no requests at all, remove all document request notifications
                        removeAllDocumentRequestNotifications();
                    }
                } else {
                    Log.e(TAG, "Failed to fetch document requests: " + (response.body() != null ? response.body().getMessage() : "Unknown error"));
                }
            }

            private void removeAllDocumentRequestNotifications() {
                List<NotificationRecyclerViewItem> notificationsToRemove = new ArrayList<>();

                for (NotificationRecyclerViewItem notification : notificationItems) {
                    if (notification.getNameOfUser().equals("Document Request Update")) {
                        notificationsToRemove.add(notification);
                    }
                }

                if (!notificationsToRemove.isEmpty()) {
                    notificationItems.removeAll(notificationsToRemove);
                    notificationAdapter.notifyDataSetChanged();
                    saveNotifications();
                }
            }

            @Override
            public void onFailure(Call<DocumentRequestListResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching document requests: " + t.getMessage());
            }
        });
    }

    // Add a notification for document status change
    private void addDocumentStatusNotification(DocumentRequest request) {
        String status = request.getStatus().toUpperCase();
        String notificationMessage = "";
        int iconResource = R.drawable.notification_pps_logo; // Default icon

        switch (status) {
            case "APPROVED":
                notificationMessage = "Your document request ID #" + request.getId() + " (" + request.getDocumentType() + ") has been APPROVED. Please visit the barangay office to pick it up.";
                break;
            case "REJECTED":
                notificationMessage = "Your document request ID #" + request.getId() + " (" + request.getDocumentType() + ") has been REJECTED.";
                if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
                    notificationMessage += " Reason: " + request.getRejectionReason();
                } else {
                    notificationMessage += " Please contact the barangay office for more information.";
                }
                break;
            case "CANCELLED":
                notificationMessage = "Your document request ID #" + request.getId() + " (" + request.getDocumentType() + ") has been CANCELLED.";
                break;
            case "OVERDUE":
                notificationMessage = "Your document request ID #" + request.getId() + " (" + request.getDocumentType() + ") is now OVERDUE. Please contact the barangay office immediately.";
                break;
            default:
                notificationMessage = "Your document request ID #" + request.getId() + " (" + request.getDocumentType() + ") status has changed to " + status;
                break;
        }

        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                "Document Request Update",
                notificationMessage,
                iconResource
        );

        // Log the notification being added
        Log.d(TAG, "Adding notification: " + notificationMessage);

        // Add to the top of the list
        notificationItems.add(0, newNotification);
        notificationAdapter.notifyItemInserted(0);

        // Save notifications after adding new one
        saveNotifications();
        String docNotificationContentId = "doc_" + request.getId() + "_" + request.getStatus().toUpperCase();
        
        Intent docIntent = new Intent(NotificationActivity.this, NotificationActivity.class); // Always go to NotificationActivity
        docIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent docPendingIntent = PendingIntent.getActivity(NotificationActivity.this, docNotificationContentId.hashCode(), docIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        showSystemNotification(newNotification.getNameOfUser(), newNotification.getCaption(), docNotificationContentId, docPendingIntent);
        
        // Show toast for immediate feedback
        Toast.makeText(this, "Document status updated: " + status, Toast.LENGTH_SHORT).show();
    }
    
    public void addRecentNotification(View view) {
        // Use a counter to track completed async calls
        final int[] completedCalls = {0};
        final boolean[] foundNewNotifications = {false};
        
        // Check for document updates first
        fetchDocumentRequestsForRecent(new DocumentUpdateCallback() {
            @Override
            public void onDocumentUpdateComplete(boolean foundUpdates) {
                foundNewNotifications[0] = foundNewNotifications[0] || foundUpdates;
                completedCalls[0]++;
                checkAllCallsComplete(completedCalls, foundNewNotifications);
            }
        });

        // Then proceed with announcements
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();
        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {
                boolean addedAnnouncements = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    List<AnnouncementResponse> announcements = response.body();

                    for (AnnouncementResponse announcement : announcements) {
                        String announcementTitle = announcement.getAnnouncementTitle();
                        String createdAt = announcement.getCreatedAt();
                        
                        // Check if this announcement should be shown based on first login timestamp
                        if (!shouldShowNotification(createdAt)) {
                            Log.d(TAG, "Recent fetch - skipping old announcement (before first login): " + announcementTitle);
                            continue; // Skip announcements created before first login
                        }
                        
                        // Check if this announcement was deleted by the user
                        Log.d(TAG, "Recent fetch - checking announcement: " + announcementTitle);
                        if (deletedAnnouncementIds.contains(announcementTitle)) {
                            Log.d(TAG, "Recent fetch - skipping deleted announcement: " + announcementTitle);
                            continue; // Skip deleted announcements
                        }

                        // Prepend the text to the announcement title
                        String updatedTitle = "Post Proper Southside posted a new announcement.\n" + announcementTitle;

                        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        );

                        // Check for duplicates by content, not just object equality
                        boolean isDuplicate = false;
                        for (NotificationRecyclerViewItem existingItem : notificationItems) {
                            if (existingItem.getCaption().equals(updatedTitle)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        // Avoid duplicates: Check if the new announcement already exists
                        if (!isDuplicate) {
                            notificationItems.add(0, newNotification); // Add to the top
                            addedAnnouncements = true;
                            String announcementContentId = "announcement_" + announcement.getId();

                            Intent intent = new Intent(NotificationActivity.this, NotificationActivity.class); // Always go to NotificationActivity
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationActivity.this, announcementContentId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                            showSystemNotification(newNotification.getNameOfUser(), newNotification.getCaption(), announcementContentId, pendingIntent);
                        }
                    }

                    if (addedAnnouncements) {
                        notificationAdapter.notifyDataSetChanged(); // Notify the adapter
                        saveNotifications(); // Save after adding
                    }
                }
                
                foundNewNotifications[0] = foundNewNotifications[0] || addedAnnouncements;
                completedCalls[0]++;
                checkAllCallsComplete(completedCalls, foundNewNotifications);
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
                completedCalls[0]++;
                checkAllCallsComplete(completedCalls, foundNewNotifications);
            }
        });
    }

    private void checkAllCallsComplete(int[] completedCalls, boolean[] foundNewNotifications) {
        if (completedCalls[0] >= 2) { // Both document and announcement calls completed
            if (foundNewNotifications[0]) {
                Toast.makeText(this, "Recent notifications added!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No new notifications found", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Interface for callback when document update check is complete
    private interface DocumentUpdateCallback {
        void onDocumentUpdateComplete(boolean foundUpdates);
    }
    
    // Enhanced method to fetch document requests specifically for "recent" button
    private void fetchDocumentRequestsForRecent(final DocumentUpdateCallback callback) {
        if (userId == -1) {
            callback.onDocumentUpdateComplete(false);
            return;
        }

        Call<DocumentRequestListResponse> call = apiService.getUserRequests(userId);
        call.enqueue(new Callback<DocumentRequestListResponse>() {
            @Override
            public void onResponse(Call<DocumentRequestListResponse> call, Response<DocumentRequestListResponse> response) {
                boolean foundUpdates = false;
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DocumentRequest> requests = response.body().getRequests();
                    if (requests != null) {
                        Log.d(TAG, "Fetched " + requests.size() + " document requests for recent check");

                        // Check each request for status changes
                        for (DocumentRequest request : requests) {
                            String currentStatus = request.getStatus();
                            String trackedStatus = statusTracker.getTrackedStatus(request.getId());
                            
                            Log.d(TAG, "Request ID: " + request.getId() + 
                                  ", Current: " + currentStatus + 
                                  ", Tracked: " + trackedStatus);
                            
                            // If this is a new request or status has changed
                            if (trackedStatus == null || !trackedStatus.equals(currentStatus)) {
                                // Only show notifications for meaningful status changes
                                if (shouldCreateNotificationForStatus(currentStatus, trackedStatus)) {
                                    Log.d(TAG, "Status change detected for request " + request.getId() + 
                                          ": " + trackedStatus + " -> " + currentStatus);
                                    
                                    // Check if notification for this status already exists
                                    if (!hasExistingNotificationForRequest(request)) {
                                        addDocumentStatusNotification(request);
                                        foundUpdates = true;
                                    }
                                }
                                
                                // Update tracked status
                                statusTracker.updateStatus(request.getId(), currentStatus);
                            }
                        }
                        
                        // Filter out notifications for deleted requests
                        filterDeletedDocumentRequestNotifications(requests);
                        
                        // Save tracked statuses
                        statusTracker.saveTrackedStatuses(NotificationActivity.this);
                    }
                }
                
                callback.onDocumentUpdateComplete(foundUpdates);
            }

            @Override
            public void onFailure(Call<DocumentRequestListResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching document requests for recent: " + t.getMessage());
                callback.onDocumentUpdateComplete(false);
            }
        });
    }
      // Helper method to check if notification already exists for this request
    private boolean hasExistingNotificationForRequest(DocumentRequest request) {
        String documentType = request.getDocumentType();
        String status = request.getStatus().toUpperCase();
        String documentId = "ID #" + request.getId();
        
        for (NotificationRecyclerViewItem notification : notificationItems) {
            if (notification.getNameOfUser().equals("Document Request Update")) {
                String caption = notification.getCaption();
                // Check for document type, status, AND document ID to ensure uniqueness
                if (caption.contains(documentType) && caption.contains(status) && caption.contains(documentId)) {
                    return true;
                }
            }
        }
        return false;
    }
      // Helper method to determine if we should create notification for this status change
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
     * Extract announcement ID from notification message
     * This method now extracts the announcement title and creates a consistent identifier
     */
    private String extractAnnouncementId(String message) {
        try {
            // Extract the announcement title from the message
            if (message.contains("announcement.\n")) {
                String[] parts = message.split("announcement.\n");
                if (parts.length > 1) {
                    String title = parts[1].trim();
                    // Create a consistent identifier based on the title
                    // This will match the title used when creating notifications
                    return title;
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting announcement ID", e);
            return null;
        }
    }

    /**
     * Extract document request ID from notification message
     */
    private String extractDocumentRequestId(String message) {
        try {
            // Extract document type and use it as identifier
            // This can be enhanced to use actual request IDs
            return message.hashCode() + ""; // Use message hash as ID for now
        } catch (Exception e) {
            Log.e(TAG, "Error extracting document request ID", e);
            return null;
        }
    }

    /**
     * Save deleted notification IDs to SharedPreferences
     */
    private void saveDeletedNotificationIds() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();

            // Save deleted announcement IDs
            JSONArray deletedAnnouncementsArray = new JSONArray();
            for (String id : deletedAnnouncementIds) {
                deletedAnnouncementsArray.put(id);
            }
            editor.putString(PREF_KEY_DELETED_ANNOUNCEMENTS, deletedAnnouncementsArray.toString());

            // Save deleted document request IDs
            JSONArray deletedDocRequestsArray = new JSONArray();
            for (String id : deletedDocumentRequestIds) {
                deletedDocRequestsArray.put(id);
            }
            editor.putString(PREF_KEY_DELETED_DOCUMENT_REQUESTS, deletedDocRequestsArray.toString());

            // Save deleted incident report IDs
            JSONArray deletedIncidentReportsArray = new JSONArray();
            for (String id : deletedIncidentReportIds) {
                deletedIncidentReportsArray.put(id);
            }
            editor.putString(PREF_KEY_DELETED_INCIDENT_REPORTS, deletedIncidentReportsArray.toString());

            // Save deleted database notification IDs
            JSONArray deletedDatabaseNotificationsArray = new JSONArray();
            for (String id : deletedDatabaseNotificationIds) {
                deletedDatabaseNotificationsArray.put(id);
            }
            editor.putString(PREF_KEY_DELETED_DATABASE_NOTIFICATIONS, deletedDatabaseNotificationsArray.toString());
            
            editor.apply();
            Log.d(TAG, "Saved " + deletedAnnouncementIds.size() + " deleted announcement IDs: " + deletedAnnouncementIds.toString());
            Log.d(TAG, "Saved " + deletedDocumentRequestIds.size() + " deleted document request IDs");
            Log.d(TAG, "Saved " + deletedIncidentReportIds.size() + " deleted incident report IDs");
            Log.d(TAG, "Saved " + deletedDatabaseNotificationIds.size() + " deleted database notification IDs");
        } catch (Exception e) {
            Log.e(TAG, "Error saving deleted notification IDs", e);
        }
    }    /**
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
                Log.d(TAG, "Loaded deleted announcement ID: " + deletedId);
            }

            // Load deleted document request IDs
            String deletedDocRequestsJson = prefs.getString(PREF_KEY_DELETED_DOCUMENT_REQUESTS, "[]");
            JSONArray deletedDocRequestsArray = new JSONArray(deletedDocRequestsJson);
            for (int i = 0; i < deletedDocRequestsArray.length(); i++) {
                deletedDocumentRequestIds.add(deletedDocRequestsArray.getString(i));
            }

            // Load deleted incident report IDs
            String deletedIncidentReportsJson = prefs.getString(PREF_KEY_DELETED_INCIDENT_REPORTS, "[]");
            JSONArray deletedIncidentReportsArray = new JSONArray(deletedIncidentReportsJson);
            for (int i = 0; i < deletedIncidentReportsArray.length(); i++) {
                deletedIncidentReportIds.add(deletedIncidentReportsArray.getString(i));
            }

            // Load deleted database notification IDs
            String deletedDatabaseNotificationsJson = prefs.getString(PREF_KEY_DELETED_DATABASE_NOTIFICATIONS, "[]");
            JSONArray deletedDatabaseNotificationsArray = new JSONArray(deletedDatabaseNotificationsJson);
            for (int i = 0; i < deletedDatabaseNotificationsArray.length(); i++) {
                deletedDatabaseNotificationIds.add(deletedDatabaseNotificationsArray.getString(i));
            }

            Log.d(TAG, "Loaded " + deletedAnnouncementIds.size() + " deleted announcement IDs and " 
                    + deletedDocumentRequestIds.size() + " deleted document request IDs and "
                    + deletedIncidentReportIds.size() + " deleted incident report IDs and "
                    + deletedDatabaseNotificationIds.size() + " deleted database notification IDs");
            Log.d(TAG, "Deleted announcement IDs: " + deletedAnnouncementIds.toString());        } catch (Exception e) {
            Log.e(TAG, "Error loading deleted notification IDs", e);
        }
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
            
            Log.d(TAG, "First login detected for user " + userId + " at timestamp: " + firstLoginTimestamp);
        } else {
            Log.d(TAG, "Existing user " + userId + " first logged in at timestamp: " + firstLoginTimestamp);
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
            // Common formats: "2024-01-15 10:30:00", "2024-01-15T10:30:00Z", etc.
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
                Log.d(TAG, "Notification created at: " + createdAtString + " (" + notificationTimestamp + 
                           "), First login: " + firstLoginTimestamp + ", Should show: " + shouldShow);
                return shouldShow;
            } else {
                Log.w(TAG, "Could not parse notification timestamp: " + createdAtString + ", showing by default");
                return true; // If we can't parse the timestamp, show the notification
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking notification timestamp: " + e.getMessage());
            return true; // If there's an error, show the notification
        }
    }

    /**
     * Load cache timestamps from SharedPreferences
     */
    private void loadLastCheckTimes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastAnnouncementCheckTime = prefs.getLong(PREF_KEY_LAST_ANNOUNCEMENT_CHECK, 0);
        lastDocumentCheckTime = prefs.getLong(PREF_KEY_LAST_DOCUMENT_CHECK, 0);
        lastIncidentCheckTime = prefs.getLong(PREF_KEY_LAST_INCIDENT_CHECK, 0);
        Log.d(TAG, "Loaded cache times - Announcements: " + lastAnnouncementCheckTime + 
                   ", Documents: " + lastDocumentCheckTime + ", Incidents: " + lastIncidentCheckTime);
    }

    /**
     * Save cache timestamps to SharedPreferences
     */
    private void saveLastCheckTimes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_KEY_LAST_ANNOUNCEMENT_CHECK, lastAnnouncementCheckTime);
        editor.putLong(PREF_KEY_LAST_DOCUMENT_CHECK, lastDocumentCheckTime);
        editor.putLong(PREF_KEY_LAST_INCIDENT_CHECK, lastIncidentCheckTime);
        editor.apply();
        Log.d(TAG, "Saved cache times - Announcements: " + lastAnnouncementCheckTime + 
                   ", Documents: " + lastDocumentCheckTime + ", Incidents: " + lastIncidentCheckTime);
    }

    /**
     * Filter out notifications for document requests that have been deleted by user
     */
    private void filterDeletedDocumentRequestNotifications(List<DocumentRequest> currentRequests) {
        try {
            // Create a set of current request IDs for quick lookup
            Set<Integer> currentRequestIds = new HashSet<>();
            for (DocumentRequest request : currentRequests) {
                currentRequestIds.add(request.getId());
            }
            
            // Remove notifications for document requests that no longer exist
            Iterator<NotificationRecyclerViewItem> iterator = notificationItems.iterator();
            boolean removedAny = false;
            
            while (iterator.hasNext()) {
                NotificationRecyclerViewItem notification = iterator.next();
                
                // Check if this is a document request notification
                if (notification.getNameOfUser().equals("Document Request Update")) {
                    // Extract request info from the notification
                    String caption = notification.getCaption();
                      // Try to find if this notification corresponds to a current request
                    boolean foundMatchingRequest = false;
                    for (DocumentRequest request : currentRequests) {
                        String documentType = request.getDocumentType();
                        String status = request.getStatus().toUpperCase();
                        String documentId = "ID #" + request.getId();
                        
                        // Check if the notification matches this request (type, status, AND ID)
                        if (caption.contains(documentType) && caption.contains(status) && caption.contains(documentId)) {
                            foundMatchingRequest = true;
                            break;
                        }
                    }
                    
                    // If no matching request found, this notification should be removed
                    if (!foundMatchingRequest) {
                        Log.d(TAG, "Removing orphaned document notification: " + caption);
                        iterator.remove();
                        removedAny = true;
                    }
                }
            }
            
            // Update the adapter if we removed any notifications
            if (removedAny) {
                notificationAdapter.notifyDataSetChanged();
                saveNotifications();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error filtering deleted document request notifications", e);
        }
    }

    /**
     * Fetch incident reports and check for status changes
     */
    private void fetchIncidentReports(final boolean notifyChanges) {
        if (userId == -1) return;

        Call<IncidentReportListResponse> call = apiService.getUserIncidentReports(userId);
        call.enqueue(new Callback<IncidentReportListResponse>() {
            @Override
            public void onResponse(Call<IncidentReportListResponse> call, Response<IncidentReportListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<IncidentReport> reports = response.body().getReports();
                    if (reports != null) {
                        Log.d(TAG, "Fetched " + reports.size() + " incident reports");

                        // Check for status changes
                        if (!reports.isEmpty()) {
                            for (IncidentReport report : reports) {
                                Log.d(TAG, "Incident #" + report.getId() + " status: " + report.getStatus());

                                // If this is the first view or if we want to notify about changes
                                if (notifyChanges) {
                                    boolean statusChanged = incidentStatusTracker.hasIncidentStatusChanged(report.getId(), report.getStatus());
                                    Log.d(TAG, "Status changed for incident #" + report.getId() + ": " + statusChanged);

                                    if (statusChanged) {
                                        addIncidentStatusNotification(report);
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No incident reports found");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch incident reports: " + (response.body() != null ? response.body().getMessage() : "Unknown error"));
                }
            }

            @Override
            public void onFailure(Call<IncidentReportListResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching incident reports: " + t.getMessage());
            }
        });
    }

    /**
     * Add a notification for incident status change
     */
    private void addIncidentStatusNotification(IncidentReport report) {
        String status = report.getStatus();
        String title = report.getTitle();
        
        // Check if notification should be created
        if (!shouldCreateNotificationForIncidentStatus(status, incidentStatusTracker.getTrackedIncidentStatus(report.getId()))) {
            return;
        }
        
        // Check for existing notification
        if (hasExistingNotificationForIncident(report)) {
            return;
        }

        String notificationMessage = String.format(
            "Your incident report '%s' (ID #%d) has been %s.",
            title,
            report.getId(),
            status.toLowerCase()
        );

        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
            "Incident Report Update",
            notificationMessage,
            R.drawable.notification_pps_logo
        );

        Log.d(TAG, "Adding incident notification: " + notificationMessage);

        // Add to the top of the list
        notificationItems.add(0, newNotification);
        notificationAdapter.notifyItemInserted(0);

        // Save notifications after adding new one
        saveNotifications();
        String incidentContentId = "incident_" + report.getId() + "_" + report.getStatus().toLowerCase();
        
        Intent incidentIntent = new Intent(NotificationActivity.this, NotificationActivity.class); // Always go to NotificationActivity
        incidentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent incidentPendingIntent = PendingIntent.getActivity(NotificationActivity.this, incidentContentId.hashCode(), incidentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        showSystemNotification(newNotification.getNameOfUser(), newNotification.getCaption(), incidentContentId, incidentPendingIntent);
        
        // Update status tracker
        incidentStatusTracker.updateIncidentStatus(report.getId(), status);
        
        // Show toast for immediate feedback
        Toast.makeText(this, "Incident report status updated: " + status, Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper method to check if notification already exists for this incident
     */
    private boolean hasExistingNotificationForIncident(IncidentReport report) {
        String title = report.getTitle();
        String status = report.getStatus().toUpperCase();
        String incidentId = "ID #" + report.getId();
        
        for (NotificationRecyclerViewItem notification : notificationItems) {
            if (notification.getNameOfUser().equals("Incident Report Update")) {
                String caption = notification.getCaption();
                // Check for incident title, status, AND incident ID to ensure uniqueness
                if (caption.contains(title) && caption.contains(status) && caption.contains(incidentId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to determine if we should create notification for this incident status change
     */
    private boolean shouldCreateNotificationForIncidentStatus(String currentStatus, String previousStatus) {
        // Don't create notifications for initial "pending" status unless there was a previous status
        if (previousStatus == null && "pending".equalsIgnoreCase(currentStatus)) {
            return false;
        }
        
        // Don't create notification if status hasn't actually changed
        if (previousStatus != null && previousStatus.equalsIgnoreCase(currentStatus)) {
            return false;
        }
        
        // Create notifications for meaningful status changes
        return "resolved".equalsIgnoreCase(currentStatus) ||
               // Also notify when moving from pending to other statuses
               ("pending".equalsIgnoreCase(previousStatus) && !"pending".equalsIgnoreCase(currentStatus));
    }

    /**
     * Extract incident report ID from notification message
     */
    private String extractIncidentReportId(String message) {
        try {
            // Extract incident title and use it as identifier
            // This can be enhanced to use actual incident IDs
            return message.hashCode() + ""; // Use message hash as ID for now
        } catch (Exception e) {
            Log.e(TAG, "Error extracting incident report ID", e);
            return null;
        }
    }

    /**
     * Fetch notifications from the database (the primary source of notifications)
     */
    private void fetchDatabaseNotifications() {
        if (userId == -1) return;

        Call<NotificationListResponse> call = apiService.getUserNotifications(userId);
        call.enqueue(new Callback<NotificationListResponse>() {
            @Override
            public void onResponse(Call<NotificationListResponse> call, Response<NotificationListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<NotificationResponse> dbNotifications = response.body().getNotifications();
                    if (dbNotifications != null && !dbNotifications.isEmpty()) {
                        Log.d(TAG, "Fetched " + dbNotifications.size() + " database notifications");

                        for (NotificationResponse dbNotification : dbNotifications) {
                            // Convert database notification to NotificationRecyclerViewItem
                            String title = dbNotification.getTitle();
                            String message = dbNotification.getMessage();
                            String createdAt = dbNotification.getCreatedAt();
                            
                            // Check if this notification should be shown based on first login timestamp
                            if (!shouldShowNotification(createdAt)) {
                                Log.d(TAG, "Skipping old database notification (before first login): " + title + " - " + message);
                                continue; // Skip notifications created before first login
                            }
                            
                            // Create a temporary notification item to get unique ID
                            NotificationRecyclerViewItem tempNotification = new NotificationRecyclerViewItem(
                                title,
                                message,
                                R.drawable.notification_pps_logo
                            );
                            String notificationId = tempNotification.getUniqueId();
                            
                            // Check if this notification was deleted by the user
                            if (deletedDatabaseNotificationIds.contains(notificationId)) {
                                Log.d(TAG, "Skipping deleted database notification: " + title + " - " + message);
                                continue; // Skip deleted database notifications
                            }
                            
                            // Check if this notification already exists in our list
                            boolean alreadyExists = false;
                            for (NotificationRecyclerViewItem existingItem : notificationItems) {
                                // Check if this is the same database notification
                                if (existingItem.getCaption().equals(message) && 
                                    existingItem.getNameOfUser().equals(title)) {
                                    alreadyExists = true;
                                    break;
                                }
                            }
                            
                            if (!alreadyExists) {
                                NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                    title,
                                    message,
                                    R.drawable.notification_pps_logo
                                );
                                
                                // Add to the top of the list (most recent first)
                                notificationItems.add(0, newNotification);
                                Log.d(TAG, "Added database notification: " + title + " - " + message);
                                String dbNotificationContentId = "db_notif_" + dbNotification.getId(); // Assuming NotificationResponse has getId()
                                
                                Intent dbIntent = new Intent(NotificationActivity.this, NotificationActivity.class); // Always go to NotificationActivity
                                dbIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent dbPendingIntent = PendingIntent.getActivity(NotificationActivity.this, dbNotificationContentId.hashCode(), dbIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                
                                showSystemNotification(newNotification.getNameOfUser(), newNotification.getCaption(), dbNotificationContentId, dbPendingIntent);
                            }
                        }
                        
                        // Update the adapter and save notifications
                        notificationAdapter.notifyDataSetChanged();
                        saveNotifications();
                    } else {
                        Log.d(TAG, "No database notifications found");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch database notifications: " + 
                          (response.body() != null ? response.body().getMessage() : "Unknown error"));
                }
            }

            @Override
            public void onFailure(Call<NotificationListResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching database notifications: " + t.getMessage());
            }
        });
    }

    private void showSystemNotification(String title, String message, String notificationContentId, PendingIntent pendingIntent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> shownNotificationIds = prefs.getStringSet(PREF_KEY_SHOWN_SYSTEM_NOTIFICATION_IDS, new HashSet<>());

        if (shownNotificationIds.contains(notificationContentId)) {
            Log.d(TAG, "System notification for " + notificationContentId + " already shown. Skipping.");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Barangay Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_pps_logo) // Replace with your app's notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // Set the PendingIntent
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());

        // Add to set and save
        shownNotificationIds.add(notificationContentId);
        prefs.edit().putStringSet(PREF_KEY_SHOWN_SYSTEM_NOTIFICATION_IDS, shownNotificationIds).apply();
        Log.d(TAG, "Shown system notification for " + notificationContentId + " and saved to prefs.");
    }
}