package com.example.barangayinformationsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DocumentStatusAdapter extends RecyclerView.Adapter<DocumentStatusAdapter.ViewHolder> {
    private List<DocumentRequest> requests;
    private OnCancelClickListener cancelListener;
    private OnItemClickListener itemClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(DocumentRequest request);
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentRequest request);
    }

    public DocumentStatusAdapter(List<DocumentRequest> requests, OnCancelClickListener cancelListener, OnItemClickListener itemClickListener) {
        this.requests = requests;
        this.cancelListener = cancelListener;
        this.itemClickListener = itemClickListener;
    }

    public void setRequests(List<DocumentRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentRequest request = requests.get(position);

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(request);
            }
        });

        holder.transactionNumber.setText("TXN-" + request.getId());
        holder.documentType.setText(request.getDocumentType());

        // Update status text based on pickup status
        if (request.isComplete()) {
            holder.statusText.setText("Complete");
            holder.pickupStatus.setVisibility(View.GONE); // Hide the separate pickup status since we're showing it in status
        } else {
            holder.statusText.setText(request.getStatus());
            holder.pickupStatus.setVisibility(View.GONE);
        }

        // Set status indicator color based on status
        int statusColor;
        boolean canCancel = false;

        if (request.isComplete()) {
            statusColor = 0xFF4CAF50; // Green color for completed
            holder.pickupInstructions.setVisibility(View.GONE);
            holder.rejectionReason.setVisibility(View.GONE);
        } else {
            switch(request.getStatus().toLowerCase()) {
                case "pending":
                    statusColor = 0xFFFFD700; // Gold color for pending
                    canCancel = true;
                    holder.pickupInstructions.setVisibility(View.GONE);
                    holder.rejectionReason.setVisibility(View.GONE);
                    break;
                case "approved":
                    statusColor = 0xFF00FF00; // Green color for approved
                    canCancel = false;
                    holder.pickupInstructions.setVisibility(View.VISIBLE);
                    holder.rejectionReason.setVisibility(View.GONE);
                    break;
                case "rejected":
                    statusColor = 0xFFFF0000; // Red color for rejected
                    canCancel = false;
                    holder.pickupInstructions.setVisibility(View.GONE);
                    if (request.getRejectionReason() != null && !request.getRejectionReason().isEmpty()) {
                        holder.rejectionReason.setVisibility(View.VISIBLE);
                        holder.rejectionReason.setText("Reason: " + request.getRejectionReason());
                    } else {
                        holder.rejectionReason.setVisibility(View.GONE);
                    }
                    break;
                default:
                    statusColor = 0xFFFFFFFF; // White color for unknown status
                    canCancel = false;
                    holder.pickupInstructions.setVisibility(View.GONE);
                    holder.rejectionReason.setVisibility(View.GONE);
                    break;
            }
        }

        // Set the status indicator color
        holder.statusIndicator.setBackgroundColor(statusColor);

        // Handle cancel button visibility
        if (request.getStatus().toLowerCase().equals("pending")) {
            canCancel = request.canBeCancelled();
            holder.cancelButton.setVisibility(View.VISIBLE);

            if (canCancel) {
                long remainingMinutes = request.getRemainingMinutes();
                holder.cancelButton.setText("Cancel (" + remainingMinutes + "m)");
                holder.cancelButton.setEnabled(true);
                holder.cancelButton.setAlpha(1.0f);
                holder.cancelButton.setOnClickListener(v -> {
                    if (cancelListener != null) {
                        cancelListener.onCancelClick(request);
                    }
                });
            } else {
                holder.cancelButton.setText("Cannot Cancel");
                holder.cancelButton.setEnabled(false);
                holder.cancelButton.setAlpha(0.5f);
            }
        } else {
            holder.cancelButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView transactionNumber;
        TextView documentType;
        TextView statusText;
        TextView pickupInstructions;
        TextView rejectionReason;
        TextView pickupStatus;      // Added this field
        View statusIndicator;
        Button cancelButton;

        ViewHolder(View itemView) {
            super(itemView);
            transactionNumber = itemView.findViewById(R.id.transaction_number);
            documentType = itemView.findViewById(R.id.document_type);
            statusText = itemView.findViewById(R.id.status_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            cancelButton = itemView.findViewById(R.id.cancel_request_button);
            pickupInstructions = itemView.findViewById(R.id.pickup_instructions);
            rejectionReason = itemView.findViewById(R.id.rejection_reason);
            pickupStatus = itemView.findViewById(R.id.pickup_status);  // Added this line
        }
    }
}