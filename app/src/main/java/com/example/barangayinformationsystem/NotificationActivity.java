package com.example.barangayinformationsystem;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    AppCompatImageButton notification_activity_header_back_button;
    RecyclerView notification_activity_recycler_view;
    List<NotificationRecyclerViewItem> notificationRecyclerViewItems;

    String nameOfUser = "Post Proper Southside";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initializeComponents();
    }

    private void initializeComponents() {
        notification_activity_header_back_button = findViewById(R.id.notification_activity_header_back_button);
        notification_activity_recycler_view = findViewById(R.id.notification_activity_recycler_view);

        addItemsToRecyclerView();

        notification_activity_recycler_view.setLayoutManager(new LinearLayoutManager(this));
        notification_activity_recycler_view.setAdapter(new NotificationAdapter(getApplicationContext(), notificationRecyclerViewItems));

    }

    private void addItemsToRecyclerView() {

        notificationRecyclerViewItems = new ArrayList<NotificationRecyclerViewItem>();
        notificationRecyclerViewItems.add(new NotificationRecyclerViewItem(nameOfUser, "Test Announcements", R.drawable.notification_pps_logo));
    }

    public void back(View view) {
        finish();
    }

}