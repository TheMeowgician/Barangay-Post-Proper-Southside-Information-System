package com.example.barangayinformationsystem;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
        adapter = new DocumentStatusAdapter(new ArrayList<>(),
                this::showCancellationDialog,
                this::showDetailsDialog); // Add this
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void showDetailsDialog(DocumentRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_document_details, null);

        // Find views in dialog
        TextView nameText = dialogView.findViewById(R.id.name_text);
        TextView addressText = dialogView.findViewById(R.id.address_text);
        TextView birthdayText = dialogView.findViewById(R.id.birthday_text);
        TextView purposeText = dialogView.findViewById(R.id.purpose_text);
        TextView quantityText = dialogView.findViewById(R.id.quantity_text);
        TextView dateRequestedText = dialogView.findViewById(R.id.date_requested_text);
        TextView documentTypeText = dialogView.findViewById(R.id.document_type_text);
        TextView statusText = dialogView.findViewById(R.id.status_text);

        // Set values with better formatting
        documentTypeText.setText(request.getDocumentType());
        nameText.setText("Name: " + request.getName());
        addressText.setText("Address: " + request.getAddress());
        birthdayText.setText("Birthday: " + request.getBirthday());
        purposeText.setText("Purpose: " + request.getPurpose());
        quantityText.setText("Quantity: " + String.valueOf(request.getQuantity()));
        dateRequestedText.setText("Date Requested: " + request.getDateRequested());

        // Set status with color
        String status = "Status: " + request.getStatus();
        statusText.setText(status);
        switch(request.getStatus().toLowerCase()) {
            case "pending":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "approved":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "rejected":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
        }

        if ("cancelled".equalsIgnoreCase(request.getStatus()) && request.getCancellationReason() != null) {
            TextView cancellationText = dialogView.findViewById(R.id.cancellation_reason);
            cancellationText.setVisibility(View.VISIBLE);
            cancellationText.setText("Cancellation Reason: " + request.getCancellationReason());
        }

        AlertDialog dialog = builder.setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
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
        if (!request.canBeCancelled()) {
            Toast.makeText(getContext(),
                    "Requests can only be cancelled within 15 minutes of submission",
                    Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_cancel_request, null);

        RadioGroup reasonGroup = dialogView.findViewById(R.id.cancel_reason_group);
        TextInputLayout otherReasonLayout = dialogView.findViewById(R.id.other_reason_layout);
        TextInputEditText otherReasonInput = dialogView.findViewById(R.id.other_reason_input);
        TextView warningText = dialogView.findViewById(R.id.warning_text);

        warningText.setText("You have " + request.getRemainingMinutes() + " minutes left to cancel this request");

        // Handle radio button changes
        reasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.reason_others) {
                otherReasonLayout.setVisibility(View.VISIBLE);
            } else {
                otherReasonLayout.setVisibility(View.GONE);
                otherReasonInput.setText("");  // Clear the text when hiding
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Cancel Request")
                .setView(dialogView)
                .setPositiveButton("Continue", null)  // Set to null initially
                .setNegativeButton("Keep request", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button continueButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            continueButton.setOnClickListener(v -> {
                // Get the selected reason
                int selectedId = reasonGroup.getCheckedRadioButtonId();

                if (selectedId == -1) {
                    Toast.makeText(getContext(), "Please select a reason", Toast.LENGTH_SHORT).show();
                    return;
                }

                String reason;
                if (selectedId == R.id.reason_others) {
                    String otherReason = otherReasonInput.getText().toString().trim();
                    if (otherReason.isEmpty()) {
                        Toast.makeText(getContext(), "Please specify your reason", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reason = otherReason;
                } else {
                    RadioButton selectedButton = dialogView.findViewById(selectedId);
                    reason = selectedButton.getText().toString();
                }

                dialog.dismiss();
                showFinalConfirmationDialog(request, reason);
            });
        });

        dialog.show();
    }
    private void showFinalConfirmationDialog(DocumentRequest request, String reason) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Cancellation")
                .setMessage("Are you sure you want to cancel this request?\n\nReason: " + reason)
                .setPositiveButton("Yes, cancel request", (dialog, which) -> {
                    cancelRequest(request, reason);
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void cancelRequest(DocumentRequest request, String reason) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<DocumentRequestResponse> call = apiService.cancelRequest(request.getId(), reason);

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