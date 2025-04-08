package com.example.barangayinformationsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IncidentReportStatusAdapter extends RecyclerView.Adapter<IncidentReportStatusAdapter.ViewHolder> {
    private List<IncidentReport> reports;
    private OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        void onItemClick(IncidentReport report);
    }

    public IncidentReportStatusAdapter(List<IncidentReport> reports, OnItemClickListener itemClickListener) {
        this.reports = reports;
        this.itemClickListener = itemClickListener;
    }

    public void setReports(List<IncidentReport> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incident_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IncidentReport report = reports.get(position);

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(report);
            }
        });

        holder.reportId.setText("Report #" + report.getId());
        holder.incidentTitle.setText(report.getTitle());
        holder.statusText.setText(report.getStatus());

        // Set status indicator color based on status
        int statusColor;
        switch(report.getStatus().toLowerCase()) {
            case "pending":
                statusColor = 0xFFFFD700; // Gold color
                // Hide resolved time for pending reports
                holder.resolvedTimeContainer.setVisibility(View.GONE);
                break;
            case "resolved":
                statusColor = 0xFF00FF00; // Green color

                // Show resolved time if available
                if (report.getResolvedAt() != null && !report.getResolvedAt().isEmpty()) {
                    holder.resolvedTimeContainer.setVisibility(View.VISIBLE);
                    holder.resolvedTimeText.setText(report.getFormattedResolvedTime());
                } else {
                    holder.resolvedTimeContainer.setVisibility(View.GONE);
                }
                break;
            default:
                statusColor = 0xFFFFFFFF; // White color
                holder.resolvedTimeContainer.setVisibility(View.GONE);
                break;
        }
        holder.statusIndicator.setBackgroundColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return reports != null ? reports.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reportId;
        TextView incidentTitle;
        TextView statusText;
        View statusIndicator;
        LinearLayout resolvedTimeContainer;
        TextView resolvedTimeText;

        ViewHolder(View itemView) {
            super(itemView);
            reportId = itemView.findViewById(R.id.report_number);
            incidentTitle = itemView.findViewById(R.id.incident_title);
            statusText = itemView.findViewById(R.id.status_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            resolvedTimeContainer = itemView.findViewById(R.id.resolved_time_container);
            resolvedTimeText = itemView.findViewById(R.id.resolved_time_text);
        }
    }
}