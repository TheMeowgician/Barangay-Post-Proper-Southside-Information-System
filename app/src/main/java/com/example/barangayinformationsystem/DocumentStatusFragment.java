package com.example.barangayinformationsystem;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentStatusFragment extends Fragment {
    private RecyclerView recyclerView;
    private DocumentStatusAdapter adapter;
    private LinearLayout emptyStateLayout;
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_status, container, false);

        recyclerView = view.findViewById(R.id.document_status_recycler_view);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);

        setupRecyclerView();
        loadDocumentRequests();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new DocumentStatusAdapter(new ArrayList<>(), this::showCancellationDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadDocumentRequests() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int userId = prefs.getInt("user_id", -1);  // Changed to match LoginActivity's key

        if (userId == -1) {
            Toast.makeText(getContext(), "Please log in to view requests", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService = RetrofitClient.getApiService();
        Call<DocumentRequestListResponse> call = apiService.getUserRequests(userId);
        call.enqueue(new Callback<DocumentRequestListResponse>() {
            @Override
            public void onResponse(Call<DocumentRequestListResponse> call, Response<DocumentRequestListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<DocumentRequest> requests = response.body().getRequests();
                    if (requests == null || requests.isEmpty()) {
                        showEmptyState();
                    } else {
                        showRequests(requests);
                    }
                } else {
                    Toast.makeText(getContext(), "Error loading requests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DocumentRequestListResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void showRequests(List<DocumentRequest> requests) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        adapter.setRequests(requests);
    }

    private void showCancellationDialog(DocumentRequest request) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_cancel_request, null);

        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Request")
                .setView(dialogView)
                .setPositiveButton("Continue", (dialog, which) ->
                        showConfirmationDialog(request))
                .setNegativeButton("No, cancel", null)
                .show();
    }

    private void showConfirmationDialog(DocumentRequest request) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Request Confirmation")
                .setMessage("Are you sure you want to cancel your request? This action cannot be undone.")
                .setPositiveButton("Yes, confirm", (dialog, which) ->
                        cancelRequest(request))
                .setNegativeButton("No, cancel", null)
                .show();
    }

    private void cancelRequest(DocumentRequest request) {
        Call<DocumentRequestResponse> call = apiService.cancelRequest(request.getId());
        call.enqueue(new Callback<DocumentRequestResponse>() {
            @Override
            public void onResponse(Call<DocumentRequestResponse> call, Response<DocumentRequestResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    showSuccessDialog();
                    loadDocumentRequests(); // Reload the list
                } else {
                    Toast.makeText(getContext(), "Error cancelling request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DocumentRequestResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog() {
        SuccessDialog.showSuccess(
                getContext(),
                "Your request for the document has been successfully cancelled.",
                null,
                2000
        );
    }
}