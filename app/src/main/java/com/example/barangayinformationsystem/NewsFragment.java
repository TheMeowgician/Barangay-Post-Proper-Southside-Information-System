package com.example.barangayinformationsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsFragment extends Fragment {

    private RecyclerView announcementsRecyclerView;
    private AnnouncementAdapter announcementAdapter;

    public NewsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        announcementsRecyclerView = view.findViewById(R.id.announcementsRecyclerView);
        setupRecyclerView();
        loadAnnouncements();

        return view;
    }

    private void setupRecyclerView() {
        announcementAdapter = new AnnouncementAdapter(requireContext());
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        announcementsRecyclerView.setAdapter(announcementAdapter);
    }

    private void loadAnnouncements() {
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<AnnouncementResponse>> call = apiService.getAnnouncements();

        call.enqueue(new Callback<List<AnnouncementResponse>>() {
            @Override
            public void onResponse(Call<List<AnnouncementResponse>> call, Response<List<AnnouncementResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    announcementAdapter.setAnnouncements(response.body());
                } else {
                    Toast.makeText(requireContext(), "Failed to load announcements", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementResponse>> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}