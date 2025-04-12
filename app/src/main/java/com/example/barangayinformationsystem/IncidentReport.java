package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class IncidentReport {
    private int id;
    private String name;
    private String title;
    private String description;

    @SerializedName("incident_picture")
    private String incident_picture;

    @SerializedName("incident_video")
    private String incident_video;

    @SerializedName("date_submitted")
    private String date_submitted;

    private String status;

    @SerializedName("resolved_at")
    private String resolved_at;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIncidentPicture() { return incident_picture; }
    public void setIncidentPicture(String incident_picture) { this.incident_picture = incident_picture; }

    public String getIncidentVideo() { return incident_video; }
    public void setIncidentVideo(String incident_video) { this.incident_video = incident_video; }

    public String getDateSubmitted() { return date_submitted; }
    public void setDateSubmitted(String date_submitted) { this.date_submitted = date_submitted; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolvedAt() { return resolved_at; }
    public void setResolvedAt(String resolved_at) { this.resolved_at = resolved_at; }

    // Helper methods
    public boolean isResolved() {
        return "resolved".equalsIgnoreCase(status);
    }

    public boolean hasVideo() {
        return incident_video != null && !incident_video.isEmpty();
    }

    // Format resolved time to Manila, Philippines time (PHT/Asia/Manila)
    public String getFormattedResolvedTime() {
        if (resolved_at == null || resolved_at.isEmpty()) {
            return "";
        }

        try {
            // Parse the database datetime format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

            Date date = inputFormat.parse(resolved_at);

            // Format to Manila time (PHT)
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return resolved_at; // Return original if parsing fails
        }
    }
}