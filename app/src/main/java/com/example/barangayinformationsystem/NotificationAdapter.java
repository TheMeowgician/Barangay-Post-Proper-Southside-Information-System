package com.example.barangayinformationsystem;

import android.content.ClipData;
import android.content.Context;
import android.media.RouteListingPreference;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationRecyclerViewHolder> {

    Context context;
    List<NotificationRecyclerViewItem> items;

    public NotificationAdapter(Context context, List<NotificationRecyclerViewItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public NotificationRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationRecyclerViewHolder(LayoutInflater.from(context).inflate(R.layout.notification_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationRecyclerViewHolder holder, int position) {
        holder.notification_recycler_view_item_name_of_user_material_text_view.setText(items.get(position).getNameOfUser());
        holder.notification_recycler_view_item_caption_of_user_material_text_view.setText(items.get(position).getCaption());
        holder.notification_recycler_view_item_image_view.setImageResource(items.get(position).getImage());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
