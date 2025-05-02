package com.example.barangayinformationsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private static final String PREF_KEY_SAVED_NOTIFICATIONS = "saved_notifications";
    private static final int MAX_NOTIFICATIONS = 50; // Maximum notifications to keep

    private RecyclerView notificationRecyclerView;
    private MaterialTextView notification_activity_recent_textview;
    private AppCompatImageButton notification_activity_header_back_button;
    private NotificationAdapter notificationAdapter;
    private List<NotificationRecyclerViewItem> notificationItems;
    private ApiService apiService;
    private DocumentStatusTracker statusTracker;
    private int userId;

    private final Handler handler = new Handler();
    private final int POLLING_INTERVAL = 5000; // Poll every 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notification_activity_recent_textview = findViewById(R.id.notification_activity_recent_textview);
        notification_activity_header_back_button = findViewById(R.id.notification_activity_header_back_button);

        notificationRecyclerView = findViewById(R.id.notification_activity_recycler_view);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the list and adapter
        notificationItems = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationItems);
        notificationRecyclerView.setAdapter(notificationAdapter);

        // Initialize the API service
        apiService = RetrofitClient.getApiService();

        // Initialize status tracker
        statusTracker = DocumentStatusTracker.getInstance();
        statusTracker.loadTrackedStatuses(this);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("user_id", -1);

        if (userId != -1) {
            // Load saved notifications first
            loadSavedNotifications();

            // Then fetch announcements
            fetchAnnouncements();

            // Check document requests with notification enabled
            fetchDocumentRequests(true);

            // Start polling for updates
            startPolling();
        } else {
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show();
        }
    }

    public void goBack(View view) {
        finish();
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchNewAnnouncements(); // Fetch new announcements
                fetchDocumentRequests(true); // Fetch document requests and notify on changes
                handler.postDelayed(this, POLLING_INTERVAL); // Re-run after interval
            }
        }, POLLING_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Stop all scheduled tasks
        // Save tracked statuses when activity is destroyed
        statusTracker.saveTrackedStatuses(this);
        // Save notifications
        saveNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save tracked statuses when activity is paused
        statusTracker.saveTrackedStatuses(this);
        // Save notifications
        saveNotifications();
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
                        String updatedTitle = "Post Proper Southside posted a new announcement.\n" + announcement.getAnnouncementTitle();

                        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        );

                        // Avoid duplicates
                        if (!notificationItems.contains(newNotification)) {
                            notificationItems.add(0, newNotification); // Add new to top
                            notificationAdapter.notifyItemInserted(0); // Notify adapter
                            saveNotifications(); // Save after adding
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
                        // Prepend the text to the announcement title
                        String updatedTitle = "Post Proper Southside posted a new announcement.\n" + announcement.getAnnouncementTitle();

                        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        );

                        // Only add if not already in the list
                        if (!notificationItems.contains(newNotification)) {
                            notificationItems.add(newNotification);
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
                notificationMessage = "Your document request (" + request.getDocumentType() + ") has been APPROVED. Please visit the barangay office to pick it up.";
                break;
            case "REJECTED":
                notificationMessage = "Your document request (" + request.getDocumentType() + ") has been REJECTED.";
                if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
                    notificationMessage += " Reason: " + request.getRejectionReason();
                } else {
                    notificationMessage += " Please contact the barangay office for more information.";
                }
                break;
            case "CANCELLED":
                notificationMessage = "Your document request (" + request.getDocumentType() + ") has been CANCELLED.";
                break;
            default:
                notificationMessage = "Your document request (" + request.getDocumentType() + ") status has changed to " + status;
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

        // Show toast for immediate feedback
        Toast.makeText(this, "Document status updated: " + status, Toast.LENGTH_SHORT).show();
    }

    public void addRecentNotification(View view) {
        // Check for document updates first
        fetchDocumentRequests(true);

        // Then proceed with announcements as before
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();
        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AnnouncementResponse> announcements = response.body();
                    boolean added = false;

                    for (AnnouncementResponse announcement : announcements) {
                        // Prepend the text to the announcement title
                        String updatedTitle = "Post Proper Southside posted a new announcement.\n" + announcement.getAnnouncementTitle();

                        NotificationRecyclerViewItem newNotification = new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        );

                        // Avoid duplicates: Check if the new announcement already exists
                        if (!notificationItems.contains(newNotification)) {
                            notificationItems.add(0, newNotification); // Add to the top
                            added = true;
                        }
                    }

                    if (added) {
                        notificationAdapter.notifyDataSetChanged(); // Notify the adapter
                        saveNotifications(); // Save after adding
                        Toast.makeText(NotificationActivity.this, "Recent notifications added!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(NotificationActivity.this, "No new notifications found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(NotificationActivity.this, "No new notifications found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
                Toast.makeText(NotificationActivity.this, "An error occurred while fetching recent notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterDeletedDocumentRequestNotifications(List<DocumentRequest> currentRequests) {
        // Create a set of current request IDs for quick lookup
        Set<Integer> currentRequestIds = new HashSet<>();
        for (DocumentRequest request : currentRequests) {
            currentRequestIds.add(request.getId());
        }

        // Identify notifications to remove (those for document requests that no longer exist)
        List<NotificationRecyclerViewItem> notificationsToRemove = new ArrayList<>();

        for (NotificationRecyclerViewItem notification : notificationItems) {
            // Check if this is a document request notification
            if (notification.getNameOfUser().equals("Document Request Update")) {
                String caption = notification.getCaption();

                // Try to extract the document type from the notification
                int startIndex = caption.indexOf("(") + 1;
                int endIndex = caption.indexOf(")");

                if (startIndex > 0 && endIndex > startIndex) {
                    String documentType = caption.substring(startIndex, endIndex);
                    boolean found = false;

                    // Check if any current request matches this document type
                    for (DocumentRequest request : currentRequests) {
                        if (request.getDocumentType().equals(documentType)) {
                            found = true;
                            break;
                        }
                    }

                    // If no matching request found, mark for removal
                    if (!found) {
                        notificationsToRemove.add(notification);
                    }
                }
            }
        }

        // Remove the identified notifications
        if (!notificationsToRemove.isEmpty()) {
            notificationItems.removeAll(notificationsToRemove);
            notificationAdapter.notifyDataSetChanged();
            saveNotifications(); // Save after removing
        }
    }
}