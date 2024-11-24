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

    public interface OnCancelClickListener {
        void onCancelClick(DocumentRequest request);
    }

    public DocumentStatusAdapter(List<DocumentRequest> requests, OnCancelClickListener cancelListener) {
        this.requests = requests;
        this.cancelListener = cancelListener;
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

        holder.transactionNumber.setText("TXN-" + request.getId());
        holder.documentType.setText(request.getDocumentType());
        holder.statusText.setText(request.getStatus());

        // Set status indicator color based on status
        int statusColor;
        boolean canCancel = false;

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
                // Show rejection reason if available
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

        // Set the status indicator color
        holder.statusIndicator.setBackgroundColor(statusColor);

        // Handle cancel button visibility
        if (canCancel) {
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancelClick(request);
                }
            });
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
        }
    }
}