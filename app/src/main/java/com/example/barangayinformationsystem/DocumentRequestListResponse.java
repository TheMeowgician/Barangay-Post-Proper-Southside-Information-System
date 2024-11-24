package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DocumentRequestListResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("requests")
    private List<DocumentRequest> requests;

    @SerializedName("message")
    private String message;  // For error messages if success is false

    public boolean isSuccess() {
        return success;
    }

    public List<DocumentRequest> getRequests() {
        return requests;
    }

    public String getMessage() {
        return message;
    }
}