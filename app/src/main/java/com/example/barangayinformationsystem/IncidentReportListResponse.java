package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class IncidentReportListResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("reports")
    private List<IncidentReport> reports;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public List<IncidentReport> getReports() { return reports; }
    public String getMessage() { return message; }
}