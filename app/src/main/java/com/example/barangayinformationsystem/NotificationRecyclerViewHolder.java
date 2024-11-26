package com.example.barangayinformationsystem;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

public class NotificationRecyclerViewHolder extends RecyclerView.ViewHolder {

    MaterialTextView notification_recycler_view_item_name_of_user_material_text_view;
    MaterialTextView notification_recycler_view_item_caption_of_user_material_text_view;
    ImageView notification_recycler_view_item_image_view;

    public NotificationRecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
        notification_recycler_view_item_name_of_user_material_text_view = itemView.findViewById(R.id.notification_recycler_view_item_name_of_user_material_text_view);
        notification_recycler_view_item_caption_of_user_material_text_view = itemView.findViewById(R.id.notification_recycler_view_item_caption_of_user_material_text_view);
        notification_recycler_view_item_image_view = itemView.findViewById(R.id.notification_recycler_view_item_image_view);
    }
}
