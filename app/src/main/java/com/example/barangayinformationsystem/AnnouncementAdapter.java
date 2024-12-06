package com.example.barangayinformationsystem;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private List<AnnouncementResponse> announcements;
    private final WeakReference<Context> contextRef;
    private final SimpleDateFormat inputFormat;
    private final SimpleDateFormat outputFormat;
    private final AnnouncementClickListener listener;
    private final Handler timeUpdateHandler;
    private final Runnable timeUpdateRunnable;
    private boolean isUpdatingTime = false;

    public interface AnnouncementClickListener {
        void onShareClick(AnnouncementResponse announcement);
        void onImageClick(String imageUrl, List<String> allImages, int position);
        default void onAnnouncementClick(AnnouncementResponse announcement) {}
    }

    public AnnouncementAdapter(Context context, @Nullable AnnouncementClickListener listener) {
        this.contextRef = new WeakReference<>(context);
        this.listener = listener;
        this.announcements = new ArrayList<>();

        // Initialize date formatters with explicit locale
        Locale locale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        }
        this.inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
        this.outputFormat = new SimpleDateFormat("MMMM dd, yyyy • h:mm a", locale);

        // Setup periodic time updates
        timeUpdateHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                notifyItemRangeChanged(0, getItemCount(), "timeUpdate");
                timeUpdateHandler.postDelayed(this, TimeUnit.MINUTES.toMillis(1));
            }
        };
    }

    public void startTimeUpdates() {
        if (!isUpdatingTime) {
            isUpdatingTime = true;
            timeUpdateHandler.post(timeUpdateRunnable);
        }
    }

    public void stopTimeUpdates() {
        isUpdatingTime = false;
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
    }

    public void setAnnouncements(List<AnnouncementResponse> newAnnouncements) {
        if (newAnnouncements == null) {
            newAnnouncements = Collections.emptyList();
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AnnouncementDiffCallback(this.announcements, newAnnouncements));
        this.announcements = new ArrayList<>(newAnnouncements);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = contextRef.get();
        if (context == null) {
            throw new IllegalStateException("Context has been garbage collected");
        }

        View view = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementResponse announcement = announcements.get(position);
        bindAnnouncement(holder, announcement);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty() || !payloads.contains("timeUpdate")) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }

        // Only update the time for partial binds
        AnnouncementResponse announcement = announcements.get(position);
        updateTimeDisplay(holder, announcement);
    }

    private void bindAnnouncement(ViewHolder holder, AnnouncementResponse announcement) {
        // Set announcement text with proper text processing
        String descriptionText = announcement.getDescriptionText();
        if (!TextUtils.isEmpty(descriptionText)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                holder.textDescription.setText(Html.fromHtml(descriptionText, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.textDescription.setText(Html.fromHtml(descriptionText));
            }
        } else {
            holder.textDescription.setText("");
        }

        // Update time display
        updateTimeDisplay(holder, announcement);

        // Setup click listeners
        setupClickListeners(holder, announcement);

        // Setup images grid
        setupImagesGrid(holder, announcement.getAnnouncementImages());
    }

    private void updateTimeDisplay(ViewHolder holder, AnnouncementResponse announcement) {
        try {
            Date date = inputFormat.parse(announcement.getPostedAt());
            if (date != null) {
                String formattedDate = getRelativeTimeSpanString(date.getTime());
                holder.textDate.setText(formattedDate);
            }
        } catch (ParseException e) {
            holder.textDate.setText(announcement.getPostedAt());
        }
    }

    private void setupClickListeners(ViewHolder holder, AnnouncementResponse announcement) {
        // Root view click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAnnouncementClick(announcement);
            }
        });

        // Share button
        holder.shareButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClick(announcement);
            }
        });

        // See more button
        holder.seeMoreButton.setOnClickListener(v -> {
            boolean isExpanded = holder.textDescription.getMaxLines() > 4;

            // Setup transition
            AutoTransition transition = new AutoTransition();
            transition.setDuration(200);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView, transition);

            // Update view state
            if (isExpanded) {
                holder.textDescription.setMaxLines(4);
                holder.seeMoreButton.setText(R.string.see_more);
            } else {
                holder.textDescription.setMaxLines(Integer.MAX_VALUE);
                holder.seeMoreButton.setText(R.string.see_less);
            }
        });
    }

    private void setupImagesGrid(ViewHolder holder, List<String> images) {
        if (images == null || images.isEmpty()) {
            holder.imagesRecyclerView.setVisibility(View.GONE);
            return;
        }

        Context context = contextRef.get();
        if (context == null) return;

        holder.imagesRecyclerView.setVisibility(View.VISIBLE);

        // Calculate optimal span count
        int spanCount = calculateOptimalSpanCount(images.size());
        GridLayoutManager layoutManager = new GridLayoutManager(context, spanCount);
        holder.imagesRecyclerView.setLayoutManager(layoutManager);

        // Create and set adapter
        ImagesAdapter imagesAdapter = new ImagesAdapter(context, images,
                (imageUrl, position) -> {
                    if (listener != null) {
                        listener.onImageClick(imageUrl, images, position);
                    }
                });
        holder.imagesRecyclerView.setAdapter(imagesAdapter);
    }

    private int calculateOptimalSpanCount(int imageCount) {
        if (imageCount <= 2) return 1;
        if (imageCount <= 4) return 2;
        return 3;
    }

    private String getRelativeTimeSpanString(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return "Just now";
        } else if (diff < DateUtils.HOUR_IN_MILLIS) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
        } else if (diff < DateUtils.DAY_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString();
        } else if (diff < 7 * DateUtils.DAY_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.DAY_IN_MILLIS).toString();
        } else {
            return outputFormat.format(new Date(time));
        }
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.setIsRecyclable(false);  // Prevent recycling during animations
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.setIsRecyclable(true);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopTimeUpdates();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialTextView textDescription;
        final TextView textDate;
        final MaterialButton shareButton;
        final MaterialButton seeMoreButton;
        final RecyclerView imagesRecyclerView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDescription = itemView.findViewById(R.id.expandableTextView);
            textDate = itemView.findViewById(R.id.dateTextView);
            shareButton = itemView.findViewById(R.id.shareButton);
            seeMoreButton = itemView.findViewById(R.id.seeMoreButton);
            imagesRecyclerView = itemView.findViewById(R.id.imagesRecyclerView);
        }
    }
}