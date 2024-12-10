package com.example.barangayinformationsystem;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentReportStatusFragment extends Fragment {
    private RecyclerView recyclerView;
    private IncidentReportStatusAdapter adapter;
    private LinearLayout emptyStateLayout;
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incident_report_status, container, false);

        recyclerView = view.findViewById(R.id.incident_status_recycler_view);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);

        setupRecyclerView();
        loadIncidentReports();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new IncidentReportStatusAdapter(new ArrayList<>(), this::showDetailsDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadIncidentReports() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(getContext(), "Please log in to view reports", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService = RetrofitClient.getApiService();
        Call<IncidentReportListResponse> call = apiService.getUserIncidentReports(userId);
        call.enqueue(new Callback<IncidentReportListResponse>() {
            @Override
            public void onResponse(Call<IncidentReportListResponse> call, Response<IncidentReportListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<IncidentReport> reports = response.body().getReports();
                    if (reports == null || reports.isEmpty()) {
                        showEmptyState();
                    } else {
                        showReports(reports);
                    }
                } else {
                    Toast.makeText(getContext(), "Error loading reports", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<IncidentReportListResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void showReports(List<IncidentReport> reports) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        adapter.setReports(reports);
    }

    private void showDetailsDialog(IncidentReport report) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_incident_details, null);

        // Find views in dialog
        TextView titleText = dialogView.findViewById(R.id.incident_title_text);
        TextView descriptionText = dialogView.findViewById(R.id.incident_description_text);
        TextView dateSubmittedText = dialogView.findViewById(R.id.date_submitted_text);
        TextView statusText = dialogView.findViewById(R.id.status_text);

        // Set values
        titleText.setText(report.getTitle());
        descriptionText.setText("Description: " + report.getDescription());
        dateSubmittedText.setText("Date Submitted: " + report.getDateSubmitted());

        // Set status with color
        String status = "Status: " + report.getStatus();
        statusText.setText(status);
        switch(report.getStatus().toLowerCase()) {
            case "pending":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "resolved":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
        }

        AlertDialog dialog = builder.setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        dialog.show();
    }
}