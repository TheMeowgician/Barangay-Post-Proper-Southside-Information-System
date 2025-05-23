package com.example.barangayinformationsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<NotificationRecyclerViewItem> notificationItems;

    public NotificationAdapter(Context context, List<NotificationRecyclerViewItem> notificationItems) {
        this.context = context;
        this.notificationItems = notificationItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationRecyclerViewItem item = notificationItems.get(position);

        holder.titleTextView.setText(item.getNameOfUser());
        holder.messageTextView.setText(item.getCaption());
        holder.iconImageView.setImageResource(item.getImage());

        // Set timestamp - for now we'll use a generated timestamp since NotificationRecyclerViewItem doesn't have a timestamp field
        // You can enhance NotificationRecyclerViewItem to include timestamp if needed
        String timeAgo = generateTimeStamp(position);
        holder.timestampTextView.setText(timeAgo);

        // Add click listener for potential future functionality
        holder.itemView.setOnClickListener(v -> {
            // Handle notification click if needed
        });
    }

    @Override
    public int getItemCount() {
        return notificationItems != null ? notificationItems.size() : 0;
    }

    private String generateTimeStamp(int position) {
        // Generate different timestamps based on position for demo purposes
        // In a real app, you would get this from your notification data
        switch (position % 5) {
            case 0:
                return "2 hours ago";
            case 1:
                return "12 hours ago";
            case 2:
                return "21 hours ago";
            case 3:
                return "2 days ago";
            case 4:
                return "1 week ago";
            default:
                return "Just now";
        }
    }

    // Method to calculate actual time ago if you have a timestamp
    private String getTimeAgo(String createdAt) {
        try {
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            };

            Date notificationDate = null;
            for (SimpleDateFormat format : formats) {
                try {
                    notificationDate = format.parse(createdAt);
                    break;
                } catch (ParseException e) {
                    // Try next format
                }
            }

            if (notificationDate == null) {
                return "Just now";
            }

            Date now = new Date();
            long diff = now.getTime() - notificationDate.getTime();
            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
            } else if (days < 7) {
                return days + " day" + (days == 1 ? "" : "s") + " ago";
            } else if (days < 30) {
                long weeks = days / 7;
                return weeks + " week" + (weeks == 1 ? "" : "s") + " ago";
            } else {
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return displayFormat.format(notificationDate);
            }
        } catch (Exception e) {
            return "Just now";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView titleTextView;
        MaterialTextView messageTextView;
        MaterialTextView timestampTextView;
        ImageView iconImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notification_recycler_view_item_name_of_user_material_text_view);
            messageTextView = itemView.findViewById(R.id.notification_recycler_view_item_caption_of_user_material_text_view);
            timestampTextView = itemView.findViewById(R.id.notification_timestamp);
            iconImageView = itemView.findViewById(R.id.notification_recycler_view_item_image_view);
        }
    }
}