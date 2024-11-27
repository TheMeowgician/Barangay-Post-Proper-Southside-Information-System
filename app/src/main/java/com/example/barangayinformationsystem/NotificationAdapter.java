package com.example.barangayinformationsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.List;

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
        holder.notification_recycler_view_item_caption_of_user_material_text_view.setText(item.getCaption());
        holder.notification_recycler_view_item_name_of_user_material_text_view.setText(item.getNameOfUser());
        holder.notification_recycler_view_item_image_view.setImageResource(item.getImage());
    }

    @Override
    public int getItemCount() {
        return notificationItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView notification_recycler_view_item_caption_of_user_material_text_view, notification_recycler_view_item_name_of_user_material_text_view;
        ImageView notification_recycler_view_item_image_view;

        public ViewHolder(View itemView) {
            super(itemView);
            notification_recycler_view_item_caption_of_user_material_text_view = itemView.findViewById(R.id.notification_recycler_view_item_caption_of_user_material_text_view);
            notification_recycler_view_item_name_of_user_material_text_view = itemView.findViewById(R.id.notification_recycler_view_item_name_of_user_material_text_view);
            notification_recycler_view_item_image_view = itemView.findViewById(R.id.notification_recycler_view_item_image_view);
        }
    }
}
