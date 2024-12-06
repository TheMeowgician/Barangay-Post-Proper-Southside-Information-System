package com.example.barangayinformationsystem;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsFragment extends Fragment implements AnnouncementAdapter.AnnouncementClickListener {
    private RecyclerView announcementsRecyclerView;
    private AnnouncementAdapter announcementAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateLayout;
    private MaterialButton retryButton;
    private LoadingState currentState = LoadingState.LOADING;

    enum LoadingState {
        LOADING, CONTENT, ERROR, EMPTY
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        loadAnnouncements();
        return view;
    }

    private void initializeViews(View view) {
        announcementsRecyclerView = view.findViewById(R.id.announcementsRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        retryButton = view.findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> loadAnnouncements());
    }

    private void setupRecyclerView() {
        announcementAdapter = new AnnouncementAdapter(requireContext(), this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        announcementsRecyclerView.setLayoutManager(layoutManager);
        announcementsRecyclerView.setAdapter(announcementAdapter);

        // Add item decoration for spacing
        announcementsRecyclerView.addItemDecoration(new MarginItemDecoration(
                getResources().getDimensionPixelSize(R.dimen.announcement_item_spacing)
        ));
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.purple_500);
        swipeRefreshLayout.setOnRefreshListener(this::loadAnnouncements);
    }

    private void loadAnnouncements() {
        updateLoadingState(LoadingState.LOADING);

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();

        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AnnouncementResponse>> call,
                                   @NonNull Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AnnouncementResponse> announcements = response.body();
                    if (announcements.isEmpty()) {
                        updateLoadingState(LoadingState.EMPTY);
                    } else {
                        updateLoadingState(LoadingState.CONTENT);
                        announcementAdapter.setAnnouncements(announcements);
                    }
                } else {
                    updateLoadingState(LoadingState.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AnnouncementResponse>> call,
                                  @NonNull Throwable t) {
                updateLoadingState(LoadingState.ERROR);
            }
        });
    }

    private void updateLoadingState(LoadingState state) {
        currentState = state;
        swipeRefreshLayout.setRefreshing(state == LoadingState.LOADING);

        switch (state) {
            case LOADING:
                announcementsRecyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                break;
            case CONTENT:
                announcementsRecyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                break;
            case ERROR:
            case EMPTY:
                announcementsRecyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onShareClick(AnnouncementResponse announcement) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, announcement.getDescriptionText());
        startActivity(Intent.createChooser(shareIntent, "Share Announcement"));
    }

    @Override
    public void onImageClick(String imageUrl, List<String> allImages, int position) {
        showFullScreenImagePager(allImages, position);
    }

    private void showFullScreenImagePager(List<String> images, int startPosition) {
        Dialog dialog = new Dialog(requireContext(),
                android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_pager);

        ViewPager2 imagePager = dialog.findViewById(R.id.imagePager);
        TextView imageCounter = dialog.findViewById(R.id.imageCounter);
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);

        FullScreenImageAdapter adapter = new FullScreenImageAdapter(requireContext(), images);
        imagePager.setAdapter(adapter);
        imagePager.setCurrentItem(startPosition, false);

        updateImageCounter(imageCounter, startPosition + 1, images.size());
        imagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageCounter(imageCounter, position + 1, images.size());
            }
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateImageCounter(TextView counter, int current, int total) {
        counter.setText(String.format(Locale.getDefault(), "%d/%d", current, total));
    }
}