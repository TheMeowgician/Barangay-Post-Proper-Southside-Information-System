package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

public class VideoUploadResponse {
    @SerializedName("secure_url")
    private String secureUrl;

    @SerializedName("error")
    private ErrorInfo error;

    public String getSecureUrl() {
        return secureUrl;
    }

    public ErrorInfo getError() {
        return error;
    }

    public static class ErrorInfo {
        @SerializedName("message")
        private String message;

        public String getMessage() {
            return message;
        }
    }
}