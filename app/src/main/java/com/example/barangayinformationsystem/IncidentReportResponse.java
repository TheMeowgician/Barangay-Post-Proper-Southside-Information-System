package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class IncidentReportResponse {
    private String status;
    private String message;

    @SerializedName("images")
    private List<String> images;

    @SerializedName("video")
    private String video;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getImages() {
        return images;
    }

    public String getVideo() {
        return video;
    }
}