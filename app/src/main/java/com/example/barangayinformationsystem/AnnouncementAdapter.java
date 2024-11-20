package com.example.barangayinformationsystem;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private List<AnnouncementResponse> announcements;
    private Context context;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd", Locale.getDefault());

    public AnnouncementAdapter(Context context) {
        this.context = context;
        this.announcements = new ArrayList<>();
    }

    public void setAnnouncements(List<AnnouncementResponse> announcements) {
        this.announcements = announcements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementResponse announcement = announcements.get(position);

        holder.textDescription.setText(announcement.getDescriptionText());

        // Format and set the date
        try {
            Date date = inputFormat.parse(announcement.getPostedAt());
            holder.textDate.setText(outputFormat.format(date));
        } catch (ParseException e) {
            holder.textDate.setText(announcement.getPostedAt());
        }

        // Setup images
        setupImages(holder, announcement.getAnnouncementImages());

        // Setup see more functionality
        holder.textDescription.setMaxLines(4);
        holder.seeMoreText.setOnClickListener(v -> {
            if (holder.textDescription.getMaxLines() == 4) {
                holder.textDescription.setMaxLines(Integer.MAX_VALUE);
                holder.seeMoreText.setText("See Less");
            } else {
                holder.textDescription.setMaxLines(4);
                holder.seeMoreText.setText("See More...");
            }
        });
    }

    private void setupImages(ViewHolder holder, List<String> images) {
        holder.leftImagesLayout.removeAllViews();
        holder.rightImagesLayout.removeAllViews();

        if (images == null || images.isEmpty()) {
            holder.imagesContainer.setVisibility(View.GONE);
            return;
        }

        holder.imagesContainer.setVisibility(View.VISIBLE);

        // For left side (2 images)
        for (int i = 0; i < Math.min(2, images.size()); i++) {
            ImageView imageView = createImageView();
            String imageUrl = images.get(i);
            loadImage(imageUrl, imageView);
            setupImageClicks(imageView, imageUrl);
            holder.leftImagesLayout.addView(imageView);
        }

        // For right side (3 images)
        for (int i = 2; i < Math.min(5, images.size()); i++) {
            ImageView imageView = createImageView();
            String imageUrl = images.get(i);
            loadImage(imageUrl, imageView);
            setupImageClicks(imageView, imageUrl);
            holder.rightImagesLayout.addView(imageView);
        }
    }

    private ImageView createImageView() {
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        params.setMargins(0, 0, 0, 1); // 1dp margin bottom
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return imageView;
    }

    private void loadImage(String imageUrl, ImageView imageView) {
        try {
            // For debugging - print the image URL
            Log.d("ImageLoading", "Loading image from URL: " + imageUrl);

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ImageLoading", "Error loading image: " + imageUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d("ImageLoading", "Successfully loaded image: " + imageUrl);
                            return false;
                        }
                    })
                    .centerCrop()
                    .into(imageView);
        } catch (Exception e) {
            Log.e("ImageLoading", "Exception loading image: " + e.getMessage());
            imageView.setImageResource(R.drawable.error_image);
        }
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDescription;
        TextView textDate;
        LinearLayout leftImagesLayout;
        LinearLayout rightImagesLayout;
        LinearLayout imagesContainer;
        TextView seeMoreText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDescription = itemView.findViewById(R.id.expandableTextView);
            textDate = itemView.findViewById(R.id.dateTextView);
            leftImagesLayout = itemView.findViewById(R.id.leftImagesLayout);
            rightImagesLayout = itemView.findViewById(R.id.rightImagesLayout);
            imagesContainer = itemView.findViewById(R.id.imagesContainer);
            seeMoreText = itemView.findViewById(R.id.seeMoreTextView);
        }
    }
    // Add this method to your existing AnnouncementAdapter class
    private void setupImageClicks(ImageView imageView, String imageUrl) {
        imageView.setOnClickListener(v -> showFullScreenImage(imageUrl));
    }

    private void showFullScreenImage(String imageUrl) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_viewer);

        ImageView fullscreenImage = dialog.findViewById(R.id.fullscreenImageView);
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);

        Glide.with(context)
                .load(imageUrl)
                .error(R.drawable.error_image)
                .into(fullscreenImage);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateImageCounter(TextView counter, int current, int total) {
        counter.setText(String.format("%d/%d", current, total));
    }
}