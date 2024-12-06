package com.example.barangayinformationsystem;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {
    private final List<String> images;
    private final Context context;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl, int position);
    }

    public ImagesAdapter(Context context, List<String> images, OnImageClickListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_announcement_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = images.get(position);

        // Calculate aspect ratio based on position and total images
        float aspectRatio = position == 0 && images.size() == 1 ? 16f/9f : 1f;
        holder.adjustAspectRatio(aspectRatio);

        // Load image with shimmer effect
        Glide.with(context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        holder.errorView.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(imageUrl, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imageView;
        final ProgressBar progressBar;
        final View errorView;
        final ConstraintLayout container;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.imageContainer);
            imageView = itemView.findViewById(R.id.announcementImage);
            progressBar = itemView.findViewById(R.id.imageProgress);
            errorView = itemView.findViewById(R.id.errorView);
        }

        void adjustAspectRatio(float ratio) {
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(container);
            constraintSet.setDimensionRatio(imageView.getId(), String.valueOf(ratio));
            constraintSet.applyTo(container);
        }
    }
}