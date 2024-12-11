package com.example.barangayinformationsystem;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.SharedPreferences; // Import SharedPreferences
import android.preference.PreferenceManager;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView notificationRecyclerView;
    private MaterialTextView notification_activity_recent_textview;
    private AppCompatImageButton notification_activity_header_back_button;
    private NotificationAdapter notificationAdapter;
    private List<NotificationRecyclerViewItem> notificationItems;
    private ApiService apiService;
    private HashSet<Integer> displayedRequestIds = new HashSet<>();

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

        displayedRequestIds.clear();
        startPolling();

        // Fetch announcements
        fetchAnnouncements();
        fetchDocumentRequestUpdates();

    }

    public void goBack(View view) {
        finish();
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchNewAnnouncements(); // Fetch new announcements
                fetchDocumentRequestUpdates();
                handler.postDelayed(this, POLLING_INTERVAL); // Re-run after interval
            }
        }, POLLING_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Stop all scheduled tasks
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
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e("NotificationActivity", "Error fetching announcements: " + t.getMessage());
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

                        // Add each announcement to the list
                        notificationItems.add(new NotificationRecyclerViewItem(
                                "Post Proper Southside",
                                updatedTitle,
                                R.drawable.notification_pps_logo
                        ));
                    }
                    notificationAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(NotificationActivity.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e("NotificationActivity", "Error: " + t.getMessage());
                Toast.makeText(NotificationActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addRecentNotification(View view) {
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

                        // Avoid duplicates: Check if the new announcement already exists
                        if (!notificationItems.contains(newNotification)) {
                            notificationItems.add(0, newNotification); // Add to the top
                        }
                    }

                    notificationAdapter.notifyDataSetChanged(); // Notify the adapter
                    Toast.makeText(NotificationActivity.this, "Recent notification added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NotificationActivity.this, "No new notifications found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Log.e("NotificationActivity", "Error: " + t.getMessage());
                Toast.makeText(NotificationActivity.this, "An error occurred while fetching recent notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDocumentRequestUpdates() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("user_id", -1);

        if (userId != -1) {
            Call<List<DocumentRequestUpdate>> call = apiService.getDocumentRequestUpdates(userId);
            call.enqueue(new Callback<List<DocumentRequestUpdate>>() {
                @Override
                public void onResponse(Call<List<DocumentRequestUpdate>> call, Response<List<DocumentRequestUpdate>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<DocumentRequestUpdate> updates = response.body();
                        for (DocumentRequestUpdate update : updates) {
                            // Generate unique hashcode from ID and status
                            int uniqueId = (update.getId() + "_" + update.getStatus()).hashCode();

                            // Check if this uniqueId has already been displayed
                            if (!displayedRequestIds.contains(uniqueId)) {
                                String notificationText = "Your " + update.getDocumentType() + " request submitted on " + update.getDateRequested() + " is " + update.getStatus();
                                notificationItems.add(0, new NotificationRecyclerViewItem(notificationText));

                                // Mark this uniqueId as displayed
                                displayedRequestIds.add(uniqueId);
                            }
                        }
                        notificationAdapter.notifyDataSetChanged();
                    } else {
                        // ... (handle unsuccessful response)
                    }
                }

                @Override
                public void onFailure(Call<List<DocumentRequestUpdate>> call, Throwable t) {
                    // Handle failure
                    Toast.makeText(NotificationActivity.this, "Error fetching document request updates: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Handle case where user ID is not found
            Toast.makeText(NotificationActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

}
