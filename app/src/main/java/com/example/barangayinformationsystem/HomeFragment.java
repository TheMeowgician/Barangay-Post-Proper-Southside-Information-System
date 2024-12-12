package com.example.barangayinformationsystem;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private TextView notableProjectsTextView, seeInfoTextView1, seeInfoTextView2, seeInfoTextView3;
    private TextView newsAnnouncementsAndUpdatesTextView;
    private RecyclerView announcementsRecyclerView;
    private AnnouncementAdapter announcementAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupTextStyles();
        setupRecyclerView();
        loadAnnouncements();

        return view;
    }

    private void initializeViews(View view) {
        notableProjectsTextView = view.findViewById(R.id.notableProjectsOfOurBarangayTextView);
        seeInfoTextView1 = view.findViewById(R.id.seeInfoTextView1);
        seeInfoTextView2 = view.findViewById(R.id.seeInfoTextView2);
        seeInfoTextView3 = view.findViewById(R.id.seeInfoTextView3);
        newsAnnouncementsAndUpdatesTextView = view.findViewById(R.id.newsAnnouncementsAndUpdatesTextView);
        announcementsRecyclerView = view.findViewById(R.id.announcementsRecyclerView);
    }

    private void setupTextStyles() {
        SpannableStringBuilder spannableStringBuilder1 = new SpannableStringBuilder("See Info");
        spannableStringBuilder1.setSpan(new UnderlineSpan(), 0, spannableStringBuilder1.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        String text = notableProjectsTextView.getText().toString();
        SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(text);
        int start = text.indexOf("Projects");
        int end = start + "Projects".length();
        spannableStringBuilder2.setSpan(new ForegroundColorSpan(Color.parseColor("#61009F")),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        notableProjectsTextView.setText(spannableStringBuilder2);

        String text2 = newsAnnouncementsAndUpdatesTextView.getText().toString();
        SpannableStringBuilder spannableStringBuilder3 = new SpannableStringBuilder(text2);
        int start2 = text2.indexOf("and");
        int end2 = start2 + "and".length();
        spannableStringBuilder3.setSpan(new ForegroundColorSpan(Color.parseColor("#000000")),
                start2, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        newsAnnouncementsAndUpdatesTextView.setText(spannableStringBuilder3);
    }

    private void setupRecyclerView() {
        // Old version
        // announcementAdapter = new AnnouncementAdapter(requireContext());

        // New version - implement the listener
        announcementAdapter = new AnnouncementAdapter(requireContext(), new AnnouncementAdapter.AnnouncementClickListener() {
            @Override
            public void onShareClick(AnnouncementResponse announcement) {
                // Handle share click
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, announcement.getDescriptionText());
                startActivity(Intent.createChooser(shareIntent, "Share Announcement"));
            }

            @Override
            public void onImageClick(String imageUrl, List<String> allImages, int position) {
                showFullScreenImagePager(allImages, position);
            }
        });
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        announcementsRecyclerView.setAdapter(announcementAdapter);
    }

    private void showFullScreenImagePager(List<String> images, int startPosition) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
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

    private void loadAnnouncements() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();

        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call,
                                   Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    announcementAdapter.setAnnouncements(response.body());
                } else {
                    Toast.makeText(requireContext(), "Failed to load announcements",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}